package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class StudentReturnToOwnerGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;
    private final double speed;

    private final double triggerDist;
    private final double stopDist;
    private final double teleportDistSq;

    private final int stuckTriggerTicks;

    private Player owner;

    private int repathCooldown = 0;
    private static final int REPATH_INTERVAL = 10;

    private Vec3 lastPos = Vec3.ZERO;
    private int noMoveTicks = 0;

    public StudentReturnToOwnerGoal(
            PathfinderMob mob, IStudentEntity student,
            double speed,
            double triggerDist, double stopDist,
            double teleportDist,
            int stuckTriggerTicks
    ) {
        this.mob = mob;
        this.student = student;
        this.speed = speed;
        this.triggerDist = triggerDist;
        this.stopDist = stopDist;
        this.teleportDistSq = teleportDist * teleportDist;
        this.stuckTriggerTicks = stuckTriggerTicks;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;

        owner = resolveOwner();
        if (owner == null || !owner.isAlive()) return false;

        double dist2 = mob.distanceToSqr(owner);

        if (dist2 > triggerDist * triggerDist) return true;

        updateStuckCounter();
        return dist2 > 9.0 && noMoveTicks >= stuckTriggerTicks;
    }

    @Override
    public boolean canContinueToUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;
        if (owner == null || !owner.isAlive()) return false;

        updateStuckCounter();
        return mob.distanceToSqr(owner) > stopDist * stopDist;
    }

    @Override
    public void start() {
        repathCooldown = 0;
        lastPos = mob.position();
        noMoveTicks = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        owner = null;
        noMoveTicks = 0;
    }

    @Override
    public void tick() {
        if (owner == null) return;

        double dist2 = mob.distanceToSqr(owner);

        if (dist2 > teleportDistSq) {
            tryTeleportNearOwner();
            repathCooldown = 0;
            noMoveTicks = 0;
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
            return;
        }
        repathCooldown = REPATH_INTERVAL;

        mob.getNavigation().moveTo(owner, speed);
    }

    private void updateStuckCounter() {
        Vec3 cur = mob.position();

        boolean barelyMoved = cur.distanceToSqr(lastPos) < 0.0025;
        boolean tryingToMove = !mob.getNavigation().isDone() || mob.horizontalCollision;

        if (barelyMoved && tryingToMove) noMoveTicks++;
        else noMoveTicks = 0;

        lastPos = cur;
    }

    private Player resolveOwner() {
        if (student instanceof AbstractStudentEntity se) {
            Player p = se.getOwnerPlayer();
            if (p != null) return p;
        }
        return mob.level().getNearestPlayer(mob, 32.0);
    }

    private void tryTeleportNearOwner() {
        BlockPos base = owner.blockPosition();

        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (isSafeTeleportPos(p)) {
                        mob.snapTo(
                                p.getX() + 0.5,
                                p.getY(),
                                p.getZ() + 0.5,
                                mob.getYRot(),
                                mob.getXRot()
                        );
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

        return w.getBlockState(p).isAir() && w.getBlockState(p.above()).isAir();
    }
}