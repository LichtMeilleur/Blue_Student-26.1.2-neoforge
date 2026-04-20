package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class StudentCombatGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private int cooldown = 0;
    private LivingEntity target;

    private static final double COMBAT_CHASE_SPEED = 1.35;
    private static final double COMBAT_AIM_SPEED = 1.10;

    private static final int REPATH_INTERVAL = 16;
    private int repathCooldown = 0;

    private static final double GUARD_RADIUS = 16.0;

    public StudentCombatGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean isBr() {
        return (student instanceof AbstractStudentEntity ase)
                && ase.getForm() == StudentForm.BR;
    }

    @Override
    public boolean canUse() {
        if (mob instanceof AbstractStudentEntity se && se.getForm() == StudentForm.BR) return false;
        if (isBr()) return false;

        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        target = findTarget();

        if (target != null) {
            mob.setTarget(target);
        }
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob instanceof AbstractStudentEntity se && se.getForm() == StudentForm.BR) return false;
        if (isBr()) return false;
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (target == null || !target.isAlive()) return false;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double keep = spec.range + 8.0;
        return mob.distanceToSqr(target) <= keep * keep;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void stop() {
        target = null;
        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        boolean flying = (mob instanceof HinaEntity hina) && hina.isFlying();

        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return;

        if (cooldown > 0) cooldown--;
        if (repathCooldown > 0) repathCooldown--;

        if (target == null || !target.isAlive()) {
            target = findTarget();
            mob.setTarget(target);
            mob.getNavigation().stop();
            return;
        }

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(target);

        if (!flying) {
            if (dist > spec.preferredMaxRange || dist > spec.range) {
                tryMoveTowardTarget(COMBAT_CHASE_SPEED, spec);
                return;
            }

            if (!mob.getSensing().hasLineOfSight(target)) {
                tryMoveTowardTarget(COMBAT_AIM_SPEED, spec);
                return;
            }

            if (dist < spec.preferredMinRange) {
                mob.getNavigation().stop();
                return;
            }
        }

        if (!mob.getSensing().hasLineOfSight(target)) {
            tryMoveTowardTarget(COMBAT_AIM_SPEED, spec);
            return;
        }

        if (dist < spec.preferredMinRange) {
            mob.getNavigation().stop();
            return;
        }

        if (student.isReloading()) {
            student.tickReload(spec);

            if (dist < spec.panicRange) {
                tryMoveAwayFromTarget(COMBAT_CHASE_SPEED, spec);
            } else {
                mob.getNavigation().stop();
            }
            return;
        }

        if (student.getAmmoInMag() <= 0 && !spec.infiniteAmmo) {
            if (dist < spec.panicRange) {
                tryMoveAwayFromTarget(COMBAT_CHASE_SPEED, spec);
            } else {
                student.startReload(spec);
                mob.getNavigation().stop();
            }
            return;
        }

        if (!spec.infiniteAmmo && student.getAmmoInMag() <= spec.reloadStartAmmo) {
            if (dist < spec.panicRange) {
                tryMoveAwayFromTarget(COMBAT_CHASE_SPEED, spec);
            } else {
                student.startReload(spec);
                mob.getNavigation().stop();
            }
            return;
        }

        student.requestLookTarget(target, 50, 2);

        if (!flying) mob.getNavigation().stop();

        if (cooldown > 0) return;

        student.queueFire(target);
        cooldown = spec.cooldownTicks;
    }

    private void tryMoveTowardTarget(double speed, WeaponSpec spec) {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 to = target.position().subtract(mob.position());
        to = new Vec3(to.x, 0, to.z);
        if (to.lengthSqr() < 1.0e-6) return;

        Vec3 dir = to.normalize();
        double want = Math.max(spec.preferredMinRange + 0.5, Math.min(spec.preferredMaxRange, spec.range - 0.5));
        Vec3 desired = target.position().subtract(dir.scale(want));

        desired = clampToGuardAreaIfNeeded(desired);

        mob.getNavigation().moveTo(desired.x, desired.y, desired.z, speed);
    }

    private Vec3 clampToGuardAreaIfNeeded(Vec3 desired) {
        if (student.getAiMode() != StudentAiMode.SECURITY) return desired;

        BlockPos guard = student.getSecurityPos();
        if (guard == null) return desired;

        Vec3 center = new Vec3(guard.getX() + 0.5, guard.getY() + 0.5, guard.getZ() + 0.5);
        Vec3 v = desired.subtract(center);

        double r = GUARD_RADIUS;
        if (v.lengthSqr() > r * r) {
            Vec3 clamped = center.add(v.normalize().scale(r));
            return new Vec3(clamped.x, desired.y, clamped.z);
        }
        return desired;
    }

    private LivingEntity findTarget() {
        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());

        AABB box = mob.getBoundingBox().inflate(spec.range + 8.0);
        LivingEntity found = mob.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                        e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);

        if (found != null && student.getAiMode() == StudentAiMode.SECURITY) {
            BlockPos guard = student.getSecurityPos();
            if (guard != null) {
                double d2 = found.distanceToSqr(guard.getX() + 0.5, guard.getY() + 0.5, guard.getZ() + 0.5);
                double r = GUARD_RADIUS + 6.0;
                if (d2 > r * r) return null;
            }
        }

        return found;
    }

    private void tryMoveAwayFromTarget(double speed, WeaponSpec spec) {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 away = mob.position().subtract(target.position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0e-6) return;

        Vec3 dir = away.normalize();
        Vec3 desired = mob.position().add(dir.scale(6.0));
        desired = clampToGuardAreaIfNeeded(desired);

        Vec3 pos = DefaultRandomPos.getPosTowards(mob, 12, 7, desired, Math.PI / 2);
        if (pos == null) pos = desired;

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, speed);
    }

    private void tryTriggerSkill(WeaponSpec spec, double dist) {
        if (!(mob instanceof AbstractStudentEntity se)) return;
        if (!se.canStartSkill()) return;
        if (mob.tickCount % 200 != 0) return;
        se.startSkillNow();
    }
}