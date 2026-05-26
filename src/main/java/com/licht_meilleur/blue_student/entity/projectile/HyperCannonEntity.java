package com.licht_meilleur.blue_student.entity.projectile;

import com.licht_meilleur.blue_student.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class HyperCannonEntity extends Entity {

    public enum Side {
        LEFT, RIGHT
    }

    public static final int LIFE_TICKS = 20;
    public static final int HIT_INTERVAL = 2;
    public static final float DAMAGE_PER_HIT = 4.0f;
    public static final double MAX_RANGE = 24.0;
    public static final double RADIUS = 0.7;

    private static final double PARTICLE_STEP = 0.6;
    private static final int PARTICLE_EVERY_TICKS = 2;

    private UUID ownerUuid;
    private UUID targetUuid;
    private Side side = Side.LEFT;
    private int ageTicks = 0;
    private int life = LIFE_TICKS;

    public HyperCannonEntity(EntityType<? extends HyperCannonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public HyperCannonEntity(Level level) {
        this(ModEntities.HYPER_CANNON.get(), level);
    }

    public void init(LivingEntity owner, LivingEntity target, Side side) {
        this.ownerUuid = owner.getUUID();
        this.targetUuid = target.getUUID();
        this.side = side;
        this.ageTicks = 0;
        this.life = LIFE_TICKS;

        this.setPos(owner.getX(), owner.getEyeY(), owner.getZ());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {

    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        ageTicks++;
        life--;

        if (life <= 0) {
            this.discard();
            return;
        }

        LivingEntity owner = getLiving(serverLevel, ownerUuid);
        LivingEntity target = getLiving(serverLevel, targetUuid);

        if (owner == null || !owner.isAlive() || target == null || !target.isAlive()) {
            this.discard();
            return;
        }

        Vec3 start = startFrom(owner, target, side);
        Vec3 wantedEnd = target.getEyePosition();
        Vec3 end = clipByBlocks(serverLevel, owner, start, wantedEnd);

        this.setPos(start.x, start.y, start.z);

        if ((ageTicks % HIT_INTERVAL) == 0) {
            damageAlongSegment(serverLevel, owner, start, end);
        }

        if ((ageTicks % PARTICLE_EVERY_TICKS) == 0) {
            spawnSonicBeam(serverLevel, start, end);
        }
    }

    private static Vec3 startFrom(LivingEntity owner, LivingEntity target, Side side) {
        Vec3 base = owner.getEyePosition().subtract(0, 0.10, 0);

        Vec3 toT = target.getEyePosition().subtract(base);
        Vec3 forward = (toT.lengthSqr() < 1.0e-6)
                ? owner.getViewVector(1.0f)
                : toT.normalize();

        Vec3 right = new Vec3(0, 1, 0).cross(forward);
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();

        double sign = (side == Side.LEFT) ? -1.0 : 1.0;

        double sideOff = 0.22 * sign;
        double fwdOff = 0.12;

        return base.add(right.scale(sideOff)).add(forward.scale(fwdOff));
    }

    private static Vec3 clipByBlocks(ServerLevel serverLevel, LivingEntity owner, Vec3 start, Vec3 wantedEnd) {
        HitResult hit = serverLevel.clip(new ClipContext(
                start,
                wantedEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                owner
        ));
        if (hit.getType() != HitResult.Type.MISS) return hit.getLocation();
        return wantedEnd;
    }

    private static void spawnSonicBeam(ServerLevel serverLevel, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double len = dir.length();
        if (len < 0.01) return;

        Vec3 step = dir.normalize().scale(PARTICLE_STEP);
        int steps = (int) Math.ceil(len / PARTICLE_STEP);

        Vec3 p = start;
        for (int i = 0; i <= steps; i++) {
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);
            p = p.add(step);
        }
    }

    private static void damageAlongSegment(ServerLevel serverLevel, LivingEntity owner, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(RADIUS);

        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != owner
        );

        DamageSource src = serverLevel.damageSources().magic();

        for (LivingEntity t : candidates) {
            Vec3 center = t.position().add(0, t.getBbHeight() * 0.5, 0);
            double distSq = distanceSqPointToSegment(center, start, end);
            if (distSq > RADIUS * RADIUS) continue;

            if (!hasLine(serverLevel, owner, start, center)) continue;

            boolean ok = t.hurtServer(serverLevel, src, DAMAGE_PER_HIT);
            if (ok) {
                t.setDeltaMovement(t.getDeltaMovement());
            }
        }
    }

    private static boolean hasLine(ServerLevel serverLevel, LivingEntity owner, Vec3 from, Vec3 to) {
        HitResult hr = serverLevel.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                owner
        ));
        return hr.getType() == HitResult.Type.MISS;
    }

    private static double distanceSqPointToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1.0e-9) return p.distanceToSqr(a);

        double t = p.subtract(a).dot(ab) / abLenSq;
        t = Mth.clamp(t, 0.0, 1.0);
        Vec3 proj = a.add(ab.scale(t));
        return p.distanceToSqr(proj);
    }

    private static LivingEntity getLiving(ServerLevel serverLevel, UUID id) {
        if (id == null) return null;
        Entity e = serverLevel.getEntity(id);
        return (e instanceof LivingEntity le) ? le : null;
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

        String sideName = input.getString("Side").orElse(Side.LEFT.name());
        try {
            side = Side.valueOf(sideName);
        } catch (Exception ignored) {
            side = Side.LEFT;
        }

        ageTicks = input.getInt("AgeTicks").orElse(0);
        life = input.getInt("Life").orElse(LIFE_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
        if (targetUuid != null) {
            output.putString("Target", targetUuid.toString());
        }

        output.putString("Side", side.name());
        output.putInt("AgeTicks", ageTicks);
        output.putInt("Life", life);
    }
}