package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class StudentEvadeGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private LivingEntity target;

    private int evadeTicks = 0;
    private int stepCooldown = 0;

    private static final int EVADE_DURATION = 10;
    private static final int STEP_COOLDOWN = 60;
    private static final double STEP_DIST = 3.0;
    private static final double STEP_SPEED = 3.0;
    private static final double MAX_DROP = 2.0;

    private static final int EVADE_GLOBAL_COOLDOWN = 60;
    private int lastEvadeStartAge = -999999;

    private static final double GUARD_RADIUS = 14.0;

    public StudentEvadeGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        StudentAiMode mode = student.getAiMode();
        if (mob instanceof AbstractStudentEntity ase && ase.isBrActionActiveServer()) return false;
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;
        if (mob.level().isClientSide()) return false;

        if (mob.tickCount - lastEvadeStartAge < EVADE_GLOBAL_COOLDOWN) return false;

        target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
        }
        if (target == null) return false;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(target);

        double danger = Math.max(5.0, spec.preferredMinRange);
        return dist < danger;
    }

    @Override
    public boolean canContinueToUse() {
        if (evadeTicks > 0) return true;

        if (target == null || !target.isAlive()) return false;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(target);

        return dist < (spec.preferredMinRange + 1.5);
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        evadeTicks = EVADE_DURATION;
        stepCooldown = 0;
        student.setEvading(true);
    }

    @Override
    public void stop() {
        evadeTicks = 0;
        stepCooldown = 0;
        target = null;
        student.setEvading(false);
        lastEvadeStartAge = mob.tickCount;
    }

    @Override
    public void tick() {
        if (evadeTicks > 0) evadeTicks--;
        if (stepCooldown > 0) stepCooldown--;

        if (stepCooldown == 0) {
            boolean stepped = tryStep8DirFreeSpace();
            if (stepped) {
                stepCooldown = STEP_COOLDOWN;
            }
        }

        if (mob.horizontalCollision && mob.onGround()) {
            mob.getJumpControl().jump();
            if (mob instanceof AbstractStudentEntity se) {
                se.requestJump();
            }
        }
    }

    private boolean tryStep8DirFreeSpace() {
        float yaw = mob.getYRot();
        Vec3 forward = yawToDir(yaw);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        Vec3[] dirs = new Vec3[] {
                forward.scale(-1.0),
                forward.scale(-1.0).add(right).normalize(),
                forward.scale(-1.0).subtract(right).normalize(),
                right,
                right.scale(-1.0),
                forward.scale(-0.5).add(right).normalize(),
                forward.scale(-0.5).subtract(right).normalize(),
                forward
        };

        Vec3 start = mob.position();

        Vec3 bestDir = null;
        Vec3 bestPos = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (Vec3 d : dirs) {
            Vec3 desired = start.add(d.scale(STEP_DIST));
            desired = clampToGuardAreaIfNeeded(desired);

            if (!isSafeStepDestination(start, desired)) continue;

            double open = opennessScore(start, d, 3.0);
            double align = d.dot(forward.scale(-1.0));

            double score = open * 2.0 + align * 0.2;

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
                bestPos = desired;
            }
        }

        if (bestDir == null || bestPos == null) return false;

        mob.setDeltaMovement(bestDir.x * STEP_SPEED, mob.getDeltaMovement().y, bestDir.z * STEP_SPEED);

        if (mob instanceof AbstractStudentEntity se) {
            se.requestDodge();
        }

        mob.getNavigation().moveTo(bestPos.x, bestPos.y, bestPos.z, 1.2);
        return true;
    }

    private Vec3 yawToDir(float yawDeg) {
        double rad = Math.toRadians(yawDeg);
        double x = -Math.sin(rad);
        double z = Math.cos(rad);
        return new Vec3(x, 0, z).normalize();
    }

    private double opennessScore(Vec3 start, Vec3 dir, double rayDistance) {
        var w = mob.level();
        double score = 0.0;

        Vec3 d = new Vec3(dir.x, 0, dir.z);
        if (d.lengthSqr() < 1e-6) return 0.0;
        d = d.normalize();

        for (double t = 0.5; t <= rayDistance; t += 0.5) {
            Vec3 p = start.add(d.scale(t));
            BlockPos bp = BlockPos.containing(p.x, p.y, p.z);

            if (!w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()) score -= 2.0;
            if (!w.getBlockState(bp.above()).getCollisionShape(w, bp.above()).isEmpty()) score -= 2.0;

            if (w.getBlockState(bp).isAir() && w.getBlockState(bp.above()).isAir()) score += 1.0;
        }

        return score;
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

    private boolean isSafeStepDestination(Vec3 from, Vec3 to) {
        var w = mob.level();
        BlockPos bp = BlockPos.containing(to.x, to.y, to.z);

        double dy = from.y - to.y;
        if (dy > MAX_DROP) return false;

        BlockPos below = bp.below();
        if (isDangerousFloor(below)) return false;

        if (w.getBlockState(below).isAir()) return false;
        if (w.getBlockState(below).getCollisionShape(w, below).isEmpty()) return false;

        if (!w.getBlockState(bp).isAir()) return false;
        if (!w.getBlockState(bp.above()).isAir()) return false;

        var shape = w.getBlockState(bp).getCollisionShape(w, bp);
        return shape.isEmpty();
    }

    private LivingEntity findNearestHostile() {
        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        AABB box = mob.getBoundingBox().inflate(Math.max(10.0, spec.range));

        return mob.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                        e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private List<LivingEntity> findCloseThreats(double r) {
        AABB box = mob.getBoundingBox().inflate(r);
        return mob.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e instanceof Monster
        );
    }

    private double dangerScoreAt(Vec3 pos, List<LivingEntity> threats) {
        double score = 0.0;
        for (LivingEntity e : threats) {
            double d2 = e.distanceToSqr(pos);
            score += 1.0 / Math.max(0.25, d2);
        }
        return score;
    }

    private boolean isDangerousFloor(BlockPos below) {
        var w = mob.level();
        BlockState st = w.getBlockState(below);

        if (st.is(Blocks.MAGMA_BLOCK)) return true;
        return !w.getFluidState(below).isEmpty() && w.getFluidState(below).is(Fluids.LAVA);
    }
}