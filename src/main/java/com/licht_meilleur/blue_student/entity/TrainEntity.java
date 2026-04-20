package com.licht_meilleur.blue_student.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


import java.util.UUID;

public class TrainEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerPlayerUuid;
    private UUID gunTrainUuid;
    private UUID targetUuid;
    private UUID nozomiPassengerUuid;

    public enum TrainMode {
        SINGLE_CHARGE,
        COMBO_CYCLE
    }

    private TrainMode mode = TrainMode.SINGLE_CHARGE;

    public TrainMode getMode() {
        return mode;
    }

    public void setMode(TrainMode mode) {
        this.mode = mode;
    }

    private int phaseTicks = 0;
    private boolean gunFireEnabled = true;

    public boolean isGunFireEnabled() {
        return gunFireEnabled;
    }

    private static final int CRUISE_TICKS = 20 * 5;
    private static final int CHARGE_TICKS = 20;
    private static final double CRUISE_SPEED = 0.35;
    private static final double CHARGE_SPEED = 1.25;

    private float theta = 0f;
    private float radius = 6.0f;
    private float omega = 0.12f;
    private boolean clockwise = true;

    private static final float CHARGE_HIT_RADIUS = 1.6f;
    private static final float CHARGE_DAMAGE = 8.0f;
    private static final double KNOCKBACK_H = 1.2;
    private static final double KNOCKBACK_Y = 0.25;
    private static final int SINGLE_CHARGE_MIN_TICKS = 6;

    public TrainEntity setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
        return this;
    }

    private int lifeTicks = 0;
    private static final int MAX_LIFE = 20 * 15;

    private static final EntityDataAccessor<Float> SYNC_YAW =
            SynchedEntityData.defineId(TrainEntity.class, EntityDataSerializers.FLOAT);

    public TrainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNC_YAW, 0.0f);
    }

    public TrainEntity setOwnerPlayerUuid(UUID ownerPlayerUuid) {
        this.ownerPlayerUuid = ownerPlayerUuid;
        return this;
    }

    public TrainEntity setGunTrainUuid(UUID gunUuid) {
        this.gunTrainUuid = gunUuid;
        return this;
    }

    public TrainEntity setTargetUuid(UUID target) {
        this.targetUuid = target;
        return this;
    }

    public TrainEntity setNozomiPassengerUuid(UUID id) {
        this.nozomiPassengerUuid = id;
        return this;
    }

    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    public UUID getGunTrainUuid() {
        return gunTrainUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    private void setYawStableServer(float yaw) {
        float y = Mth.wrapDegrees(yaw);
        this.setYRot(y);
        this.setYBodyRot(y);
        this.setYHeadRot(y);
        this.entityData.set(SYNC_YAW, y);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            float y = this.entityData.get(SYNC_YAW);
            this.setYRot(y);
            this.setYBodyRot(y);
            this.setYHeadRot(y);
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        if (++lifeTicks > MAX_LIFE) {
            discardWithLinked(serverLevel);
            return;
        }
        if (!isOwnerAlive(serverLevel)) {
            discardWithLinked(serverLevel);
            return;
        }

        Entity target = (targetUuid != null) ? serverLevel.getEntity(targetUuid) : null;
        if (target == null || !target.isAlive()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        gunTrainUuid = null;
        gunFireEnabled = false;

        tickSingleCharge(serverLevel, target);
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

    private void tickSingleCharge(ServerLevel serverLevel, Entity target) {
        if (lifeTicks > SINGLE_CHARGE_MIN_TICKS) {
            if (tryChargeHit(serverLevel)) {
                endSkillAndDiscard(serverLevel);
                return;
            }
        }

        Vec3 from = this.position();
        Vec3 to = target.position().add(0.0, 0.2, 0.0);
        Vec3 d = to.subtract(from);

        if (d.lengthSqr() < 0.45) {
            endSkillAndDiscard(serverLevel);
            return;
        }

        Vec3 v = d.normalize().scale(CHARGE_SPEED);
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);

        if (v.horizontalDistanceSqr() > 1.0e-6) {
            float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0f);
            setYawStableServer(yaw);
        }
    }

    private void tickComboCycle(ServerLevel serverLevel, Entity target) {
        if (phaseTicks <= 0) {
            if (gunFireEnabled) {
                gunFireEnabled = false;
                phaseTicks = CHARGE_TICKS;
            } else {
                gunFireEnabled = true;
                phaseTicks = CRUISE_TICKS;
            }
        }

        if (gunFireEnabled) {
            tickCruiseOrbit(serverLevel, target);
        } else {
            tickCharge(serverLevel, target);
        }

        phaseTicks--;
    }

    private void tickCruiseOrbit(ServerLevel serverLevel, Entity center) {
        float step = Math.abs(omega);
        theta += clockwise ? +step : -step;

        double cx = center.getX();
        double cz = center.getZ();

        double gx = cx + Math.cos(theta) * radius;
        double gz = cz + Math.sin(theta) * radius;
        double gy = center.getY();

        Vec3 goal = new Vec3(gx, gy, gz);
        Vec3 dir = goal.subtract(this.position());

        if (dir.lengthSqr() > 1.0e-6) {
            this.setDeltaMovement(dir.normalize().scale(CRUISE_SPEED));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        Vec3 v = this.getDeltaMovement();
        if (v.horizontalDistanceSqr() > 1.0e-6) {
            float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0f);
            setYawStableServer(yaw);
        }
    }

    private void tickCharge(ServerLevel serverLevel, Entity target) {
        Vec3 from = this.position();
        Vec3 to = target.position().add(0.0, 0.2, 0.0);
        Vec3 d = to.subtract(from);

        if (lifeTicks > SINGLE_CHARGE_MIN_TICKS) {
            if (tryChargeHit(serverLevel)) {
                phaseTicks = 0;
                return;
            }
        }

        if (d.lengthSqr() < 0.65) {
            phaseTicks = 0;
            return;
        }

        Vec3 v = d.normalize().scale(CHARGE_SPEED);
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);

        if (v.horizontalDistanceSqr() > 1.0e-6) {
            float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0f);
            setYawStableServer(yaw);
        }
    }

    private void dropNozomiIfAny(ServerLevel serverLevel) {
        if (nozomiPassengerUuid == null) return;
        Entity p = serverLevel.getEntity(nozomiPassengerUuid);
        if (p != null && p.getVehicle() == this) {
            p.stopRiding();
        }
    }

    private void discardWithLinked(ServerLevel serverLevel) {
        if (gunTrainUuid != null) {
            Entity e = serverLevel.getEntity(gunTrainUuid);
            if (e != null) e.discard();
        }
        endSkillAndDiscard(serverLevel);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);

        float bodyYaw = this.getYRot();

        if (passenger instanceof NozomiEntity) {
            Vec3 seat = getSeatWorldNozomiOnTrain();
            passenger.setPos(seat.x, seat.y, seat.z);
            passenger.setYRot(bodyYaw);

            if (passenger instanceof LivingEntity le) {
                le.setYBodyRot(bodyYaw);
                le.setYHeadRot(bodyYaw);
            }

            passenger.noPhysics = true;
            passenger.setNoGravity(true);
            passenger.setDeltaMovement(Vec3.ZERO);
        }
    }

    private boolean isOwnerAlive(ServerLevel serverLevel) {
        if (ownerPlayerUuid == null) return false;

        for (NozomiEntity n : serverLevel.getEntitiesOfClass(
                NozomiEntity.class,
                this.getBoundingBox().inflate(128.0),
                e -> e.isAlive()
        )) {
            if (ownerPlayerUuid.equals(n.getOwnerUuid())) return true;
        }
        return false;
    }

    private void loadTrainData(CompoundTag tag) {
        String ownerP = tag.getString("OwnerP").orElse("");
        ownerPlayerUuid = ownerP.isEmpty() ? null : UUID.fromString(ownerP);

        String gun = tag.getString("Gun").orElse("");
        gunTrainUuid = gun.isEmpty() ? null : UUID.fromString(gun);

        String target = tag.getString("Target").orElse("");
        targetUuid = target.isEmpty() ? null : UUID.fromString(target);

        String nozomiP = tag.getString("NozomiP").orElse("");
        nozomiPassengerUuid = nozomiP.isEmpty() ? null : UUID.fromString(nozomiP);

        String modeName = tag.getString("Mode").orElse("");
        if (!modeName.isEmpty()) {
            try {
                mode = TrainMode.valueOf(modeName);
            } catch (Exception ignored) {
                mode = TrainMode.SINGLE_CHARGE;
            }
        }

        lifeTicks = tag.getInt("Life").orElse(0);
        phaseTicks = tag.getInt("Phase").orElse(0);
        gunFireEnabled = tag.getBoolean("GunFire").orElse(false);

        theta = tag.getFloat("Theta").orElse(0f);
        radius = tag.getFloat("Radius").orElse(6.0f);
        omega = tag.getFloat("Omega").orElse(0.12f);
        clockwise = tag.getBoolean("Clockwise").orElse(true);
    }

    private void saveTrainData(CompoundTag tag) {
        if (ownerPlayerUuid != null) {
            tag.putString("OwnerP", ownerPlayerUuid.toString());
        }
        if (gunTrainUuid != null) {
            tag.putString("Gun", gunTrainUuid.toString());
        }
        if (targetUuid != null) {
            tag.putString("Target", targetUuid.toString());
        }
        if (nozomiPassengerUuid != null) {
            tag.putString("NozomiP", nozomiPassengerUuid.toString());
        }

        tag.putString("Mode", mode.name());
        tag.putInt("Life", lifeTicks);
        tag.putInt("Phase", phaseTicks);
        tag.putBoolean("GunFire", gunFireEnabled);

        tag.putFloat("Theta", theta);
        tag.putFloat("Radius", radius);
        tag.putFloat("Omega", omega);
        tag.putBoolean("Clockwise", clockwise);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "controller",
                0,
                state -> {
                    boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.0008;
                    state.setAnimation(
                            moving
                                    ? RawAnimation.begin().thenLoop("animation.go")
                                    : RawAnimation.begin().thenLoop("animation.stop")
                    );
                    return PlayState.CONTINUE;
                }
        ));
    }

    private void endSkillAndDiscard(ServerLevel serverLevel) {
        dropNozomiIfAny(serverLevel);

        if (nozomiPassengerUuid != null) {
            Entity p = serverLevel.getEntity(nozomiPassengerUuid);
            if (p instanceof NozomiEntity n && n.isAlive()) {
                n.setTrainSkillActive(false);
                n.startTrainCooldown();
            }
        }
        this.discard();
    }

    private boolean tryChargeHit(ServerLevel serverLevel) {
        Vec3 c = this.position();
        AABB box = new AABB(c, c).inflate(CHARGE_HIT_RADIUS, 1.6, CHARGE_HIT_RADIUS);

        var list = serverLevel.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive);
        if (list.isEmpty()) return false;

        Monster best = null;
        double bestD2 = Double.MAX_VALUE;
        for (var h : list) {
            if (nozomiPassengerUuid != null && h.getUUID().equals(nozomiPassengerUuid)) continue;

            double d2 = h.distanceToSqr(c);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = h;
            }
        }
        if (best == null) return false;

        best.hurt(serverLevel.damageSources().source(DamageTypes.MOB_ATTACK, this), CHARGE_DAMAGE);

        Vec3 push = best.position().subtract(c);
        if (push.horizontalDistanceSqr() < 1.0e-6) push = new Vec3(0, 0, 1);
        Vec3 dir = new Vec3(push.x, 0, push.z).normalize();

        best.knockback((float) KNOCKBACK_H, -dir.x, -dir.z);
        best.setDeltaMovement(best.getDeltaMovement().add(0.0, KNOCKBACK_Y, 0.0));

        return true;
    }

    private static Vec3 forwardFromYaw(float yawDeg) {
        float r = yawDeg * ((float) Math.PI / 180.0f);
        return new Vec3(-Mth.sin(r), 0.0, Mth.cos(r));
    }

    private Vec3 getSeatWorldNozomiOnTrain() {
        Vec3 forward = forwardFromYaw(this.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        return this.position()
                .add(0.0, 0.90, 0.0)
                .add(forward.scale(-0.10))
                .add(right.scale(0.00));
    }

    private Vec3 getSeatWorldGunOnTrain() {
        Vec3 forward = forwardFromYaw(this.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        return this.position()
                .add(0.0, 0.65, 0.0)
                .add(forward.scale(0.05))
                .add(right.scale(1.10));
    }
}