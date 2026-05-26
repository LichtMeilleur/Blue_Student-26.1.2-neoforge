package com.licht_meilleur.blue_student.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GunTrainEntity extends Entity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private UUID ownerPlayerUuid;
    private UUID trainUuid;
    private UUID passengerStudentUuid;

    private int fireCooldown = 0;
    private int lifeTicks = 0;
    private static final int MAX_LIFE = 20 * 15;

    private Vec3 anchorPos = null;

    private boolean mergedMode = false;

    private static final EntityDataAccessor<Integer> SYNC_TARGET_EID =
            SynchedEntityData.defineId(GunTrainEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> SYNC_SHEET_YAW =
            SynchedEntityData.defineId(GunTrainEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Float> SYNC_BODY_YAW =
            SynchedEntityData.defineId(GunTrainEntity.class, EntityDataSerializers.FLOAT);

    public GunTrainEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public GunTrainEntity setMergedMode(boolean v) {
        this.mergedMode = v;
        return this;
    }

    public boolean isMergedMode() {
        return mergedMode;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SYNC_BODY_YAW, 0.0f);
        builder.define(SYNC_SHEET_YAW, 0.0f);
        builder.define(SYNC_TARGET_EID, -1);
    }

    public float getBodyYawDegSynced() {
        return this.entityData.get(SYNC_BODY_YAW);
    }

    private void setBodyYawServer(float yaw) {
        this.entityData.set(SYNC_BODY_YAW, Mth.wrapDegrees(yaw));
    }

    public float getSheetYawDeg() {
        return this.entityData.get(SYNC_SHEET_YAW);
    }

    private void setSheetYawServer(float yaw) {
        this.entityData.set(SYNC_SHEET_YAW, Mth.wrapDegrees(yaw));
    }

    public GunTrainEntity setOwnerPlayerUuid(UUID ownerPlayerUuid) {
        this.ownerPlayerUuid = ownerPlayerUuid;
        return this;
    }

    public GunTrainEntity setTrainUuid(UUID trainUuid) {
        this.trainUuid = trainUuid;
        return this;
    }

    public GunTrainEntity setPassengerStudentUuid(UUID passengerStudentUuid) {
        this.passengerStudentUuid = passengerStudentUuid;
        return this;
    }

    public UUID getOwnerPlayerUuid() {
        return ownerPlayerUuid;
    }

    public UUID getTrainUuid() {
        return trainUuid;
    }

    public UUID getPassengerStudentUuid() {
        return passengerStudentUuid;
    }

    public void setAnchorPos(Vec3 p) {
        this.anchorPos = p;
    }

    public int getSyncedTargetEntityId() {
        return this.entityData.get(SYNC_TARGET_EID);
    }

    private void setYawStableServer(float yaw) {
        float y = Mth.wrapDegrees(yaw);
        this.setYRot(y);
        this.setXRot(0.0f);
        this.yRotO = y;
        setBodyYawServer(y);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel sw)) return;

        tickAnchor();

        if (++lifeTicks > MAX_LIFE) {
            discardAndNotify(sw);
            return;
        }
        if (!isOwnerAlive(sw)) {
            discardAndNotify(sw);
            return;
        }

        if (trainUuid != null) {
            Entity t = sw.getEntity(trainUuid);
            if (t == null || !t.isAlive()) {
                discardAndNotify(sw);
                return;
            }
        }

        LivingEntity target = findTarget(sw);
        this.entityData.set(SYNC_TARGET_EID, target != null ? target.getId() : -1);

        if (this.isPassenger()) this.stopRiding();

        if (target != null) {
            Vec3 d = target.getEyePosition().subtract(this.position());
            if (d.horizontalDistanceSqr() > 1.0e-6) {
                float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0f);
                setYawStableServer(yaw);
            }
        }

        if (target != null) {
            Vec3 d = target.getEyePosition().subtract(this.position());
            if (d.horizontalDistanceSqr() > 1.0e-6) {
                float aimYaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0f);
                setSheetYawServer(aimYaw);
            }
        } else {
            setSheetYawServer(this.getYRot());
        }

        ensurePassengerMounted(sw);

        boolean allowFire = true;
        if (this.getVehicle() instanceof TrainEntity t) {
            allowFire = t.isGunFireEnabled();
        }

        if (allowFire) {
            if (fireCooldown > 0) fireCooldown--;
            if (fireCooldown == 0) {
                fireCooldown = 12;
                fireTwinCannonsShell(sw);
            }
        }
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

    private void tickAnchor() {
        if (anchorPos == null) return;
        if (this.isPassenger()) return;

        this.setPos(anchorPos.x, anchorPos.y, anchorPos.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.hurtMarked = true;
    }

    private void ensurePassengerMounted(ServerLevel sw) {
        if (passengerStudentUuid == null) return;
        Entity p = sw.getEntity(passengerStudentUuid);
        if (!(p instanceof AbstractStudentEntity st) || !st.isAlive()) return;

        if (st.getVehicle() != this) {
            st.stopRiding();
            st.startRiding(this);
        }
    }

    private void fireTwinCannonsShell(ServerLevel sw) {
        LivingEntity target = findTarget(sw);
        if (target == null) {
            this.entityData.set(SYNC_TARGET_EID, -1);
            return;
        }

        this.entityData.set(SYNC_TARGET_EID, target.getId());

        Vec3 startL = getMuzzlePos(WeaponSpec.MuzzleLocator.LEFT_SUB_MUZZLE);
        Vec3 startR = getMuzzlePos(WeaponSpec.MuzzleLocator.RIGHT_SUB_MUZZLE);

        Vec3 dirL = target.getEyePosition().subtract(startL).normalize();
        Vec3 dirR = target.getEyePosition().subtract(startR).normalize();

        spawnShell(sw, target, startL, dirL, -1);
        spawnShell(sw, target, startR, dirR, +1);
    }

    private void spawnShell(ServerLevel sw, LivingEntity target, Vec3 start, Vec3 dir, int curveSign) {
        GunTrainShellEntity shell = new GunTrainShellEntity(
                com.licht_meilleur.blue_student.registry.ModEntities.GUN_TRAIN_SHELL.get(),
                sw
        ).setOwnerUuid(ownerPlayerUuid)
                .setTarget(target)
                .setCurveSign(curveSign);

        shell.setPos(start.x, start.y, start.z);

        double speed = 1.25;
        shell.setDeltaMovement(dir.normalize().scale(speed));

        sw.addFreshEntity(shell);
    }

    private LivingEntity findTarget(ServerLevel sw) {
        double r = 18.0;
        AABB box = this.getBoundingBox().inflate(r, 6.0, r);
        var list = sw.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive);
        if (list.isEmpty()) return null;

        Monster best = null;
        double bestD2 = 1e18;
        for (Monster h : list) {
            double d2 = this.distanceToSqr(h);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = h;
            }
        }
        return best;
    }

    private Vec3 getMuzzlePos(WeaponSpec.MuzzleLocator loc) {
        Vec3 forward = forwardFromYaw(this.getYRot()).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 base = this.position().add(0, 0.75, 0);

        return switch (loc) {
            case LEFT_SUB_MUZZLE -> base.add(right.scale(-0.75)).add(forward.scale(0.65));
            case RIGHT_SUB_MUZZLE -> base.add(right.scale(+0.75)).add(forward.scale(0.65));
            default -> base.add(forward.scale(0.65));
        };
    }

    private static Vec3 forwardFromYaw(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);

        Vec3 seat = getSeatWorldGun();
        float y = this.getYRot();

        passenger.setPos(seat.x, seat.y, seat.z);
        passenger.setYRot(y);
        passenger.setXRot(0.0f);

        if (passenger instanceof LivingEntity le) {
            le.yBodyRot = y;
            le.yHeadRot = y;
            le.yBodyRotO = y;
            le.yHeadRotO = y;
        }

        passenger.noPhysics = true;
        passenger.setNoGravity(true);
        passenger.setDeltaMovement(Vec3.ZERO);
        passenger.hurtMarked = true;
    }


    private void loadGunTrainData(CompoundTag tag) {
        if (tag.contains("OwnerP")) {
            String s = tag.getString("OwnerP").orElse("");
            ownerPlayerUuid = s.isEmpty() ? null : UUID.fromString(s);
        }
        if (tag.contains("Train")) {
            String s = tag.getString("Train").orElse("");
            trainUuid = s.isEmpty() ? null : UUID.fromString(s);
        }
        if (tag.contains("Passenger")) {
            String s = tag.getString("Passenger").orElse("");
            passengerStudentUuid = s.isEmpty() ? null : UUID.fromString(s);
        }

        fireCooldown = tag.getInt("FireCd").orElse(0);
        lifeTicks = tag.getInt("Life").orElse(0);

        if (tag.contains("Ax")) {
            anchorPos = new Vec3(
                    tag.getDouble("Ax").orElse(0.0),
                    tag.getDouble("Ay").orElse(0.0),
                    tag.getDouble("Az").orElse(0.0)
            );
        } else {
            anchorPos = null;
        }
    }

    private void saveGunTrainData(CompoundTag tag) {
        if (ownerPlayerUuid != null) tag.putString("OwnerP", ownerPlayerUuid.toString());
        if (trainUuid != null) tag.putString("Train", trainUuid.toString());
        if (passengerStudentUuid != null) tag.putString("Passenger", passengerStudentUuid.toString());

        tag.putInt("FireCd", fireCooldown);
        tag.putInt("Life", lifeTicks);

        if (anchorPos != null) {
            tag.putDouble("Ax", anchorPos.x);
            tag.putDouble("Ay", anchorPos.y);
            tag.putDouble("Az", anchorPos.z);
        }
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
                    state.setAnimation(RawAnimation.begin().thenLoop("animation.go"));
                    return PlayState.CONTINUE;
                }
        ));
    }

    private static Vec3 locatorPxToWorld(Vec3 locPx, float yawDeg, Vec3 basePos) {
        double yawRad = Math.toRadians(yawDeg);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        double lx = locPx.x / 16.0;
        double ly = locPx.y / 16.0;
        double lz = locPx.z / 16.0;

        double rx = lx * cos - lz * sin;
        double rz = lx * sin + lz * cos;

        return basePos.add(rx, ly, rz);
    }

    private static final Vec3 SEAT_PX = new Vec3(0.4, 13.4, 6.6);
    private static final Vec3 SEAT_TUNE = new Vec3(0.0, 0.0, -12.0);

    private Vec3 getSeatWorldGun() {
        return locatorPxToWorld(SEAT_PX.add(SEAT_TUNE), this.getYRot(), this.position());
    }

    private boolean isOwnerAlive(ServerLevel sw) {
        if (ownerPlayerUuid == null) return false;

        for (HikariEntity h : sw.getEntitiesOfClass(
                HikariEntity.class,
                this.getBoundingBox().inflate(128),
                e -> e.isAlive()
        )) {
            if (ownerPlayerUuid.equals(h.getOwnerUuid())) return true;
        }
        return false;
    }

    private void discardAndNotify(ServerLevel sw) {
        if (passengerStudentUuid != null) {
            Entity p = sw.getEntity(passengerStudentUuid);
            if (p instanceof HikariEntity h) {
                h.startGunTrainCooldown();
            }
        }
        this.discard();
    }
}