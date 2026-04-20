package com.licht_meilleur.blue_student.entity.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class SonicBeamEntity extends Entity {

    private UUID ownerUuid;
    private UUID targetUuid;

    private Vec3 start = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;

    private int life = 20;

    public SonicBeamEntity(EntityType<? extends SonicBeamEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void init(Vec3 start, Vec3 end) {
        this.start = start;
        this.end = end;
    }

    public SonicBeamEntity setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        return this;
    }

    public SonicBeamEntity setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
        return this;
    }

    public Vec3 getStart() {
        return start;
    }

    public Vec3 getEnd() {
        return end;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        LivingEntity owner = getLiving(serverLevel, ownerUuid);
        LivingEntity target = getLiving(serverLevel, targetUuid);

        if (owner == null || target == null) {
            discard();
            return;
        }

        start = owner.getEyePosition();
        end = target.getEyePosition();

        life--;
        if (life <= 0) {
            discard();
        }
    }

    private static LivingEntity getLiving(ServerLevel serverLevel, UUID uuid) {
        if (uuid == null) return null;
        Entity e = serverLevel.getEntity(uuid);
        return (e instanceof LivingEntity le && le.isAlive()) ? le : null;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String owner = input.getString("Owner").orElse("");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);

        String target = input.getString("Target").orElse("");
        targetUuid = target.isEmpty() ? null : UUID.fromString(target);

        double sx = input.getDoubleOr("StartX", 0.0);
        double sy = input.getDoubleOr("StartY", 0.0);
        double sz = input.getDoubleOr("StartZ", 0.0);
        start = new Vec3(sx, sy, sz);

        double ex = input.getDoubleOr("EndX", 0.0);
        double ey = input.getDoubleOr("EndY", 0.0);
        double ez = input.getDoubleOr("EndZ", 0.0);
        end = new Vec3(ex, ey, ez);

        life = input.getIntOr("Life", 20);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
        if (targetUuid != null) {
            output.putString("Target", targetUuid.toString());
        }

        output.putDouble("StartX", start.x);
        output.putDouble("StartY", start.y);
        output.putDouble("StartZ", start.z);

        output.putDouble("EndX", end.x);
        output.putDouble("EndY", end.y);
        output.putDouble("EndZ", end.z);

        output.putInt("Life", life);
    }
}