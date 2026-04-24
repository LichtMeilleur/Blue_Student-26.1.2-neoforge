package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.ai.prediction.EnemyIntentAnalyzer;
import com.licht_meilleur.blue_student.ai.prediction.EnemyIntentRead;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.HoshinoEntity;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class HoshinoBrCombatGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;
    private LivingEntity target;

    private static final double DETECT_EXTRA = 10.0;

    private static final double TACKLE_RANGE = 3.6;
    private static final double BASH_RANGE = 3.9;

    private static final double SHOTGUN_MIN = 3.6;
    private static final double SHOTGUN_MAX = 8.8;

    private static final double EVADE_DIST = 2.4;
    private static final double APPROACH_IF_OVER = 11.0;

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

    private static final int[] SUB_BURST_TICKS = new int[]{1, 3, 6};

    private double prevDist = 9999.0;

    private final EnumMap<StudentBrAction, Double> desire = new EnumMap<>(StudentBrAction.class);

    public HoshinoBrCombatGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.noneOf(Flag.class));

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

        LivingEntity cur = mob.getTarget();
        if (isValidCombatTarget(cur)) {
            target = cur;
            return true;
        }

        target = findTargetLoose();
        if (target != null) {
            mob.setTarget(target);
            return true;
        }

        Player owner = resolveOwner();
        if (owner != null && owner.level() == mob.level()) {
            double dOwner = mob.distanceToSqr(owner);
            if (dOwner > 22.0 * 22.0) return false;
        }

        return false;
    }

    @Nullable
    private Player resolveOwner() {
        if (student instanceof AbstractStudentEntity se) {
            Player p = se.getOwnerPlayer();
            if (p != null) return p;
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!isBr()) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (!isValidCombatTarget(target)) {
            return false;
        }

        WeaponSpec main = WeaponSpecs.forStudent(
                student.getStudentId(),
                StudentForm.BR,
                IStudentEntity.FireChannel.MAIN
        );

        double keep = main.range + DETECT_EXTRA + 6.0;
        return mob.distanceToSqr(target) <= keep * keep;
    }

    private boolean isValidCombatTarget(LivingEntity e) {
        if (e == null) return false;
        if (!e.isAlive()) return false;
        if (!(e instanceof Monster)) return false;

        WeaponSpec main = WeaponSpecs.forStudent(
                student.getStudentId(),
                StudentForm.BR,
                IStudentEntity.FireChannel.MAIN
        );
        double keep = main.range + DETECT_EXTRA + 6.0;
        return mob.distanceToSqr(e) <= keep * keep;
    }

    private LivingEntity findTargetLoose() {
        WeaponSpec main = WeaponSpecs.forStudent(
                student.getStudentId(),
                StudentForm.BR,
                IStudentEntity.FireChannel.MAIN
        );
        AABB box = mob.getBoundingBox().inflate(main.range + DETECT_EXTRA + 6.0);

        List<LivingEntity> list = mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster);

        if (list.isEmpty()) return null;

        LivingEntity best = list.stream()
                .filter(mob::hasLineOfSight)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (best != null) return best;

        return list.stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    @Override
    public void start() {
        clearCds();
        stopActionHard();
        mob.getNavigation().stop();

        sideDashTicksLeft = 0;
        sideDashVel = Vec3.ZERO;
        meleeHitDone = false;
        actionLockTicks = 0;
        hitReactCooldown = 0;

        noSeeTicks = 0;
        prevDist = 9999.0;
        for (StudentBrAction a : StudentBrAction.values()) {
            desire.put(a, 1.0);
        }
    }

    @Override
    public void stop() {
        if (target != null) {
            EnemyIntentAnalyzer.clear(target);
        }

        target = null;
        mob.setTarget(null);
        mob.getNavigation().stop();
        stopActionHard();
        clearCds();
        noSeeTicks = 0;
        prevDist = 9999.0;
    }

    @Override
    public void tick() {
        tickDesire();
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        if (!mob.onGround() && mob.getDeltaMovement().y < -0.08) {
            stopActionSoft();
            return;
        }

        tickCds();
        if (hitReactCooldown > 0) hitReactCooldown--;
        if (actionLockTicks > 0) actionLockTicks--;

        if (target == null || !target.isAlive()) {
            if (target != null) EnemyIntentAnalyzer.clear(target);

            target = findTarget();
            mob.setTarget(target);
            mob.getNavigation().stop();
            stopActionHard();
            prevDist = 9999.0;
            return;
        }

        final double dist = mob.distanceTo(target);
        final boolean canSee = mob.hasLineOfSight(target);

        student.requestLookTarget(target, 180, 4);

        boolean crossedDodgeBand = (prevDist > 3.0 && dist <= 2.0);
        prevDist = dist;

        final WeaponSpec mainSpec = WeaponSpecs.forStudent(
                student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN
        );
        final WeaponSpec subSpec = WeaponSpecs.forStudent(
                student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.SUB_L
        );

        if (!canSee) noSeeTicks++;
        else noSeeTicks = 0;

        if (noSeeTicks >= DROP_TARGET_NOSEE_TICKS) {
            EnemyIntentAnalyzer.clear(target);
            target = findTargetPreferVisibleAndPath();
            mob.setTarget(target);
            mob.getNavigation().stop();
            stopActionHard();
            noSeeTicks = 0;
            return;
        }

        if (mob.tickCount % REACQUIRE_INTERVAL == 0) {
            LivingEntity better = findTargetPreferVisibleAndPath();
            if (better != null && better != target) {
                EnemyIntentAnalyzer.clear(target);
                target = better;
                mob.setTarget(target);
                mob.getNavigation().stop();
                stopActionHard();
            }
        }

        if (student.isReloading()) {
            student.tickReload(mainSpec);
        }

        if (crossedDodgeBand
                && canSee
                && cdDodge <= 0
                && actionLockTicks <= 0
                && hitReactCooldown <= 0
                && current != StudentBrAction.DODGE_SHOT) {
            startAction(StudentBrAction.DODGE_SHOT);
            return;
        }

        if (!canSee) {
            stopActionHard();
            requestMoveActionFromNavigation();
            return;
        }

        if (dist >= APPROACH_IF_OVER) {
            stopActionHard();
            requestMoveActionFromNavigation();
            return;
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
            } else {
                requestMoveActionFromNavigation();
            }
            return;
        }

        if (!mainSpec.infiniteAmmo && student.getAmmoInMag() <= 0) {
            if (!student.isReloading()) {
                student.startReload(mainSpec);
            }

            if (current != StudentBrAction.SUB_RELOAD_SHOT || actionHoldTicks <= 0) {
                startAction(StudentBrAction.SUB_RELOAD_SHOT);
            }
            return;
        }

        StudentBrAction next = selectActionSmart(dist, canSee);

        if (next == StudentBrAction.NONE) {
            if (canSee && dist >= SHOTGUN_MIN && dist <= SHOTGUN_MAX) {
                if (cdMain <= 0) {
                    startAction(StudentBrAction.MAIN_SHOT);
                    return;
                }
                if (cdSub <= 0) {
                    startAction(StudentBrAction.SUB_SHOT);
                    return;
                }
            }

            stopActionSoft();
            requestMoveActionFromNavigation();
            return;
        }

        startAction(next);
    }

    private enum CombatBand {
        CLOSE,
        MID,
        FAR
    }

    private CombatBand getBand(double dist) {
        if (dist <= BASH_RANGE) {
            return CombatBand.CLOSE;
        }
        if (dist <= SHOTGUN_MAX) {
            return CombatBand.MID;
        }
        return CombatBand.FAR;
    }

    private double repeatPenalty(StudentBrAction action) {
        if (action == current) {
            return -8.0;
        }
        return 0.0;
    }

    private StudentBrAction selectActionSmart(double dist, boolean canSee) {
        EnemyIntentRead read = EnemyIntentAnalyzer.analyze(mob, target);
        CombatBand band = getBand(dist);

        boolean danger = dist <= EVADE_DIST && canSee;
        if (danger && cdDodge <= 0 && read.meleeThreat >= 0.45) {
            return StudentBrAction.DODGE_SHOT;
        }

        if (!canSee) {
            return StudentBrAction.NONE;
        }

        if (student.getAmmoInMag() <= 0) {
            return StudentBrAction.SUB_RELOAD_SHOT;
        }

        Map<StudentBrAction, Double> score = new EnumMap<>(StudentBrAction.class);

        switch (band) {
            case CLOSE -> {
                double sDodge = 0.0;
                sDodge += 16.0;
                if (danger) sDodge += 18.0;
                if (read.closingIn) sDodge += 10.0;
                if (read.meleeThreat > 0.55) sDodge += 12.0;
                if (read.chargeThreat > 0.60) sDodge += 8.0;
                sDodge += repeatPenalty(StudentBrAction.DODGE_SHOT);
                if (cdDodge > 0) sDodge = -9999.0;
                score.put(StudentBrAction.DODGE_SHOT, sDodge);

                double sBash = 0.0;
                sBash += 26.0;
                if (dist <= BASH_RANGE) sBash += 12.0;
                if (read.opening > 0.45) sBash += 14.0;
                if (read.hurtTime > 0) sBash += 10.0;
                if (!read.closingIn) sBash += 6.0;
                sBash += repeatPenalty(StudentBrAction.GUARD_BASH);
                sBash += desire.get(StudentBrAction.GUARD_BASH) * 18.0;
                if (cdBash > 0) sBash = -9999.0;
                score.put(StudentBrAction.GUARD_BASH, sBash);

                double sGuardShot = 0.0;
                sGuardShot += 24.0;
                if (read.targetFacingSelf) sGuardShot += 10.0;
                if (read.meleeThreat > 0.35) sGuardShot += 10.0;
                if (read.rangedThreat > 0.30) sGuardShot += 5.0;
                if (read.opening < 0.45) sGuardShot += 5.0;
                sGuardShot += desire.getOrDefault(StudentBrAction.GUARD_SHOT, 1.0) * 14.0;
                sGuardShot += repeatPenalty(StudentBrAction.GUARD_SHOT);
                if (cdMain > 0) sGuardShot = -9999.0;
                score.put(StudentBrAction.GUARD_SHOT, sGuardShot);
            }

            case MID -> {
                double sMain = 0.0;
                sMain += 20.0;
                if (dist >= 5.0 && dist <= 7.5) sMain += 12.0;
                if (read.opening > 0.45) sMain += 10.0;
                if (read.rangedThreat > 0.45 && read.holdingStill) sMain += 8.0;
                sMain += repeatPenalty(StudentBrAction.MAIN_SHOT);
                sMain += desire.getOrDefault(StudentBrAction.MAIN_SHOT, 1.0) * 8.0;
                if (cdMain > 0) sMain = -9999.0;
                score.put(StudentBrAction.MAIN_SHOT, sMain);

                double sSub = 0.0;
                sSub += 4.0;
                if (dist >= 6.0 && dist <= 8.8) sSub += 4.0;
                if (read.holdingStill) sSub += 2.0;
                if (read.rangedThreat > 0.40) sSub += 2.0;
                sSub += repeatPenalty(StudentBrAction.SUB_SHOT);
                sSub += desire.getOrDefault(StudentBrAction.SUB_SHOT, 1.0) * 4.0;
                if (cdSub > 0) sSub = -9999.0;
                score.put(StudentBrAction.SUB_SHOT, sSub);

                double sSideL = 0.0;
                double sSideR = 0.0;
                boolean left = shouldUseLeft();

                if (left) {
                    sSideL += 8.0;
                } else {
                    sSideR += 8.0;
                }

                sSideL += 24.0;
                sSideR += 24.0;

                if (dist >= 5.0 && dist <= 8.0) {
                    sSideL += 12.0;
                    sSideR += 12.0;
                }
                if (read.holdingStill) {
                    sSideL += 12.0;
                    sSideR += 12.0;
                }
                if (!read.targetFacingSelf) {
                    sSideL += 10.0;
                    sSideR += 10.0;
                }
                if (read.opening > 0.40) {
                    sSideL += 10.0;
                    sSideR += 10.0;
                }

                sSideL += repeatPenalty(StudentBrAction.LEFT_SIDE_SUB_SHOT);
                sSideR += repeatPenalty(StudentBrAction.RIGHT_SIDE_SUB_SHOT);
                sSideL += desire.get(StudentBrAction.LEFT_SIDE_SUB_SHOT) * 16.0;
                sSideR += desire.get(StudentBrAction.RIGHT_SIDE_SUB_SHOT) * 16.0;

                if (cdSide > 0 || cdSub > 0) {
                    sSideL = -9999.0;
                    sSideR = -9999.0;
                }

                score.put(StudentBrAction.LEFT_SIDE_SUB_SHOT, sSideL);
                score.put(StudentBrAction.RIGHT_SIDE_SUB_SHOT, sSideR);
            }

            case FAR -> {
                double sTackle = 0.0;
                sTackle += 18.0;
                if (dist >= 9.0) sTackle += 12.0;
                if (read.backingOff) sTackle += 10.0;
                if (read.opening > 0.50) sTackle += 8.0;
                sTackle += repeatPenalty(StudentBrAction.GUARD_TACKLE);
                if (cdTackle > 0) sTackle = -9999.0;
                score.put(StudentBrAction.GUARD_TACKLE, sTackle);

                double sSub = 0.0;
                sSub += 2.0;
                if (dist >= 8.5) sSub += 2.0;
                if (read.holdingStill) sSub += 1.0;
                if (read.rangedThreat > 0.35) sSub += 1.0;
                sSub += repeatPenalty(StudentBrAction.SUB_SHOT);
                if (cdSub > 0) sSub = -9999.0;
                score.put(StudentBrAction.SUB_SHOT, sSub);
            }
        }

        return pickBest(score);
    }



    private StudentBrAction pickBest(Map<StudentBrAction, Double> score) {
        StudentBrAction best = StudentBrAction.NONE;
        double bestScore = 0.0;

        for (var e : score.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                best = e.getKey();
            }
        }

        return best;
    }

    private boolean shouldUseLeft() {
        Vec3 to = target.position().subtract(mob.position());
        return to.x * mob.getZ() - to.z * mob.getX() > 0;
    }

    private void tickDesire() {
        for (StudentBrAction a : StudentBrAction.values()) {
            double cur = desire.getOrDefault(a, 1.0);
            double max = 1.0;

            double regen = switch (a) {
                case GUARD_BASH -> 0.035;
                case GUARD_SHOT -> 0.030;
                case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 0.028;
                case GUARD_TACKLE -> 0.025;
                case DODGE_SHOT -> 0.025;
                case MAIN_SHOT -> 0.040;
                case SUB_SHOT -> 0.015;
                case SUB_RELOAD_SHOT -> 0.020;
                default -> 0.050;
            };

            desire.put(a, Math.min(max, cur + regen));
        }
    }

    private void consumeDesire(StudentBrAction a) {
        double next = switch (a) {
            case GUARD_BASH -> 0.0;
            case GUARD_SHOT -> 0.0;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 0.0;
            case GUARD_TACKLE -> 0.0;
            case DODGE_SHOT -> 0.0;
            case MAIN_SHOT -> 0.25;
            case SUB_SHOT -> 0.10;
            case SUB_RELOAD_SHOT -> 0.15;
            default -> 0.5;
        };
        desire.put(a, next);
    }

    private void startAction(StudentBrAction a) {
        consumeDesire(a);
        //logActionStart(a);

        current = a;
        actionAge = 0;
        meleeHitDone = false;

        setBrGuardStateForAction(a);

        actionHoldTicks = switch (a) {
            case GUARD_TACKLE -> 12;
            case GUARD_BASH -> 8;
            case GUARD_SHOT -> 10;
            case DODGE_SHOT -> 12;
            case MAIN_SHOT -> 6;
            case SUB_SHOT -> 10;
            case SUB_RELOAD_SHOT -> 12;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 10;
            default -> 5;
        };

        actionLockTicks = switch (a) {
            case DODGE_SHOT -> 10;
            case GUARD_TACKLE -> 10;
            case GUARD_BASH -> 7;
            case GUARD_SHOT -> 8;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> 8;
            case SUB_SHOT, SUB_RELOAD_SHOT -> 5;
            case MAIN_SHOT -> 4;
            default -> 0;
        };

        switch (a) {
            case GUARD_TACKLE -> cdTackle = 26;
            case GUARD_BASH -> cdBash = 18;
            case GUARD_SHOT -> cdMain = 10;
            case DODGE_SHOT -> cdDodge = 10;
            case LEFT_SIDE_SUB_SHOT, RIGHT_SIDE_SUB_SHOT -> cdSide = 16;
            default -> {
            }
        }

        student.requestBrAction(a, actionHoldTicks);
    }

    /*
    private void logActionStart(StudentBrAction a) {
        System.out.println("[BR-ACTION] " + a);
    }
    */


    private void stopActionSoft() {
        current = StudentBrAction.NONE;
        actionHoldTicks = 0;
        actionAge = 0;
        meleeHitDone = false;
        actionLockTicks = 0;
        sideDashTicksLeft = 0;
        sideDashVel = Vec3.ZERO;
        clearBrGuardState();
    }

    private void stopActionHard() {
        current = StudentBrAction.NONE;
        actionHoldTicks = 0;
        actionAge = 0;
        meleeHitDone = false;
        actionLockTicks = 0;
        sideDashTicksLeft = 0;
        sideDashVel = Vec3.ZERO;
        clearBrGuardState();
    }

    private void requestMoveActionFromNavigation() {
        // 通常移動ではBRアクションを送らない
    }

    private void runCurrent(StudentBrAction a, double dist, boolean canSee,
                            WeaponSpec mainSpec, WeaponSpec subSpec, ServerLevel serverLevel) {

        if (target != null) {
            student.requestLookTarget(target, 180, 5);

            if (mob instanceof AbstractStudentEntity ase) {
                ase.requestLookTarget(target, 180, 5);
            }
        }

        if (!canSee) {
            requestMoveActionFromNavigation();
            return;
        }

        switch (a) {
            case GUARD_TACKLE -> {
                mob.getNavigation().stop();

                if (actionAge <= 5) {
                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        double dashSpeed = 1.22;
                        mob.setDeltaMovement(dir.x * dashSpeed, mob.getDeltaMovement().y, dir.z * dashSpeed);
                    }
                }

                if (!meleeHitDone && actionAge >= 2 && actionAge <= 7 && isInMeleeRange(3.0)) {
                    meleeHitDone = true;
                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(mob), 6.0f);

                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        Vec3 old = target.getDeltaMovement();
                        Vec3 launch = new Vec3(dir.x * 1.10, 0.28, dir.z * 1.10);

                        target.setDeltaMovement(
                                old.x * 0.25 + launch.x,
                                Math.max(old.y, launch.y),
                                old.z * 0.25 + launch.z
                        );
                        target.hurtMarked = true;
                    }
                }
            }

            case GUARD_BASH -> {
                mob.getNavigation().stop();

                if (!meleeHitDone && actionAge >= 1 && actionAge <= 5 && isInMeleeRange(2.9)) {
                    meleeHitDone = true;
                    target.hurtServer(serverLevel, serverLevel.damageSources().mobAttack(mob), 4.0f);

                    Vec3 to = target.position().subtract(mob.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        Vec3 old = target.getDeltaMovement();
                        Vec3 launch = new Vec3(dir.x * 1.05, 0.32, dir.z * 1.05);

                        target.setDeltaMovement(
                                old.x * 0.25 + launch.x,
                                Math.max(old.y, launch.y),
                                old.z * 0.25 + launch.z
                        );
                        target.hurtMarked = true;
                    }
                }
            }

            case DODGE_SHOT -> {
                mob.getNavigation().stop();

                if (actionAge <= 4) {
                    Vec3 away = mob.position().subtract(target.position());
                    Vec3 flat = new Vec3(away.x, 0, away.z);
                    if (flat.lengthSqr() > 1.0e-6) {
                        Vec3 dir = flat.normalize();
                        double dodgeSpeed = 1.15;
                        mob.setDeltaMovement(dir.x * dodgeSpeed, mob.getDeltaMovement().y, dir.z * dodgeSpeed);
                    }
                }

                queueSingleShotAtTick(false, mainSpec, 1);
            }

            case MAIN_SHOT -> {
                mob.getNavigation().stop();
                student.requestLookTarget(target, 180, 5);
                queueSingleShotAtTick(false, mainSpec, 0);
            }

            case RIGHT_SIDE_SUB_SHOT -> {
                if (actionAge == 0) startSideStepDash(false, 1.4, 3);
                tickSideDash();
                queueSingleShotAtTick(true, subSpec, 2);
            }

            case LEFT_SIDE_SUB_SHOT -> {
                if (actionAge == 0) startSideStepDash(true, 1.4, 3);
                tickSideDash();
                queueSingleShotAtTick(true, subSpec, 2);
            }

            case SUB_SHOT, SUB_RELOAD_SHOT -> {
                mob.getNavigation().stop();
                for (int t : SUB_BURST_TICKS) {
                    queueBurstShotAtExactTick(true, subSpec, t);
                }
            }

            case GUARD_SHOT -> {
                mob.getNavigation().stop();
                student.requestLookTarget(target, 180, 5);
                queueSingleShotAtTick(false, mainSpec, 1);
            }

            default -> requestMoveActionFromNavigation();
        }
    }

    private void queueSingleShotAtTick(boolean isSub, WeaponSpec spec, int triggerTick) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (actionAge < triggerTick) {
            return;
        }

        if (!isSub) {
            if (student.hasQueuedFire(IStudentEntity.FireChannel.MAIN)) {
                return;
            }
            student.queueFire(target, IStudentEntity.FireChannel.MAIN);
            cdMain = Math.max(cdMain, spec.cooldownTicks);
            return;
        }

        IStudentEntity.FireChannel ch = IStudentEntity.FireChannel.SUB_L;
        if (student.hasQueuedFire(ch)) {
            return;
        }
        student.queueFire(target, ch);
        cdSub = Math.max(cdSub, spec.cooldownTicks);
    }

    private void queueBurstShotAtExactTick(boolean isSub, WeaponSpec spec, int exactTick) {
        if (target == null || !target.isAlive()) {
            return;
        }
        if (actionAge != exactTick) {
            return;
        }

        IStudentEntity.FireChannel ch = isSub ? IStudentEntity.FireChannel.SUB_L : IStudentEntity.FireChannel.MAIN;
        if (student.hasQueuedFire(ch)) return;

        student.queueFire(target, ch);

        if (isSub) cdSub = Math.max(cdSub, spec.cooldownTicks);
        else cdMain = Math.max(cdMain, spec.cooldownTicks);
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
                .filter(mob::hasLineOfSight)
                .filter(this::hasPathTo)
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
        if (best != null) return best;

        best = list.stream()
                .filter(mob::hasLineOfSight)
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

    private void setBrGuardStateForAction(StudentBrAction action) {
        if (mob instanceof HoshinoEntity hoshino) {
            boolean on = action == StudentBrAction.GUARD_TACKLE
                    || action == StudentBrAction.GUARD_SHOT;
            hoshino.setBrGuarding(on);
        }
    }

    private void clearBrGuardState() {
        if (mob instanceof HoshinoEntity hoshino) {
            hoshino.setBrGuarding(false);
        }
    }
}