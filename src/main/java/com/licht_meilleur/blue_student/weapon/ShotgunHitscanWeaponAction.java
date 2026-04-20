package com.licht_meilleur.blue_student.weapon;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.network.ServerFx;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ShotgunHitscanWeaponAction implements WeaponAction {

    @Override
    public boolean shoot(IStudentEntity shooter, LivingEntity target, WeaponSpec spec) {
        if (!(shooter instanceof Entity shooterEntity)) return false;
        if (!(shooterEntity.level() instanceof ServerLevel sw)) return false;
        if (target == null || !target.isAlive()) return false;

        float damage = spec.damage;
        if (shooterEntity instanceof AbstractStudentEntity se && se.hasKisakiSupportBuff()) {
            damage *= 1.25f;
        }

        final Vec3 start = (shooterEntity instanceof AbstractStudentEntity se)
                ? se.getMuzzlePosFor(spec)
                : shooterEntity.getEyePosition();

        RandomSource r = sw.getRandom();

        int pellets = Math.max(1, spec.pellets);
        Vec3[] dirs = new Vec3[pellets];

        double dist = target.getEyePosition().distanceTo(start);
        float spread = spec.spreadRad * (float) (1.0 + dist / 6.0);

        Vec3 baseAim = target.getEyePosition().subtract(start).normalize();

        int hitCount = 0;

        float travelDist = computeTravelDistToBlock(sw, shooterEntity, start, baseAim, (float) spec.range);

        for (int i = 0; i < pellets; i++) {
            Vec3 dir = applySpread(baseAim, spread, r);
            dirs[i] = dir;

            Vec3 end = start.add(dir.scale(spec.range));

            HitResult bh = sw.clip(new ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    shooterEntity
            ));
            if (bh.getType() != HitResult.Type.MISS) {
                end = bh.getLocation();
            }

            EntityHitResult ehr = raycastLiving(sw, shooterEntity, start, end);
            if (ehr != null && ehr.getEntity() == target) {
                hitCount++;
            }
        }

        if (hitCount > 0) {
            DamageSource ds = sw.damageSources().mobAttack((LivingEntity) shooterEntity);
            float total = damage * hitCount;
            target.hurtServer(sw, ds, total);

            if (spec.knockback > 0.001f) {
                target.setDeltaMovement(target.getDeltaMovement().add(
                        baseAim.x * 0.18 * spec.knockback,
                        0.05 * spec.knockback,
                        baseAim.z * 0.18 * spec.knockback
                ));
            }
        }

        ServerFx.sendShotFx(
                sw,
                shooterEntity.getId(),
                start,
                spec.fxType,
                spec.fxWidth,
                dirs,
                travelDist
        );

        return true;
    }

    private float computeTravelDistToBlock(ServerLevel sw, Entity shooter, Vec3 start, Vec3 dir, float maxRange) {
        Vec3 end = start.add(dir.normalize().scale(maxRange));
        HitResult hit = sw.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));
        if (hit.getType() != HitResult.Type.MISS) {
            return (float) start.distanceTo(hit.getLocation());
        }
        return maxRange;
    }

    private EntityHitResult raycastLiving(ServerLevel sw, Entity shooter, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(0.6);

        EntityHitResult best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : sw.getEntities(shooter, box, ent ->
                ent instanceof LivingEntity && ent.isAlive() && ent != shooter)) {

            AABB bb = e.getBoundingBox().inflate(0.3);
            var clip = bb.clip(start, end);
            if (clip.isPresent()) {
                double d = start.distanceToSqr(clip.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = new EntityHitResult(e, clip.get());
                }
            }
        }

        return best;
    }

    private Vec3 applySpread(Vec3 dir, float spreadRad, RandomSource r) {
        if (spreadRad <= 0.0001f) return dir;

        double yaw = (r.nextDouble() * 2.0 - 1.0) * spreadRad;
        double pitch = (r.nextDouble() * 2.0 - 1.0) * spreadRad;

        Vec3 d = dir;

        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        d = new Vec3(d.x * cosY - d.z * sinY, d.y, d.x * sinY + d.z * cosY);

        double cosP = Math.cos(pitch);
        double sinP = Math.sin(pitch);
        d = new Vec3(d.x, d.y * cosP - d.z * sinP, d.y * sinP + d.z * cosP);

        return d.normalize();
    }
}