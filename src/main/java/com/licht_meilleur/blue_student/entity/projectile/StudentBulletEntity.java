package com.licht_meilleur.blue_student.entity.projectile;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class StudentBulletEntity extends Entity {

    private UUID ownerUuid;
    private float damage = 2.0f;

    private boolean bypassIFrames = false;
    private float knockback = 0.0f;

    private int lifeTicks = 80;

    public StudentBulletEntity(EntityType<? extends StudentBulletEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public StudentBulletEntity(Level level, Entity owner, float damage) {
        this(BlueStudentMod.STUDENT_BULLET.get(), level);
        this.setOwnerUuid(owner != null ? owner.getUUID() : null);
        this.damage = damage;
        this.setNoGravity(true);
    }

    public StudentBulletEntity setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        return this;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Entity getOwnerEntity() {
        if (ownerUuid == null) return null;
        if (!(this.level() instanceof ServerLevel serverLevel)) return null;
        return serverLevel.getEntity(ownerUuid);
    }

    public StudentBulletEntity setBypassIFrames(boolean v) {
        this.bypassIFrames = v;
        return this;
    }

    public StudentBulletEntity setKnockback(float kb) {
        this.knockback = kb;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

    }

    @Override
    public void tick() {
        super.tick();

        Vec3 cur = this.position();
        Vec3 vel = this.getDeltaMovement();
        Vec3 next = cur.add(vel);

        HitResult blockHit = this.level().clip(new ClipContext(
                cur,
                next,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        EntityHitResult entityHit = null;
        double bestDist = Double.MAX_VALUE;
        Entity owner = getOwnerEntity();

        for (Entity e : this.level().getEntities(this, this.getBoundingBox().expandTowards(vel).inflate(0.5),
                e -> e instanceof LivingEntity && e.isAlive() && e != this && e != owner)) {

            AABB bb = e.getBoundingBox().inflate(0.25);
            var clip = bb.clip(cur, next);
            if (clip.isPresent()) {
                double d = cur.distanceToSqr(clip.get());
                if (d < bestDist) {
                    bestDist = d;
                    entityHit = new EntityHitResult(e, clip.get());
                }
            }
        }

        HitResult finalHit = blockHit;
        if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS ||
                entityHit.getLocation().distanceToSqr(cur) < blockHit.getLocation().distanceToSqr(cur))) {
            finalHit = entityHit;
        }

        this.setPos(finalHit.getLocation().x, finalHit.getLocation().y, finalHit.getLocation().z);

        if (finalHit instanceof EntityHitResult ehr) {
            onEntityHit(ehr);
            return;
        }

        if (finalHit.getType() != HitResult.Type.MISS) {
            this.discard();
            return;
        }

        this.setPos(next.x, next.y, next.z);

        if (--lifeTicks <= 0) {
            this.discard();
        }
    }

    protected void onEntityHit(EntityHitResult r) {
        Entity hit = r.getEntity();
        Entity owner = getOwnerEntity();
        if (hit == owner) return;

        DamageSource source;
        if (this.level() instanceof ServerLevel serverLevel) {
            source = serverLevel.damageSources().generic();
        } else {
            this.discard();
            return;
        }

        boolean damaged = hit.hurtServer(serverLevel, source, damage);

        if (damaged) {
            if (bypassIFrames && hit instanceof LivingEntity le) {
                le.hurtTime = 0;
            }

            if (knockback > 0.001f && hit instanceof LivingEntity le) {
                Vec3 v = this.getDeltaMovement();
                Vec3 horiz = new Vec3(v.x, 0, v.z);

                if (horiz.lengthSqr() < 1.0e-6) {
                    Vec3 from = le.position().subtract(this.position());
                    horiz = new Vec3(from.x, 0, from.z);
                }

                Vec3 dir = horiz.normalize();
                le.setDeltaMovement(le.getDeltaMovement().add(dir.x * knockback, 0.05, dir.z * knockback));
            }
        }

        this.discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        String owner = input.getString("Owner").orElse("");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);

        damage = (float) input.getDoubleOr("Damage", 2.0);
        bypassIFrames = input.getBooleanOr("BypassIFrames", false);
        knockback = (float) input.getDoubleOr("Knockback", 0.0);
        lifeTicks = input.getIntOr("Life", 80);

        double vx = input.getDoubleOr("Vx", 0.0);
        double vy = input.getDoubleOr("Vy", 0.0);
        double vz = input.getDoubleOr("Vz", 0.0);
        this.setDeltaMovement(vx, vy, vz);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }

        output.putDouble("Damage", damage);
        output.putBoolean("BypassIFrames", bypassIFrames);
        output.putDouble("Knockback", knockback);
        output.putInt("Life", lifeTicks);

        Vec3 v = this.getDeltaMovement();
        output.putDouble("Vx", v.x);
        output.putDouble("Vy", v.y);
        output.putDouble("Vz", v.z);
    }
}