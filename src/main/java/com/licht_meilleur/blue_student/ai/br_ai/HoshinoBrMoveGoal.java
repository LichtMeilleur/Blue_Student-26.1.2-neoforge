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
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class HoshinoBrMoveGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private LivingEntity target;
    private int repathCooldown = 0;
    private int strafeSwitchCooldown = 0;
    private int holdTicks = 0;

    private MoveIntent currentIntent = MoveIntent.HOLD;
    private boolean strafeLeft = true;

    private static final int REPATH_INTERVAL = 8;
    private static final int STRAFE_SWITCH_MIN = 12;
    private static final int STRAFE_SWITCH_MAX = 24;

    private static final double SPEED_RETREAT = 1.35;
    private static final double SPEED_APPROACH = 1.25;
    private static final double SPEED_STRAFE = 1.10;

    private static final double PREFERRED_MIN = 4.0;
    private static final double PREFERRED_MAX = 8.5;

    private static final double EMERGENCY_RETREAT_DIST = 2.4;
    private static final double FORCE_APPROACH_DIST = 10.0;

    public HoshinoBrMoveGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public enum MoveIntent {
        RETREAT,
        APPROACH,
        STRAFE_LEFT,
        STRAFE_RIGHT,
        HOLD
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        if (!(student instanceof AbstractStudentEntity ase)) return false;
        if (ase.getForm() != StudentForm.BR) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
            if (target != null) {
                mob.setTarget(target);
            }
        }

        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        if (!(student instanceof AbstractStudentEntity ase)) return false;
        if (ase.getForm() != StudentForm.BR) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (target == null || !target.isAlive()) return false;

        WeaponSpec main = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        double keep = main.range + 10.0;
        return mob.distanceToSqr(target) <= keep * keep;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        strafeSwitchCooldown = 0;
        holdTicks = 0;
        currentIntent = MoveIntent.HOLD;
        strafeLeft = mob.getRandom().nextBoolean();
    }

    @Override
    public void stop() {
        target = null;
        currentIntent = MoveIntent.HOLD;
        holdTicks = 0;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;
        if (target == null || !target.isAlive()) return;

        if (repathCooldown > 0) repathCooldown--;
        if (strafeSwitchCooldown > 0) strafeSwitchCooldown--;
        if (holdTicks > 0) holdTicks--;

        final double dist = mob.distanceTo(target);
        final boolean canSee = mob.getSensing().hasLineOfSight(target);

        student.requestLookTarget(target, 80, 2);

        currentIntent = decideIntent(dist, canSee);

        switch (currentIntent) {
            case RETREAT -> {
                moveRetreat();
                requestMoveAnim(false);
            }
            case APPROACH -> {
                moveApproach();
                requestMoveAnim(false);
            }
            case STRAFE_LEFT -> {
                moveStrafe(true);
                requestMoveAnim(true);
            }
            case STRAFE_RIGHT -> {
                moveStrafe(false);
                requestMoveAnim(false);
            }
            case HOLD -> {
                mob.getNavigation().stop();
                // HOLD は移動アニメを握らない
            }
        }
    }

    private MoveIntent decideIntent(double dist, boolean canSee) {
        if (dist <= EMERGENCY_RETREAT_DIST) {
            holdTicks = 0;
            return MoveIntent.RETREAT;
        }

        if (!canSee) {
            if (dist < 6.0) {
                maybeFlipStrafe();
                holdTicks = 0;
                return strafeLeft ? MoveIntent.STRAFE_LEFT : MoveIntent.STRAFE_RIGHT;
            }
            holdTicks = 0;
            return MoveIntent.APPROACH;
        }

        if (dist >= FORCE_APPROACH_DIST) {
            holdTicks = 0;
            return MoveIntent.APPROACH;
        }

        if (dist < PREFERRED_MIN) {
            holdTicks = 0;
            return MoveIntent.RETREAT;
        }

        // 中距離は常時strafeしない。かなりの割合で HOLD を混ぜる
        if (dist >= PREFERRED_MIN && dist <= PREFERRED_MAX) {
            if (holdTicks > 0) {
                return MoveIntent.HOLD;
            }

            float r = mob.getRandom().nextFloat();

            // 55% HOLD, 45% STRAFE
            if (r < 0.55f) {
                holdTicks = 8 + mob.getRandom().nextInt(8);
                return MoveIntent.HOLD;
            }

            maybeFlipStrafe();
            return strafeLeft ? MoveIntent.STRAFE_LEFT : MoveIntent.STRAFE_RIGHT;
        }

        if (dist > PREFERRED_MAX) {
            // 少し遠めでも常に詰めず、たまに止まる
            if (mob.getRandom().nextFloat() < 0.25f) {
                holdTicks = 6 + mob.getRandom().nextInt(6);
                return MoveIntent.HOLD;
            }
            return MoveIntent.APPROACH;
        }

        return MoveIntent.HOLD;
    }

    private void maybeFlipStrafe() {
        if (strafeSwitchCooldown > 0) return;

        if (mob.getRandom().nextFloat() < 0.35f) {
            strafeLeft = !strafeLeft;
        }

        strafeSwitchCooldown = STRAFE_SWITCH_MIN
                + mob.getRandom().nextInt(STRAFE_SWITCH_MAX - STRAFE_SWITCH_MIN + 1);
    }

    private void requestMoveAnim(boolean left) {
        // 移動中だけ BR 移動アニメを短く握る
        student.requestBrAction(left ? StudentBrAction.LEFT_MOVE : StudentBrAction.RIGHT_MOVE, 3);

        if (mob instanceof AbstractStudentEntity ase) {
            ase.requestLookMoveDir(80, 3);
        }
    }

    private void moveRetreat() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 away = mob.position().subtract(target.position());
        away = new Vec3(away.x, 0, away.z);

        if (away.lengthSqr() < 1.0e-6) return;

        Vec3 dir = away.normalize();
        Vec3 desired = mob.position().add(dir.scale(6.0));

        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double sideOffset = (mob.getRandom().nextBoolean() ? 1.0 : -1.0) * 2.0;
        desired = desired.add(side.scale(sideOffset));

        Vec3 pos = DefaultRandomPos.getPosTowards(mob, 12, 7, desired, Math.PI / 2);
        if (pos == null) pos = desired;

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_RETREAT);
    }

    private void moveApproach() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 pos = target.position();
        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_APPROACH);
    }

    private void moveStrafe(boolean left) {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 to = target.position().subtract(mob.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);

        if (flat.lengthSqr() < 1.0e-6) return;

        Vec3 dir = flat.normalize();
        Vec3 side = left
                ? new Vec3(-dir.z, 0, dir.x)
                : new Vec3(dir.z, 0, -dir.x);

        double len = 2.25 + mob.getRandom().nextDouble() * 1.0;
        Vec3 desired = mob.position().add(side.normalize().scale(len));
        desired = desired.add(dir.scale(0.55));

        Vec3 pos = DefaultRandomPos.getPosTowards(mob, 10, 6, desired, Math.PI / 2);
        if (pos == null) pos = desired;

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_STRAFE);
    }

    private LivingEntity findNearestHostile() {
        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId(), StudentForm.BR, IStudentEntity.FireChannel.MAIN);
        AABB box = mob.getBoundingBox().inflate(spec.range + 10.0);

        return mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    public MoveIntent getCurrentIntent() {
        return currentIntent;
    }
}