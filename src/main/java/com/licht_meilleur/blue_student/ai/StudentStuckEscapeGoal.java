package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class StudentStuckEscapeGoal extends Goal {
    private final PathfinderMob mob;
    private final IStudentEntity student;

    private LivingEntity threat;

    private Vec3 lastPos = null;
    private int noMoveTicks = 0;

    private int escapeTicks = 0;
    private int repathCooldown = 0;

    private static final int STUCK_TICKS = 16;
    private static final double MOVE_EPS2 = 0.00035;
    private static final double SPEED_EPS2 = 0.00045;
    private static final double THREAT_RADIUS = 12.0;

    private static final int ESCAPE_DURATION = 40;
    private static final int PUSH_DURATION = 10;
    private static final int REPATH_INTERVAL = 20;
    private static final double PUSH_SPEED = 0.55;
    private static final double ANCHOR_SPEED = 1.35;
    private static final double MAX_DROP = 2.0;

    private static final double GUARD_RADIUS = 16.0;

    private Vec3 fixedDir = null;
    private int fixedDirLock = 0;
    private static final int FIXDIR_LOCK = 25;

    private Vec3 anchor = null;

    public StudentStuckEscapeGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (mob instanceof AbstractStudentEntity ase && ase.isBrActionActiveServer()) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        Vec3 now = mob.position();
        double moved2 = 0.0;
        if (lastPos != null) moved2 = now.distanceToSqr(lastPos);
        lastPos = now;

        double speed2 = mob.getDeltaMovement().horizontalDistanceSqr();
        boolean blocked = mob.horizontalCollision;

        LivingEntity near = findNearestHostile(THREAT_RADIUS);
        boolean inCombat = (near != null);

        boolean notMoving = (moved2 < MOVE_EPS2) || (speed2 < SPEED_EPS2);

        if (inCombat && (blocked || notMoving)) noMoveTicks++;
        else noMoveTicks = 0;

        if (noMoveTicks < STUCK_TICKS) return false;

        threat = near;
        return threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        return escapeTicks > 0;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();

        escapeTicks = ESCAPE_DURATION;
        repathCooldown = 0;

        fixedDir = computeEscapeDir();
        fixedDirLock = FIXDIR_LOCK;

        anchor = null;

        if (mob instanceof AbstractStudentEntity se) {
            se.requestDodge();
        }
    }

    @Override
    public void stop() {
        escapeTicks = 0;
        repathCooldown = 0;

        threat = null;
        anchor = null;

        mob.getNavigation().stop();
        noMoveTicks = 0;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;

        if (escapeTicks > 0) escapeTicks--;
        if (repathCooldown > 0) repathCooldown--;

        if (threat == null || !threat.isAlive()) {
            threat = findNearestHostile(THREAT_RADIUS);
        }

        if (fixedDirLock > 0) {
            fixedDirLock--;
        } else {
            fixedDir = computeEscapeDir();
            fixedDirLock = FIXDIR_LOCK;
        }

        Vec3 dir = (fixedDir != null) ? fixedDir : new Vec3(0, 0, -1);

        int elapsed = ESCAPE_DURATION - escapeTicks;

        if (elapsed < PUSH_DURATION) {
            mob.getNavigation().stop();

            Vec3 push = dir.normalize().scale(PUSH_SPEED);
            mob.setDeltaMovement(push.x, mob.getDeltaMovement().y, push.z);

            if (mob.horizontalCollision && mob.onGround()) {
                mob.getJumpControl().jump();
                if (mob instanceof AbstractStudentEntity se) {
                    se.requestJump();
                }
            }
            return;
        }

        if (repathCooldown > 0) return;
        repathCooldown = REPATH_INTERVAL;

        anchor = findBestAnchor(dir);
        if (anchor == null) {
            Vec3 fallback = mob.position().add(dir.normalize().scale(5.0));
            fallback = clampToGuardAreaIfNeeded(fallback);
            anchor = fallback;
        }

        mob.getNavigation().moveTo(anchor.x, anchor.y, anchor.z, ANCHOR_SPEED);
    }

    private Vec3 computeEscapeDir() {
        if (threat != null) {
            Vec3 away = mob.position().subtract(threat.position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1e-6) return away.normalize();
        }

        Vec3 v = mob.getDeltaMovement();
        Vec3 hv = new Vec3(v.x, 0, v.z);
        if (hv.lengthSqr() > 1e-6) return hv.normalize();

        return new Vec3(0, 0, -1);
    }

    private Vec3 findBestAnchor(Vec3 baseDir) {
        Vec3 start = mob.position();
        Vec3 dir = baseDir.normalize();
        Vec3 right = new Vec3(-dir.z, 0, dir.x);

        Vec3[] dirs = new Vec3[]{
                dir,
                dir.add(right).normalize(),
                dir.subtract(right).normalize(),
                right,
                right.scale(-1),
                dir.scale(-1)
        };

        Vec3 best = null;
        double bestScore = -1e18;

        for (Vec3 d : dirs) {
            for (int step : new int[]{5, 7, 9}) {
                Vec3 desired = start.add(d.scale(step));
                desired = clampToGuardAreaIfNeeded(desired);

                Vec3 pos = DefaultRandomPos.getPosTowards(mob, 14, 7, desired, Math.PI / 2);
                if (pos == null) continue;

                if (pos.y < mob.getY() - MAX_DROP) continue;
                if (!isSafeVec(pos)) continue;

                double score = opennessScore(pos);

                if (threat != null) {
                    score += pos.distanceToSqr(threat.position()) * 0.03;
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = pos;
                }
            }
        }

        return best;
    }

    private boolean isSafeVec(Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
        return isWalkableAndSafe(bp);
    }

    private boolean isWalkableAndSafe(BlockPos pos) {
        var w = mob.level();

        BlockPos below = pos.below();

        if (pos.getY() < mob.getBlockY() - (int) MAX_DROP) return false;

        if (w.getBlockState(below).isAir()) return false;
        if (w.getBlockState(below).getCollisionShape(w, below).isEmpty()) return false;

        if (!w.getBlockState(pos).getCollisionShape(w, pos).isEmpty()) return false;
        if (!w.getBlockState(pos.above()).getCollisionShape(w, pos.above()).isEmpty()) return false;

        if (!w.getFluidState(below).isEmpty() && w.getFluidState(below).is(Fluids.LAVA)) return false;
        if (w.getBlockState(below).is(Blocks.MAGMA_BLOCK)) return false;

        return true;
    }

    private double opennessScore(Vec3 p) {
        var w = mob.level();
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);

        int solid = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos b1 = bp.offset(dx, 0, dz);
                BlockPos b2 = bp.offset(dx, 1, dz);

                if (!w.getBlockState(b1).getCollisionShape(w, b1).isEmpty()) solid++;
                if (!w.getBlockState(b2).getCollisionShape(w, b2).isEmpty()) solid++;
            }
        }
        return -solid;
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

    private LivingEntity findNearestHostile(double r) {
        AABB box = mob.getBoundingBox().inflate(r);
        List<LivingEntity> list = mob.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                e.isAlive() && e instanceof Monster
        );
        return list.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }
}