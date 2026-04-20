package com.licht_meilleur.blue_student.weapon;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.projectile.StudentBulletEntity;
import com.licht_meilleur.blue_student.network.ServerFx;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ProjectileWeaponAction implements WeaponAction {

    @Override
    public boolean shoot(IStudentEntity shooter, LivingEntity target, WeaponSpec spec) {
        if (!(shooter instanceof Entity shooterEntity)) return false;
        if (!(shooterEntity.level() instanceof ServerLevel sw)) return false;
        if (target == null || !target.isAlive()) return false;

        float damage = spec.damage;
        if (shooterEntity instanceof AbstractStudentEntity se && se.hasKisakiSupportBuff()) {
            damage *= 1.25f;
        }

        final Vec3 spawnPos = (shooterEntity instanceof AbstractStudentEntity se)
                ? se.getMuzzlePosFor(spec)
                : shooterEntity.getEyePosition();

        if (shooterEntity.tickCount % 10 == 0) {
            System.out.println("[MUZZLE] spec=" + spec.muzzleLocator
                    + " shooterPos=" + shooterEntity.position()
                    + " eye=" + shooterEntity.getEyePosition()
                    + " spawn=" + spawnPos
                    + " isClient=" + shooterEntity.level().isClientSide());
        }

        RandomSource r = sw.getRandom();

        int pellets = Math.max(1, spec.pellets);
        Vec3[] fxDirs = new Vec3[pellets];

        Vec3 baseAim = target.getEyePosition().subtract(spawnPos).normalize();

        for (int i = 0; i < pellets; i++) {
            Vec3 dir = applySpread(baseAim, spec.spreadRad, r);
            fxDirs[i] = dir;

            StudentBulletEntity bullet = new StudentBulletEntity(sw, shooterEntity, damage)
                    .setBypassIFrames(spec.bypassIFrames)
                    .setKnockback(spec.knockback);

            bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            bullet.setYRot(shooterEntity.getYRot());
            bullet.setXRot(shooterEntity.getXRot());
            bullet.setDeltaMovement(dir.normalize().scale(spec.projectileSpeed));

            sw.addFreshEntity(bullet);
        }

        float travelDist = (float) spec.range;

        ServerFx.sendShotFx(
                sw,
                shooterEntity.getId(),
                spawnPos,
                spec.fxType,
                spec.fxWidth,
                fxDirs,
                travelDist
        );

        return true;
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

    public boolean shootFromCustomPos(Entity damageOwner,
                                      LivingEntity target,
                                      WeaponSpec spec,
                                      Vec3 spawnPos,
                                      Vec3 dir) {

        if (!(damageOwner.level() instanceof ServerLevel sw)) return false;
        if (target == null || !target.isAlive()) return false;

        RandomSource r = sw.getRandom();

        int pellets = Math.max(1, spec.pellets);
        Vec3 base = dir.normalize();

        for (int i = 0; i < pellets; i++) {
            Vec3 d = applySpread(base, spec.spreadRad, r);

            StudentBulletEntity bullet = new StudentBulletEntity(sw, damageOwner, spec.damage)
                    .setBypassIFrames(spec.bypassIFrames)
                    .setKnockback(spec.knockback);

            bullet.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            bullet.setYRot(damageOwner.getYRot());
            bullet.setXRot(damageOwner.getXRot());
            bullet.setDeltaMovement(d.normalize().scale(spec.projectileSpeed));

            sw.addFreshEntity(bullet);
        }

        return true;
    }
}