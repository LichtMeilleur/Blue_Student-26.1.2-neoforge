package com.licht_meilleur.blue_student.weapon;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.network.ServerFx;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class HitscanWeaponAction implements WeaponAction {

    @Override
    public boolean shoot(IStudentEntity shooter, LivingEntity target, WeaponSpec spec) {
        if (!(shooter instanceof Entity shooterEntity)) return false;
        if (!(shooterEntity.level() instanceof ServerLevel sw)) return false;

        float damage = spec.damage;
        if (shooterEntity instanceof AbstractStudentEntity se && se.hasKisakiSupportBuff()) {
            damage *= 1.25f;
        }

        final Vec3 start = (shooterEntity instanceof AbstractStudentEntity se)
                ? se.getMuzzlePosApprox()
                : shooterEntity.getEyePosition();

        Vec3 dir = (target != null && target.isAlive())
                ? target.getEyePosition().subtract(start).normalize()
                : shooterEntity.getViewVector(1.0f).normalize();

        final double maxRange = spec.range;
        Vec3 end = start.add(dir.scale(maxRange));

        HitResult blockHit = sw.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooterEntity
        ));

        double travelDist = maxRange;

        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
            travelDist = start.distanceTo(end);
        }

        Entity hit = raycastLiving(sw, shooterEntity, start, end);
        if (hit instanceof LivingEntity le && le.isAlive()) {
            DamageSource ds = sw.damageSources().mobAttack((LivingEntity) shooterEntity);
            le.hurtServer(sw, ds, damage);

            if (spec.knockback > 0.001f) {
                le.setDeltaMovement(le.getDeltaMovement().add(
                        dir.x * 0.2 * spec.knockback,
                        0.05 * spec.knockback,
                        dir.z * 0.2 * spec.knockback
                ));
            }
        }

        Vec3[] fxDirs = new Vec3[]{dir};

        ServerFx.sendShotFx(
                sw,
                shooterEntity.getId(),
                start,
                spec.fxType,
                spec.fxWidth,
                fxDirs,
                (float) travelDist
        );

        return true;
    }

    private Entity raycastLiving(ServerLevel sw, Entity shooter, Vec3 start, Vec3 end) {
        AABB box = new AABB(start, end).inflate(1.0);

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

        return best != null ? best.getEntity() : null;
    }
}