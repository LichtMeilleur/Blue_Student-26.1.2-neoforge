package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
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

    private MoveIntent currentIntent = MoveIntent.HOLD;
    private boolean strafeLeft = true;

    private static final int REPATH_INTERVAL = 8;
    private static final int STRAFE_SWITCH_MIN = 12;
    private static final int STRAFE_SWITCH_MAX = 24;

    private static final double SPEED_RETREAT = 1.35;
    private static final double SPEED_APPROACH = 1.25;
    private static final double SPEED_STRAFE = 1.10;
    private static final double SPEED_LOS = 1.15;

    // Hoshino BR の基準距離
    private static final double PREFERRED_MIN = 4.0;
    private static final double PREFERRED_MAX = 8.5;

    // 危険距離
    private static final double EMERGENCY_RETREAT_DIST = 2.4;

    // 遠すぎると詰める
    private static final double FORCE_APPROACH_DIST = 10.0;

    public HoshinoBrMoveGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private enum MoveIntent {
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
        currentIntent = MoveIntent.HOLD;
        strafeLeft = mob.getRandom().nextBoolean();
    }

    @Override
    public void stop() {
        target = null;
        currentIntent = MoveIntent.HOLD;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;
        if (target == null || !target.isAlive()) return;

        if (repathCooldown > 0) repathCooldown--;
        if (strafeSwitchCooldown > 0) strafeSwitchCooldown--;

        final double dist = mob.distanceTo(target);
        final boolean canSee = mob.getSensing().hasLineOfSight(target);

        // 体と視線の向きは target 基準
        student.requestLookTarget(target, 80, 2);

        currentIntent = decideIntent(dist, canSee);

        switch (currentIntent) {
            case RETREAT -> moveRetreat();
            case APPROACH -> moveApproach();
            case STRAFE_LEFT -> moveStrafe(true);
            case STRAFE_RIGHT -> moveStrafe(false);
            case HOLD -> mob.getNavigation().stop();
        }
    }

    private MoveIntent decideIntent(double dist, boolean canSee) {
        // 緊急距離なら問答無用で離れる
        if (dist <= EMERGENCY_RETREAT_DIST) {
            return MoveIntent.RETREAT;
        }

        // 視線が切れていて近いなら横移動で見通しを作る
        if (!canSee) {
            if (dist < 6.0) {
                maybeFlipStrafe();
                return strafeLeft ? MoveIntent.STRAFE_LEFT : MoveIntent.STRAFE_RIGHT;
            }
            return MoveIntent.APPROACH;
        }

        // 遠すぎるなら詰める
        if (dist >= FORCE_APPROACH_DIST) {
            return MoveIntent.APPROACH;
        }

        // 近すぎるなら少し離れる
        if (dist < PREFERRED_MIN) {
            return MoveIntent.RETREAT;
        }

        // 中距離の中でも少し動いて的をずらす
        if (dist >= PREFERRED_MIN && dist <= PREFERRED_MAX) {
            maybeFlipStrafe();
            return strafeLeft ? MoveIntent.STRAFE_LEFT : MoveIntent.STRAFE_RIGHT;
        }

        // やや遠めなら少しだけ詰める
        if (dist > PREFERRED_MAX) {
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

    private void moveRetreat() {
        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        Vec3 away = mob.position().subtract(target.position());
        away = new Vec3(away.x, 0, away.z);

        if (away.lengthSqr() < 1.0e-6) return;

        Vec3 dir = away.normalize();
        Vec3 desired = mob.position().add(dir.scale(6.0));

        // 少し横にも逃がして、真後ろ一直線を避ける
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double sideOffset = (mob.getRandom().nextBoolean() ? 1.0 : -1.0) * 2.0;
        desired = desired.add(side.scale(sideOffset));

        Vec3 pos = DefaultRandomPos.getPosTowards(mob, 12, 7, desired, Math.PI / 2);
        if (pos == null) pos = desired;

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_RETREAT);

        if (mob instanceof AbstractStudentEntity ase) {
            ase.requestLookMoveDir(80, 4);
        }
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

        double len = 3.0 + mob.getRandom().nextDouble() * 1.5;
        Vec3 desired = mob.position().add(side.normalize().scale(len));

        // 少しだけ target 方向にも寄せて、完全な真横逃げになりすぎないようにする
        desired = desired.add(dir.scale(0.8));

        Vec3 pos = DefaultRandomPos.getPosTowards(mob, 10, 6, desired, Math.PI / 2);
        if (pos == null) pos = desired;

        mob.getNavigation().moveTo(pos.x, pos.y, pos.z, SPEED_STRAFE);

        if (mob instanceof AbstractStudentEntity ase) {
            ase.requestLookMoveDir(80, 4);
        }
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