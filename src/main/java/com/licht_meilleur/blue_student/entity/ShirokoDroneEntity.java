package com.licht_meilleur.blue_student.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.network.ServerFx;
import com.licht_meilleur.blue_student.student.StudentId;
import com.licht_meilleur.blue_student.weapon.ProjectileWeaponAction;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ShirokoDroneEntity extends Entity implements GeoEntity {

    public static final String ANIM_DRONE = "animation.model.drone";
    private static final RawAnimation DRONE_LOOP = RawAnimation.begin().thenLoop(ANIM_DRONE);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int lifeTicks = 20 * 20;

    private static final EntityDataAccessor<Integer> START_TRIGGER =
            SynchedEntityData.defineId(ShirokoDroneEntity.class, EntityDataSerializers.INT);

    private UUID ownerUuid;
    private UUID forcedTargetUuid;

    private static final double SHOOT_INTERVAL = 6;
    private int shootCooldown = 0;

    private int clientStartTicks = 0;
    private int lastStartTrigger = 0;
    private static final int START_ANIM_TICKS = 22;

    private int lastOwnerShotTrigger = 0;

    public ShirokoDroneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public ShirokoDroneEntity(Level level) {
        this(BlueStudentMod.SHIROKO_DRONE.get(), level);
    }

    public ShirokoDroneEntity setOwnerUuid(UUID owner) {
        this.ownerUuid = owner;
        return this;
    }

    public ShirokoDroneEntity setTargetUuid(@Nullable UUID target) {
        this.forcedTargetUuid = target;
        return this;
    }

    public void requestStartAnim() {
        if (this.level().isClientSide()) return;
        this.entityData.set(START_TRIGGER, this.entityData.get(START_TRIGGER) + 1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START_TRIGGER, 0);
    }

    private void loadDroneData(CompoundTag tag) {
        String owner = tag.getString("Owner").orElse("");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);

        String target = tag.getString("Target").orElse("");
        forcedTargetUuid = target.isEmpty() ? null : UUID.fromString(target);

        shootCooldown = tag.getInt("ShootCd").orElse(0);
    }

    private void saveDroneData(CompoundTag tag) {
        if (ownerUuid != null) {
            tag.putString("Owner", ownerUuid.toString());
        }
        if (forcedTargetUuid != null) {
            tag.putString("Target", forcedTargetUuid.toString());
        }
        tag.putInt("ShootCd", shootCooldown);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (--lifeTicks <= 0) {
                this.discard();
                return;
            }
        }

        if (this.level().isClientSide()) {
            int trig = this.entityData.get(START_TRIGGER);
            if (trig != lastStartTrigger) {
                lastStartTrigger = trig;
                clientStartTicks = START_ANIM_TICKS;
            } else if (clientStartTicks > 0) {
                clientStartTicks--;
            }
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        Entity owner = (ownerUuid != null) ? serverLevel.getEntity(ownerUuid) : null;
        if (!(owner instanceof AbstractStudentEntity shiroko) || !owner.isAlive()) {
            this.discard();
            return;
        }

        Vec3 vel = this.getDeltaMovement();

        Vec3 desired = calcDesiredPos(shiroko);
        Vec3 cur = this.position();
        Vec3 to = desired.subtract(cur);
        Vec3 nextPos = cur.add(vel);
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        double k = 0.18;
        double damping = 0.65;

        Vec3 accel = to.scale(k).subtract(vel.scale(damping));
        vel = vel.add(accel);

        double maxSpeed = 0.35;
        if (vel.lengthSqr() > maxSpeed * maxSpeed) {
            vel = vel.normalize().scale(maxSpeed);
        }

        this.setDeltaMovement(vel);

        LivingEntity tgt = shiroko.getTarget();
        if (tgt != null && tgt.isAlive()) {
            Vec3 from = this.position();
            Vec3 targetPos = tgt.getEyePosition();
            Vec3 look = targetPos.subtract(from);

            double dx = look.x;
            double dy = look.y;
            double dz = look.z;

            float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));

            this.setYRot(yaw);
            this.setXRot(pitch);
            this.setYBodyRot(yaw);
            this.setYHeadRot(yaw);
        } else {
            this.setYRot(shiroko.getYRot());
            this.setXRot(0.0f);
            this.setYBodyRot(shiroko.getYRot());
            this.setYHeadRot(shiroko.getYRot());
        }

        int trig = shiroko.getShotTrigger();
        if (trig != lastOwnerShotTrigger) {
            lastOwnerShotTrigger = trig;
            shootFromDrone(serverLevel, shiroko);
        }

        if (shootCooldown > 0) {
            shootCooldown--;
        }
    }

    private Vec3 calcDesiredPos(AbstractStudentEntity owner) {
        Vec3 forward = owner.getViewVector(1.0f).normalize();
        return owner.position()
                .add(0.0, 2.0, 0.0)
                .add(forward.scale(-0.8));
    }

    private float approachAngle(float cur, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - cur);
        delta = Mth.clamp(delta, -maxStep, maxStep);
        return cur + delta;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "main",
                0,
                state -> {
                    state.setAnimation(DRONE_LOOP);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private void shootFromDrone(ServerLevel serverLevel, AbstractStudentEntity owner) {
        LivingEntity target = owner.getTarget();
        if (target == null || !target.isAlive()) return;

        WeaponSpec spec = WeaponSpecs.forStudent(StudentId.SHIROKO);

        Vec3 start = this.position();
        Vec3 dir = target.getEyePosition().subtract(start).normalize();

        new ProjectileWeaponAction().shootFromCustomPos(
                owner,
                target,
                spec,
                start,
                dir
        );

        ServerFx.sendShotFx(
                serverLevel,
                this.getId(),
                start,
                spec.fxType,
                spec.fxWidth,
                new Vec3[]{dir},
                (float) spec.range
        );
    }

    @Override
    public void move(MoverType moverType, Vec3 movement) {
        super.move(moverType, movement);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

    }
}