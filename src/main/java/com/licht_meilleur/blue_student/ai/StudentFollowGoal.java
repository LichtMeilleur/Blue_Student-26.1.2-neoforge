package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.UUID;

public class StudentFollowGoal extends Goal {
    private final PathfinderMob mob;
    private final IStudentEntity student;
    private final double speed;

    private Player owner;

    private final Deque<Vec3> waypoints = new ArrayDeque<>();
    private static final int MAX_WAYPOINTS = 40;
    private static final int RECORD_INTERVAL = 10;
    private static final double RECORD_MIN_DIST = 2.0;

    private int recordCooldown = 0;

    private int repathCooldown = 0;
    private static final int REPATH_INTERVAL = 10;

    private static final double STOP_DIST = 2.0;

    private Vec3 lastPos = null;
    private int stuckTicks = 0;
    private static final double MOVE_EPS2 = 0.00035;
    private static final int STUCK_THRESHOLD = 25;
    private static final int WARP_STUCK_THRESHOLD = 80;
    private static final double WARP_DIST = 24.0;

    public StudentFollowGoal(PathfinderMob mob, IStudentEntity student, double speed) {
        this.mob = mob;
        this.student = student;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;

        owner = resolveOwnerOnly();
        if (owner == null || !owner.isAlive()) return false;
        if (owner.level() != mob.level()) return false;

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;
        if (owner == null || !owner.isAlive()) return false;

        UUID uuid = student.getOwnerUuid();
        if (uuid == null || !uuid.equals(owner.getUUID())) return false;
        if (owner.level() != mob.level()) return false;

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) return false;

        return mob.distanceToSqr(owner) > (STOP_DIST * STOP_DIST);
    }

    @Override
    public void start() {
        repathCooldown = 0;
        recordCooldown = 0;
        stuckTicks = 0;
        lastPos = mob.position();
        waypoints.clear();

        waypoints.addLast(owner.position());
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        owner = null;
        waypoints.clear();
    }

    @Override
    public void tick() {
        if (owner == null) return;

        UUID uuid = student.getOwnerUuid();
        if (uuid == null || !uuid.equals(owner.getUUID())) {
            mob.getNavigation().stop();
            owner = null;
            waypoints.clear();
            return;
        }

        if (recordCooldown > 0) recordCooldown--;
        if (recordCooldown == 0) {
            recordCooldown = RECORD_INTERVAL;
            recordOwnerWaypoint();
        }

        updateStuck();

        double dist2 = mob.distanceToSqr(owner);
        if (dist2 < STOP_DIST * STOP_DIST) {
            mob.getNavigation().stop();
            stuckTicks = 0;
            return;
        }

        if (stuckTicks > STUCK_THRESHOLD) {
            rewindWaypoints();
            stuckTicks = 0;
        }

        if (stuckTicks > WARP_STUCK_THRESHOLD && dist2 > (WARP_DIST * WARP_DIST)) {
            tryTeleportNearOwner();
            stuckTicks = 0;
            repathCooldown = 0;
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        repathCooldown = REPATH_INTERVAL;

        Vec3 goal = getFollowGoalPosition();

        var path = mob.getNavigation().createPath(goal.x, goal.y, goal.z, 0);
        if (path == null) {
            int drop = 0;
            while (drop < 4 && !waypoints.isEmpty()) {
                waypoints.pollFirst();
                drop++;
                Vec3 g2 = getFollowGoalPosition();
                if (g2 == null) break;
                path = mob.getNavigation().createPath(g2.x, g2.y, g2.z, 0);
                if (path != null) {
                    goal = g2;
                    break;
                }
            }
        }
        if (path == null) {
            goal = owner.position();
        }

        if (isFlyingFollow() && mob.level() instanceof ServerLevel serverLevel) {
            flyMoveToward(serverLevel, goal);
        } else {
            mob.getNavigation().moveTo(goal.x, goal.y, goal.z, speed);
        }

        lookMoveDirectionOrOwner();
    }

    private void recordOwnerWaypoint() {
        Vec3 p = owner.position();

        Vec3 last = waypoints.peekLast();
        if (last != null && last.distanceToSqr(p) < (RECORD_MIN_DIST * RECORD_MIN_DIST)) return;

        waypoints.addLast(p);
        while (waypoints.size() > MAX_WAYPOINTS) {
            waypoints.pollFirst();
        }
    }

    private Vec3 getFollowGoalPosition() {
        Vec3 head = waypoints.peekFirst();
        if (head == null) return owner.position();

        double d2 = mob.distanceToSqr(head.x, head.y, head.z);
        if (d2 < 3.0 * 3.0) {
            waypoints.pollFirst();
            Vec3 next = waypoints.peekFirst();
            return (next != null) ? next : owner.position();
        }

        return head;
    }

    private void rewindWaypoints() {
        if (!waypoints.isEmpty()) waypoints.pollFirst();
        if (!waypoints.isEmpty()) waypoints.pollFirst();
    }

    private void updateStuck() {
        Vec3 now = mob.position();
        if (lastPos != null) {
            double moved2 = now.distanceToSqr(lastPos);
            double speed2 = mob.getDeltaMovement().horizontalDistanceSqr();
            boolean blocked = mob.horizontalCollision;
            boolean notMoving = (moved2 < MOVE_EPS2) || (speed2 < 0.0005);

            if (!mob.getNavigation().isDone() && (blocked || notMoving)) stuckTicks++;
            else stuckTicks = 0;
        }
        lastPos = now;
    }

    private void lookMoveDirectionOrOwner() {
        Vec3 v = mob.getDeltaMovement();
        Vec3 hv = new Vec3(v.x, 0, v.z);

        if (hv.lengthSqr() > 1.0e-4) {
            student.requestLookMoveDir(10, 2);
        } else {
            mob.getLookControl().setLookAt(owner, 30.0f, 30.0f);
        }
    }

    private Player resolveOwnerOnly() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return null;
        UUID uuid = student.getOwnerUuid();
        if (uuid == null) return null;
        Player p = serverLevel.getPlayerByUUID(uuid);
        return (p != null && p.isAlive()) ? p : null;
    }

    private void tryTeleportNearOwner() {
        BlockPos base = owner.blockPosition();

        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (isSafeTeleportPos(p)) {
                        mob.setPos(
                                p.getX() + 0.5,
                                p.getY(),
                                p.getZ() + 0.5
                        );
                        mob.setYRot(mob.getYRot());
                        mob.setXRot(mob.getXRot());
                        mob.getNavigation().stop();
                        return;
                    }
                }
            }
        }
    }

    private boolean isSafeTeleportPos(BlockPos p) {
        var w = mob.level();
        var below = w.getBlockState(p.below());
        if (below.isAir()) return false;
        if (!below.getCollisionShape(w, p.below()).isEmpty()) {
            return w.getBlockState(p).isAir() && w.getBlockState(p.above()).isAir();
        }
        return false;
    }

    private boolean isFlyingFollow() {
        return mob.isNoGravity();
    }

    private void flyMoveToward(ServerLevel serverLevel, Vec3 goal) {
        mob.getNavigation().stop();
        mob.setNoGravity(true);

        Vec3 pos = mob.position();
        Vec3 desired = goal.add(0, 1.2, 0);

        Vec3 to = desired.subtract(pos);
        double dist = to.length();
        if (dist < 0.001) {
            mob.setDeltaMovement(mob.getDeltaMovement().scale(0.5));
            return;
        }

        Vec3 from = pos.add(0, mob.getBbHeight() * 0.6, 0);
        BlockHitResult hit = serverLevel.clip(new ClipContext(
                from,
                desired,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        if (hit.getType() == HitResult.Type.BLOCK) {
            desired = desired.add(0, 1.0, 0);
            to = desired.subtract(pos);
            dist = to.length();
            if (dist < 0.001) return;
        }

        Vec3 dir = to.normalize();

        double spd = Math.min(speed * 1.2, 0.55);
        if (dist < 3.0) spd *= (dist / 3.0);

        Vec3 vel = mob.getDeltaMovement().scale(0.6).add(dir.scale(spd));

        mob.setDeltaMovement(vel);
        mob.fallDistance = 0;
    }
}