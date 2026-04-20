package com.licht_meilleur.blue_student.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class GunTrainShellEntity extends Entity {

    private UUID ownerUuid;
    private UUID targetUuid;

    private int lifeTicks = 20 * 5;
    private int ageTicks = 0;

    private static final float BLAST_RADIUS = 4.5f;
    private static final float DAMAGE = 8.0f;

    private static final int CURVE_TICKS = 5;
    private static final double STEER = 0.50;
    private static final double MAX_SPEED = 1.35;
    private static final double MIN_SPEED = 0.90;
    private static final double CURVE_FORCE = 0.10;
    private static final double UP_FORCE = 0.015;

    private int curveSign = 1;

    public GunTrainShellEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public GunTrainShellEntity setOwnerUuid(UUID owner) {
        this.ownerUuid = owner;
        return this;
    }

    public GunTrainShellEntity setTarget(LivingEntity target) {
        if (target != null) this.targetUuid = target.getUUID();
        return this;
    }

    public GunTrainShellEntity setCurveSign(int sign) {
        this.curveSign = (sign >= 0) ? 1 : -1;
        return this;
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        ageTicks++;

        if (--lifeTicks <= 0) {
            this.discard();
            return;
        }

        Vec3 cur = this.position();
        serverLevel.sendParticles(ParticleTypes.SMOKE, cur.x, cur.y, cur.z, 1, 0.02, 0.02, 0.02, 0.001);

        steerMissile(serverLevel);
        updateRotationFromVelocity();

        Vec3 vel = this.getDeltaMovement();
        Vec3 next = cur.add(vel);

        HitResult hit = this.level().clip(new ClipContext(
                cur,
                next,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));

        EntityHitResult eHit = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : this.level().getEntities(this, this.getBoundingBox().expandTowards(vel).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && e != this)) {

            AABB bb = e.getBoundingBox().inflate(0.3);
            var clip = bb.clip(cur, next);
            if (clip.isPresent()) {
                double d = cur.distanceToSqr(clip.get());
                if (d < bestDist) {
                    bestDist = d;
                    eHit = new EntityHitResult(e, clip.get());
                }
            }
        }

        HitResult finalHit = hit;
        if (eHit != null && (hit.getType() == HitResult.Type.MISS ||
                eHit.getLocation().distanceToSqr(cur) < hit.getLocation().distanceToSqr(cur))) {
            finalHit = eHit;
        }

        this.setPos(finalHit.getLocation().x, finalHit.getLocation().y, finalHit.getLocation().z);

        if (finalHit.getType() != HitResult.Type.MISS) {
            explodeNoBlock(serverLevel, this.position());
            this.discard();
            return;
        }

        this.setPos(next.x, next.y, next.z);
    }

    private void steerMissile(ServerLevel serverLevel) {
        Vec3 vel = this.getDeltaMovement();
        if (vel.lengthSqr() < 1.0e-9) {
            vel = new Vec3(0, 0, 1).scale(MIN_SPEED);
        }

        double speed = vel.length();
        speed = Mth.clamp(speed, MIN_SPEED, MAX_SPEED);

        Entity te = (targetUuid != null) ? serverLevel.getEntity(targetUuid) : null;
        LivingEntity target = (te instanceof LivingEntity le && le.isAlive()) ? le : null;

        if (target == null) {
            Vec3 n = vel.normalize();
            Vec3 out = new Vec3(n.x, n.y + UP_FORCE, n.z).normalize().scale(speed);
            this.setDeltaMovement(out);
            return;
        }

        Vec3 aimPos = target.getEyePosition();
        Vec3 desiredDir = aimPos.subtract(this.position()).normalize();
        Vec3 curDir = vel.normalize();

        if (ageTicks <= CURVE_TICKS) {
            Vec3 right = curDir.cross(new Vec3(0, 1, 0));
            if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
            right = right.normalize().scale(CURVE_FORCE * curveSign);

            Vec3 bent = curDir.add(right).add(0, UP_FORCE, 0).normalize();
            this.setDeltaMovement(bent.scale(speed));
            return;
        }

        Vec3 mixed = curDir.scale(1.0 - STEER).add(desiredDir.scale(STEER)).normalize();
        mixed = mixed.add(0, UP_FORCE, 0).normalize();

        this.setDeltaMovement(mixed.scale(speed));
    }

    private void explodeNoBlock(ServerLevel serverLevel, Vec3 pos) {
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 18, 0.35, 0.2, 0.35, 0.02);

        serverLevel.playSound(
                null,
                BlockPos.containing(pos),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,
                0.8f,
                1.0f
        );

        AABB box = new AABB(pos, pos).inflate(BLAST_RADIUS, 2.5, BLAST_RADIUS);
        for (Monster h : serverLevel.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            if (h.distanceToSqr(pos) <= (BLAST_RADIUS * BLAST_RADIUS)) {
                h.hurt(serverLevel.damageSources().magic(), DAMAGE);
            }
        }
    }

    private void loadShellData(CompoundTag tag) {
        String owner = tag.getString("Owner").orElse("");
        ownerUuid = owner.isEmpty() ? null : UUID.fromString(owner);

        String target = tag.getString("Target").orElse("");
        targetUuid = target.isEmpty() ? null : UUID.fromString(target);

        lifeTicks = tag.getInt("Life").orElse(0);
        ageTicks = tag.getInt("Age").orElse(0);
        curveSign = tag.getInt("CurveSign").orElse(1);

        double vx = tag.getDouble("Vx").orElse(0.0);
        double vy = tag.getDouble("Vy").orElse(0.0);
        double vz = tag.getDouble("Vz").orElse(0.0);
        this.setDeltaMovement(vx, vy, vz);
    }

    private void saveShellData(CompoundTag tag) {
        if (ownerUuid != null) tag.putString("Owner", ownerUuid.toString());
        if (targetUuid != null) tag.putString("Target", targetUuid.toString());

        tag.putInt("Life", lifeTicks);
        tag.putInt("Age", ageTicks);
        tag.putInt("CurveSign", curveSign);

        Vec3 v = this.getDeltaMovement();
        tag.putDouble("Vx", v.x);
        tag.putDouble("Vy", v.y);
        tag.putDouble("Vz", v.z);
    }

    private void updateRotationFromVelocity() {
        Vec3 v = this.getDeltaMovement();
        if (v.lengthSqr() < 1.0e-8) return;

        float yaw = (float) (Math.toDegrees(Math.atan2(v.z, v.x)) - 90.0f);
        double horiz = Math.sqrt(v.x * v.x + v.z * v.z);
        float pitch = (float) (-Math.toDegrees(Math.atan2(v.y, horiz)));

        this.setYRot(yaw);
        this.setXRot(pitch);
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