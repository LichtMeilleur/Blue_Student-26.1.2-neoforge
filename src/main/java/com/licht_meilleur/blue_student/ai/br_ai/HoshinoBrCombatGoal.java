package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentBrAction;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class HoshinoBrCombatGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;
    private LivingEntity target;

    private static final double DETECT_EXTRA = 10.0;

    private static final double TACKLE_RANGE = 3.3;
    private static final double BASH_RANGE = 3.9;

    private static final double SHOTGUN_MIN = 4.0;
    private static final double SHOTGUN_MAX = 8.5;

    private static final double EVADE_DIST = 2.2;
    private static final double APPROACH_IF_OVER = 10.0;

    private static final double SPEED_CHASE = 1.35;
    private static final double SPEED_AIM = 1.15;

    private static final int REPATH_INTERVAL = 12;
    private int repathCooldown = 0;

    private StudentBrAction current = StudentBrAction.NONE;
    private int actionHoldTicks = 0;
    private int actionAge = 0;

    private int cdTackle = 0;
    private int cdBash = 0;
    private int cdDodge = 0;
    private int cdMain = 0;
    private int cdSub = 0;
    private int cdSide = 0;

    private int sideDashTicksLeft = 0;
    private Vec3 sideDashVel = Vec3.ZERO;

    private boolean meleeHitDone = false;

    private int actionLockTicks = 0;
    private int hitReactCooldown = 0;

    private int noSeeTicks = 0;
    private static final int DROP_TARGET_NOSEE_TICKS = 35;
    private static final int REACQUIRE_INTERVAL = 10;

    private static final int[] SUB_BURST_TICKS = new int[]{0, 4, 8};

    private double prevDist = 9999.0;

    public HoshinoBrCombatGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean isBr() {
        return (student instanceof AbstractStudentEntity ase) && ase.getForm() == StudentForm.BR;
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!isBr()) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        target = findTargetPreferVisibleAndPath();
        if (target != null) mob.setTarget(target);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!isBr()) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (target == null || !target.isAlive()) return false;

        WeaponSpec main = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        double keep = main.range + DETECT_EXTRA;
        return mob.distanceToSqr(target) <= keep * keep;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        clearCds();
        stopAction();
        mob.getNavigation().stop();

        sideDashTicksLeft = 0;
        sideDashVel = Vec3.ZERO;
        meleeHitDone = false;
        actionLockTicks = 0;
        hitReactCooldown = 0;

        noSeeTicks = 0;
        prevDist = 9999.0;
    }

    @Override
    public void stop() {
        target = null;
        mob.setTarget(null);
        mob.getNavigation().stop();
        stopAction();
        clearCds();
        noSeeTicks = 0;
        prevDist = 9999.0;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        tickCds();
        if (hitReactCooldown > 0) hitReactCooldown--;
        if (repathCooldown > 0) repathCooldown--;
        if (cdSide > 0) cdSide--;
        if (actionLockTicks > 0) actionLockTicks--;

        if (target == null || !target.isAlive()) {
            target = findTarget();
            mob.setTarget(target);
            mob.getNavigation().stop();
            stopAction();
            prevDist = 9999.0;
            return;
        }

        final double dist = mob.distanceTo(target);
        final boolean canSee = mob.getSensing().hasLineOfSight(target);

        aliceLikeLook(target);

        boolean crossedDodgeBand = (prevDist > 3.0 && dist <= 2.0);
        prevDist = dist;

        if (crossedDodgeBand
                && canSee
                && cdDodge <= 0
                && actionLockTicks <= 0
                && hitReactCooldown <= 0
                && current != StudentBrAction.DODGE_SHOT) {

            startAction(StudentBrAction.DODGE_SHOT);
            return;
        }

        final WeaponSpec mainSpec = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        final WeaponSpec subSpec = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.SUB_L);

        if (!canSee) noSeeTicks++;
        else noSeeTicks = 0;

        if (noSeeTicks >= DROP_TARGET_NOSEE_TICKS) {
            target = findTargetPreferVisibleAndPath();
            mob.setTarget(target);
            mob.getNavigation().stop();
            stopAction();
            noSeeTicks = 0;
            return;
        }

        if (mob.tickCount % REACQUIRE_INTERVAL == 0) {
            LivingEntity better = findTargetPreferVisibleAndPath();
            if (better != null && better != target) {
                target = better;
                mob.setTarget(target);
                mob.getNavigation().stop();
                stopAction();
            }
        }

        if (!mainSpec.infiniteAmmo && student.getAmmoInMag() <= 0) {
            if (dist < EVADE_DIST) {
                moveAwayFromTarget(SPEED_CHASE);
                stopAction();
                return;
            } else {
                if (!student.isReloading()) student.startReload(mainSpec);
                startAction(StudentBrAction.SUB_RELOAD_SHOT);
                return;
            }
        }

        if (!canSee) {
            if (dist < 6.0) {
                moveSideToFindLoS();
            } else {
                moveTowardTarget(SPEED_AIM);
            }
            stopAction();
            return;
        }

        if (dist >= APPROACH_IF_OVER) {
            moveTowardTarget(SPEED_CHASE);
            stopAction();
            return;
        }

        if (student.isReloading()) {
            student.tickReload(mainSpec);
        }

        if (actionHoldTicks > 0 && current != StudentBrAction.NONE) {
            student.requestBrAction(current, actionHoldTicks);
            runCurrent(current, dist, canSee, mainSpec, subSpec, serverLevel);

            actionHoldTicks--;
            actionAge++;
            return;
        }

        if (actionLockTicks > 0) {
            if (current != StudentBrAction.NONE) {
                student.requestBrAction(current, 2);
            }
            mob.getNavigation().stop();
            return;
        }

        StudentBrAction next = selectByDistance(dist, mainSpec);

        if (next == StudentBrAction.NONE) {
            stopAction();
            return;
        }

        startAction(next);
    }

    private void aliceLikeLook(LivingEntity target) {
        student.requestLookTarget(target, 80, 2);
    }

    private StudentBrAction selectByDistance(double dist, WeaponSpec mainSpec) {
        if (dist > SHOTGUN_MAX && dist <= 12.0 && cdTackle <= 0) return StudentBrAction.GUARD_TACKLE;

        if (dist <= TACKLE_RANGE && cdTackle <= 0) return StudentBrAction.GUARD_TACKLE;
        if (dist <= BASH_RANGE && cdBash <= 0) return StudentBrAction.GUARD_BASH;

        if (dist <= EVADE_DIST && cdDodge <= 0) return StudentBrAction.DODGE_SHOT;

        if (dist >= SHOTGUN_MIN && dist <= SHOTGUN_MAX) {
            if (cdSide <= 0 && cdSub <= 0 && mob.getRandom().nextFloat() < 0.22f) {
                return (mob.tickCount % 2 == 0) ? StudentBrAction.LEFT_SIDE_SUB_SHOT : StudentBrAction.RIGHT_SIDE_SUB_SHOT;
            }
        }

        if (student.isReloading()) return StudentBrAction.SUB_RELOAD_SHOT;

        if (dist >= SHOTGUN_MIN && dist <= SHOTGUN_MAX) {
            if (cdMain <= 0) return StudentBrAction.MAIN_SHOT;
            if (cdSub <= 0) return StudentBrAction.SUB_SHOT;
            return StudentBrAction.NONE;
        }

        if (cdSub <= 0) return StudentBrAction.SUB_SHOT;

        return StudentBrAction.NONE;
    }

    private void startAction(StudentBrAction a) {
        current = a;
        actionAge = 0;
        meleeHitDone = false;

        actionHoldTicks = switch (a) {
            case GUARD_TACKLE, GUARD_BASH -> 10;
            case DODGE_SHOT -> 17;
            case MAIN_SHOT -> 6;
            case SUB_SHOT, SUB_RELOAD_SHOT -> 16;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 10;
            default -> 6;
        };

        actionLockTicks = switch (a) {
            case DODGE_SHOT -> 17;
            case GUARD_TACKLE, GUARD_BASH -> 10;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 8;
            case SUB_SHOT, SUB_RELOAD_SHOT -> 6;
            default -> 0;
        };

        switch (a) {
            case GUARD_TACKLE -> cdTackle = 35;
            case GUARD_BASH -> cdBash = 25;
            case DODGE_SHOT -> cdDodge = 14;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> cdSide = 24;
            default -> {
            }
        }

        student.requestBrAction(a, actionHoldTicks);
    }

    private void stopAction() {
        current = StudentBrAction.NONE;
        actionHoldTicks = 0;
        actionAge = 0;
        meleeHitDone = false;
    }

    private void runCurrent(StudentBrAction a, double dist, boolean canSee, WeaponSpec mainSpec, WeaponSpec subSpec, ServerLevel serverLevel) {
        if (target != null) student.requestLookTarget(target, 80, 2);

        if (!canSee) {
            if (dist < 6.0) moveSideToFindLoS();
            else moveTowardTarget(SPEED_AIM);
            return;
        }

        switch (a) {
            case GUARD_TACKLE -> {
                mob.getNavigation().stop();

                if (actionAge <= 4) {
                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        double dashSpeed = 0.65;
                        mob.setDeltaMovement(dir.x * dashSpeed, mob.getDeltaMovement().y, dir.z * dashSpeed);

                        if (mob instanceof AbstractStudentEntity ase) {
                            ase.requestLookMoveDir(80, 2);
                        }
                    }
                }

                if (!meleeHitDone && actionAge >= 2 && actionAge <= 8 && isInMeleeRange(2.6)) {
                    meleeHitDone = true;

                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(mob), 6.0f);

                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        target.knockback(1.25f, dir.x, dir.z);
                    }
                }
            }

            case GUARD_BASH -> {
                mob.getNavigation().stop();

                if (!meleeHitDone && actionAge >= 1 && actionAge <= 6 && isInMeleeRange(2.8)) {
                    meleeHitDone = true;

                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(mob), 4.0f);

                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        target.knockback(1.6f, dir.x, dir.z);
                    }
                }
            }

            case DODGE_SHOT -> {
                mob.getNavigation().stop();

                if (actionAge <= 6) {
                    Vec3 away = mob.position().subtract(target.position());
                    Vec3 flat = new Vec3(away.x, 0, away.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        double dodgeSpeed = 0.95;
                        mob.setDeltaMovement(dir.x * dodgeSpeed, mob.getDeltaMovement().y, dir.z * dodgeSpeed);

                        if (mob instanceof AbstractStudentEntity ase) {
                            ase.requestLookMoveDir(80, 2);
                        }
                    }
                }

                queueSingleShotAtTick(false, mainSpec, 1);
            }

            case MAIN_SHOT -> {
                mob.getNavigation().stop();
                queueSingleShotAtTick(false, mainSpec, 0);
            }

            case RIGHT_SIDE_SUB_SHOT -> {
                if (actionAge == 0) startSideStepDash(false, 1.05, 2);
                tickSideDash();
                queueSingleShotAtTick(true, subSpec, 1);
            }

            case LEFT_SIDE_SUB_SHOT -> {
                if (actionAge == 0) startSideStepDash(true, 1.05, 2);
                tickSideDash();
                queueSingleShotAtTick(true, subSpec, 1);
            }

            case SUB_SHOT, SUB_RELOAD_SHOT -> {
                mob.getNavigation().stop();

                for (int t : SUB_BURST_TICKS) {
                    queueBurstShotAtExactTick(true, subSpec, t);
                }
            }

            default -> mob.getNavigation().stop();
        }
    }

    private void queueSingleShotAtTick(boolean isSub, WeaponSpec spec, int triggerTick) {
        if (target == null || !target.isAlive()) return;
        if (actionAge < triggerTick) return;

        if (!isSub) {
            if (student.hasQueuedFire(IStudentEntity.FireChannel.MAIN)) return;
            student.queueFire(target, IStudentEntity.FireChannel.MAIN);
            cdMain = Math.max(cdMain, spec.cooldownTicks);
            return;
        }

        IStudentEntity.FireChannel ch = IStudentEntity.FireChannel.SUB_L;

        if (student.hasQueuedFire(ch)) return;
        student.queueFire(target, ch);
        cdSub = Math.max(cdSub, spec.cooldownTicks);
    }

    private void queueBurstShotAtExactTick(boolean isSub, WeaponSpec spec, int exactTick) {
        if (target == null || !target.isAlive()) return;
        if (actionAge != exactTick) return;

        IStudentEntity.FireChannel ch = isSub ? IStudentEntity.FireChannel.SUB_L : IStudentEntity.FireChannel.MAIN;
        if (student.hasQueuedFire(ch)) return;

        student.queueFire(target, ch);

        if (isSub) cdSub = Math.max(cdSub, spec.cooldownTicks);
        else cdMain = Math.max(cdMain, spec.cooldownTicks);
    }

    private void moveTowardTarget(double speed) {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;
        Vec3 pos = target.position();
        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, speed);
    }

    private void moveAwayFromTarget(double speed) {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 away = mob.position().subtract(target.position());
        away = new Vec3(away.x, 0, away.z);
        if (away.lengthSqr() < 1.0e-6) return;

        Vec3 desired = mob.position().add(away.normalize().scale(6.0));
        Vec3 fuzzy = DefaultRandomPos.getPosTowards(mob, 12, 7, desired, Math.PI / 2);
        if (fuzzy == null) fuzzy = desired;

        mob.getNavigation().moveTo(fuzzy.x, fuzzy.y, fuzzy.z, speed);
    }

    private void moveSideToFindLoS() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        if (target == null) return;

        Vec3 to = target.position().subtract(mob.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0e-6) return;

        Vec3 dir = flat.normalize();
        boolean left = mob.getRandom().nextBoolean();
        Vec3 side = left ? new Vec3(-dir.z, 0, dir.x) : new Vec3(dir.z, 0, -dir.x);

        double len = 2.5 + mob.getRandom().nextDouble() * 1.5;
        Vec3 desired = mob.position().add(side.normalize().scale(len));

        Vec3 fuzzy = DefaultRandomPos.getPosTowards(mob, 10, 6, desired, Math.PI / 2);
        if (fuzzy == null) fuzzy = desired;

        mob.getNavigation().moveTo(fuzzy.x, fuzzy.y, fuzzy.z, SPEED_AIM);
    }

    private void startSideStepDash(boolean left, double horizSpeed, int dashTicks) {
        if (target == null) return;

        Vec3 to = target.position().subtract(mob.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0e-6) return;

        Vec3 dir = flat.normalize();
        Vec3 side = left
                ? new Vec3(-dir.z, 0, dir.x)
                : new Vec3(dir.z, 0, -dir.x);

        mob.getNavigation().stop();

        sideDashVel = side.normalize().scale(horizSpeed);
        sideDashTicksLeft = Math.max(1, dashTicks);

        if (mob instanceof AbstractStudentEntity ase) {
            ase.requestLookMoveDir(80, dashTicks);
        }
    }

    private void tickSideDash() {
        if (sideDashTicksLeft <= 0) return;

        mob.setDeltaMovement(sideDashVel.x, mob.getDeltaMovement().y, sideDashVel.z);
        sideDashTicksLeft--;
    }

    private void clearCds() {
        cdTackle = cdBash = cdDodge = cdMain = cdSub = cdSide = 0;
    }

    private void tickCds() {
        if (cdTackle > 0) cdTackle--;
        if (cdBash > 0) cdBash--;
        if (cdDodge > 0) cdDodge--;
        if (cdMain > 0) cdMain--;
        if (cdSub > 0) cdSub--;
        if (cdSide > 0) cdSide--;
    }

    private LivingEntity findTargetPreferVisibleAndPath() {
        WeaponSpec main = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        AABB box = mob.getBoundingBox().inflate(main.range + DETECT_EXTRA);

        List<LivingEntity> list = mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster);

        if (list.isEmpty()) return null;

        LivingEntity best = list.stream()
                .filter(e -> mob.getSensing().hasLineOfSight(e))
                .filter(this::hasPathTo)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (best != null) return best;

        best = list.stream()
                .filter(e -> mob.getSensing().hasLineOfSight(e))
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (best != null) return best;

        best = list.stream()
                .filter(this::hasPathTo)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (best != null) return best;

        return list.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private boolean hasPathTo(LivingEntity e) {
        if (e == null) return false;
        Path p = mob.getNavigation().createPath(e, 0);
        return p != null;
    }

    private boolean isInMeleeRange(double range) {
        if (target == null) return false;
        return mob.distanceToSqr(target) <= range * range;
    }

    private LivingEntity findTarget() {
        WeaponSpec main = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        AABB box = mob.getBoundingBox().inflate(main.range + DETECT_EXTRA);

        return mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}