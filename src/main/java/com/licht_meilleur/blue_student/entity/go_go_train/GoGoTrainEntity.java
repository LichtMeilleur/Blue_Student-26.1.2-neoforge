package com.licht_meilleur.blue_student.entity.go_go_train;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.licht_meilleur.blue_student.registry.ModEntities;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GoGoTrainEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerPlayerUuid;
    private UUID targetUuid;
    private UUID nozomiPassengerUuid;
    private UUID hikariPassengerUuid;

    private int lifeTicks = 0;
    private static final int MAX_LIFE = 20 * 15;

    private int phaseTicks = 0;
    private int phaseEndTick = 0;
    private boolean isCruisePhase = true;

    private static final int CRUISE_TICKS = 20 * 5;
    private static final int CHARGE_TICKS = 20;

    private static final double CRUISE_SPEED = 0.35;
    private static final double CHARGE_SPEED = 1.25;

    private float theta = 0f;
    private float radius = 9.0f;
    private float omega = 0.12f;
    private boolean clockwise = true;

    private static final float CHARGE_HIT_RADIUS = 1.6f;
    private static final float CHARGE_DAMAGE = 8.0f;
    private static final double CHARGE_KB_H = 1.8;
    private static final double CHARGE_KB_Y = 0.55;
    private static final int CHARGE_MIN_AGE = 6;

    private int fireCooldown = 0;
    private static final int FIRE_CD = 12;

    private static final double HK_BACK = 2.0;
    private static final double HK_RIGHT = -0.5;
    private static final double HK_UP = 0.0;

    private static final EntityDataAccessor<Float> SYNC_BODY_YAW =
            SynchedEntityData.defineId(GoGoTrainEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> SYNC_SHEET2_YAW =
            SynchedEntityData.defineId(GoGoTrainEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> SYNC_TARGET_EID =
            SynchedEntityData.defineId(GoGoTrainEntity.class, EntityDataSerializers.INT);

    public GoGoTrainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNC_BODY_YAW, 0.0f);
        builder.define(SYNC_SHEET2_YAW, 0.0f);
        builder.define(SYNC_TARGET_EID, -1);
    }

    public GoGoTrainEntity setOwnerPlayerUuid(UUID id) {
        this.ownerPlayerUuid = id;
        return this;
    }

    public GoGoTrainEntity setTargetUuid(UUID id) {
        this.targetUuid = id;
        return this;
    }

    public GoGoTrainEntity setNozomiPassengerUuid(UUID id) {
        this.nozomiPassengerUuid = id;
        return this;
    }

    public GoGoTrainEntity setHikariPassengerUuid(UUID id) {
        this.hikariPassengerUuid = id;
        return this;
    }

    public GoGoTrainEntity setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
        return this;
    }

    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    public float getBodyYawDegSynced() {
        return this.entityData.get(SYNC_BODY_YAW);
    }

    public float getSheet2YawDeg() {
        return this.entityData.get(SYNC_SHEET2_YAW);
    }

    public int getSyncedTargetEntityId() {
        return this.entityData.get(SYNC_TARGET_EID);
    }

    private void setBodyYawSynced(float yawDeg) {
        float y = Mth.wrapDegrees(yawDeg);
        this.setYRot(y);
        this.setXRot(0.0f);
        this.setYBodyRot(y);
        this.setYHeadRot(y);
        this.entityData.set(SYNC_BODY_YAW, y);
    }

    private void setSheet2YawServer(float yawDeg) {
        this.entityData.set(SYNC_SHEET2_YAW, Mth.wrapDegrees(yawDeg));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        if (++lifeTicks > MAX_LIFE) {
            discardAndRelease();
            return;
        }
        if (!isOwnerAlive(serverLevel)) {
            discardAndRelease();
            return;
        }

        ensurePassengersMounted(serverLevel);

        Entity target = (targetUuid != null) ? serverLevel.getEntity(targetUuid) : null;
        if (target == null || !target.isAlive()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.entityData.set(SYNC_TARGET_EID, -1);
            return;
        }

        this.entityData.set(SYNC_TARGET_EID, target.getId());

        float targetYaw = computeYawToTarget(target);
        setSheet2YawServer(targetYaw);

        tickPhase(serverLevel, target);

        if (isCruisePhase) {
            if (fireCooldown > 0) fireCooldown--;
            if (fireCooldown == 0) {
                fireCooldown = FIRE_CD;
                if (target instanceof LivingEntity le) {
                    fireTwinCannonsShell(serverLevel, le);
                }
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        faceVelocityAndSyncYaw();
        lockHikariBehindNozomi(serverLevel, targetYaw);
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

    private float computeYawToTarget(Entity target) {
        Vec3 d = target.getEyePosition().subtract(this.position());
        if (d.horizontalDistanceSqr() > 1.0e-6) {
            return (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0f);
        }
        return this.getYRot();
    }

    private void tickPhase(ServerLevel serverLevel, Entity target) {
        if (phaseEndTick <= 0) {
            isCruisePhase = true;
            phaseTicks = 0;
            phaseEndTick = CRUISE_TICKS;
        }

        if (phaseTicks >= phaseEndTick) {
            isCruisePhase = !isCruisePhase;
            phaseTicks = 0;
            phaseEndTick = isCruisePhase ? CRUISE_TICKS : CHARGE_TICKS;
        }

        if (isCruisePhase) {
            tickCruiseFree(serverLevel, target);
        } else {
            tickCharge(serverLevel, target);
        }

        phaseTicks++;
    }

    private void tickCruiseFree(ServerLevel serverLevel, Entity target) {
        theta += (clockwise ? 1 : -1) * Math.abs(omega);

        Vec3 goal = pickCruiseGoal(serverLevel, target);
        Vec3 dir = goal.subtract(this.position());

        Vec3 v = dir.lengthSqr() > 1.0e-6
                ? dir.normalize().scale(CRUISE_SPEED)
                : Vec3.ZERO;

        this.setDeltaMovement(v);
    }

    private Vec3 pickCruiseGoal(ServerLevel serverLevel, Entity target) {
        Vec3 center = target.position();

        float base = theta;
        float step = 0.70f;
        double r = radius;
        double gy = center.y + 0.2;

        for (int i = 0; i < 6; i++) {
            float a = base + (clockwise ? 1 : -1) * (i * step);
            double gx = center.x + Math.cos(a) * r;
            double gz = center.z + Math.sin(a) * r;
            Vec3 goal = new Vec3(gx, gy, gz);

            if (isLineClear(serverLevel, this.position(), goal)) {
                theta = a;
                return goal;
            }
        }

        return new Vec3(center.x, gy, center.z);
    }

    private boolean isLineClear(ServerLevel serverLevel, Vec3 from, Vec3 to) {
        HitResult hit = serverLevel.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void tickCharge(ServerLevel serverLevel, Entity target) {
        Vec3 from = this.position();
        Vec3 to = target.position().add(0, 0.2, 0);
        Vec3 d = to.subtract(from);

        if (d.lengthSqr() > 1.0e-6) {
            this.setDeltaMovement(d.normalize().scale(CHARGE_SPEED));
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }

        if (lifeTicks > CHARGE_MIN_AGE) {
            tryChargeHit(serverLevel);
        }
    }

    private void tryChargeHit(ServerLevel serverLevel) {
        Vec3 c = this.position();
        AABB box = new AABB(c, c).inflate(CHARGE_HIT_RADIUS, 1.6, CHARGE_HIT_RADIUS);

        var list = serverLevel.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive);
        if (list.isEmpty()) return;

        Monster best = null;
        double bestD2 = Double.MAX_VALUE;
        for (var h : list) {
            double d2 = h.distanceToSqr(c);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = h;
            }
        }
        if (best == null) return;

        DamageSource src = serverLevel.damageSources().generic();
        best.hurt(src, CHARGE_DAMAGE);

        applyStrongKnockback(best, c);
    }

    private void applyStrongKnockback(LivingEntity victim, Vec3 fromPos) {
        Vec3 push = victim.position().subtract(fromPos);
        if (push.horizontalDistanceSqr() < 1.0e-6) push = new Vec3(0, 0, 1);

        Vec3 dir = new Vec3(push.x, 0, push.z).normalize();
        Vec3 kb = dir.scale(CHARGE_KB_H).add(0, CHARGE_KB_Y, 0);

        victim.setDeltaMovement(kb);
    }

    private void faceVelocityAndSyncYaw() {
        Vec3 v = this.getDeltaMovement();
        if (v.horizontalDistanceSqr() < 1.0e-6) {
            this.entityData.set(SYNC_BODY_YAW, Mth.wrapDegrees(this.getYRot()));
            return;
        }

        float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0f);
        setBodyYawSynced(yaw);
    }

    private void fireTwinCannonsShell(ServerLevel serverLevel, LivingEntity target) {
        Vec3 startL = getMuzzlePos(WeaponSpec.MuzzleLocator.LEFT_SUB_MUZZLE);
        Vec3 startR = getMuzzlePos(WeaponSpec.MuzzleLocator.RIGHT_SUB_MUZZLE);

        Vec3 dirL = target.getEyePosition().subtract(startL).normalize();
        Vec3 dirR = target.getEyePosition().subtract(startR).normalize();

        spawnShell(serverLevel, target, startL, dirL, -1);
        spawnShell(serverLevel, target, startR, dirR, +1);
    }

    private void spawnShell(ServerLevel serverLevel, LivingEntity target, Vec3 start, Vec3 dir, int curveSign) {
        GunTrainShellEntity shell = new GunTrainShellEntity(ModEntities.GUN_TRAIN_SHELL, serverLevel)
                .setOwnerUuid(ownerPlayerUuid)
                .setTarget(target)
                .setCurveSign(curveSign);

        shell.setPos(start.x, start.y, start.z);
        shell.setDeltaMovement(dir.normalize().scale(1.25));
        serverLevel.addFreshEntity(shell);
    }

    private Vec3 getMuzzlePos(WeaponSpec.MuzzleLocator loc) {
        Vec3 forward = forwardFromYaw(this.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 base = this.position().add(0, 0.75, 0);

        return switch (loc) {
            case LEFT_SUB_MUZZLE -> base.add(right.scale(-0.75)).add(forward.scale(0.65));
            case RIGHT_SUB_MUZZLE -> base.add(right.scale(0.75)).add(forward.scale(0.65));
            default -> base.add(forward.scale(0.65));
        };
    }

    private static Vec3 forwardFromYaw(float yawDeg) {
        float r = yawDeg * ((float) Math.PI / 180.0f);
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private void ensurePassengersMounted(ServerLevel serverLevel) {
        if (nozomiPassengerUuid != null) {
            Entity n = serverLevel.getEntity(nozomiPassengerUuid);
            if (n instanceof NozomiEntity noz && noz.isAlive()) {
                if (noz.getVehicle() != this) {
                    noz.stopRiding();
                    noz.startRiding(this);
                }
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        float bodyYaw = this.getYRot();

        if (passenger instanceof NozomiEntity) {
            Vec3 seat = getSeatWorldNozomi();
            passenger.setPos(seat.x, seat.y, seat.z);
            setPassengerYawOnly(passenger, bodyYaw);
            return;
        }

        Vec3 seat = this.position().add(0, 0.9, 0);
        passenger.setPos(seat.x, seat.y, seat.z);
        setPassengerYawOnly(passenger, bodyYaw);
    }

    private void setPassengerYawOnly(Entity passenger, float yaw) {
        passenger.setYRot(yaw);

        if (passenger instanceof LivingEntity le) {
            le.setYBodyRot(yaw);
            le.setYHeadRot(yaw);
        }
    }

    private Vec3 getSeatWorldNozomi() {
        Vec3 forward = forwardFromYaw(this.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        return this.position()
                .add(0, 0.90, 0)
                .add(forward.scale(-0.10))
                .add(right.scale(0.00));
    }

    private void lockHikariBehindNozomi(ServerLevel serverLevel, float lookYaw) {
        if (hikariPassengerUuid == null || nozomiPassengerUuid == null) return;

        Entity h = serverLevel.getEntity(hikariPassengerUuid);
        Entity n = serverLevel.getEntity(nozomiPassengerUuid);

        if (!(h instanceof HikariEntity hk) || !hk.isAlive()) return;
        if (!(n instanceof NozomiEntity noz) || !noz.isAlive()) return;

        if (hk.getVehicle() != null) hk.stopRiding();

        float baseYaw = noz.getYRot();
        Vec3 forward = forwardFromYaw(baseYaw).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        Vec3 base = noz.position();
        Vec3 pos = base
                .add(0, HK_UP, 0)
                .add(forward.scale(-HK_BACK))
                .add(right.scale(HK_RIGHT));

        hk.setPos(pos.x, pos.y, pos.z);
        hk.setYRot(Mth.wrapDegrees(lookYaw));
        hk.setXRot(0f);
        hk.setYBodyRot(hk.getYRot());
        hk.setYHeadRot(hk.getYRot());

        hk.setDeltaMovement(Vec3.ZERO);
        hk.setNoGravity(true);
        hk.noPhysics = true;
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

    private void discardAndRelease() {
        for (Entity p : this.getPassengers()) {
            p.stopRiding();
        }
        this.discard();
    }

    private void loadGoGoTrainData(CompoundTag tag) {
        String owner = tag.getString("OwnerP").orElse("");
        ownerPlayerUuid = owner.isEmpty() ? null : UUID.fromString(owner);

        String target = tag.getString("Target").orElse("");
        targetUuid = target.isEmpty() ? null : UUID.fromString(target);

        String nozomi = tag.getString("NozomiP").orElse("");
        nozomiPassengerUuid = nozomi.isEmpty() ? null : UUID.fromString(nozomi);

        String hikari = tag.getString("HikariP").orElse("");
        hikariPassengerUuid = hikari.isEmpty() ? null : UUID.fromString(hikari);

        lifeTicks = tag.getInt("Life").orElse(0);
        phaseTicks = tag.getInt("PhaseT").orElse(0);
        phaseEndTick = tag.getInt("PhaseE").orElse(0);
        isCruisePhase = tag.getBoolean("Cruise").orElse(true);

        theta = tag.getFloat("Theta").orElse(0f);
        radius = tag.getFloat("Radius").orElse(9.0f);
        omega = tag.getFloat("Omega").orElse(0.12f);
        clockwise = tag.getBoolean("Clockwise").orElse(true);

        fireCooldown = tag.getInt("FireCd").orElse(0);
    }

    private void saveGoGoTrainData(CompoundTag tag) {
        if (ownerPlayerUuid != null) tag.putString("OwnerP", ownerPlayerUuid.toString());
        if (targetUuid != null) tag.putString("Target", targetUuid.toString());
        if (nozomiPassengerUuid != null) tag.putString("NozomiP", nozomiPassengerUuid.toString());
        if (hikariPassengerUuid != null) tag.putString("HikariP", hikariPassengerUuid.toString());

        tag.putInt("Life", lifeTicks);
        tag.putInt("PhaseT", phaseTicks);
        tag.putInt("PhaseE", phaseEndTick);
        tag.putBoolean("Cruise", isCruisePhase);

        tag.putFloat("Theta", theta);
        tag.putFloat("Radius", radius);
        tag.putFloat("Omega", omega);
        tag.putBoolean("Clockwise", clockwise);

        tag.putInt("FireCd", fireCooldown);
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
}