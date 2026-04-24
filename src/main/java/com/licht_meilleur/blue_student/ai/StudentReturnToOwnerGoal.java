package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
    private static final int REPATH_INTERVAL = 5;

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

        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;

        owner = resolveOwner();
        if (owner == null || !owner.isAlive()) return false;

        // 別ディメンションはここではやらない
        if (owner.level() != mob.level()) return false;

        double dist2 = mob.distanceToSqr(owner);

        // オーナーがかなり遠いなら最優先で戻る
        if (dist2 > triggerDist * triggerDist) return true;

        // 戦闘中でも、ある程度離されたら戻る
        if (dist2 > 18.0 * 18.0) return true;

        updateStuckCounter();
        return dist2 > 9.0 && noMoveTicks >= stuckTriggerTicks;
    }

    @Override
    public boolean canContinueToUse() {
        if (student.getAiMode() != StudentAiMode.FOLLOW) return false;
        if (owner == null || !owner.isAlive()) return false;
        if (owner.level() != mob.level()) return false;

        updateStuckCounter();

        double dist2 = mob.distanceToSqr(owner);

        return dist2 > stopDist * stopDist;
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
        if (!owner.isAlive()) return;
        if (owner.level() != mob.level()) return;

        lookAtOwner();

        double dist2 = mob.distanceToSqr(owner);

        // 超遠距離は即テレポ
        if (dist2 > teleportDistSq) {
            tryTeleportNearOwner();
            repathCooldown = 0;
            noMoveTicks = 0;
            return;
        }

        // 中距離で詰まっていたらテレポ
        if (noMoveTicks >= stuckTriggerTicks && dist2 > 8.0 * 8.0) {
            tryTeleportNearOwner();
            repathCooldown = 0;
            noMoveTicks = 0;
            return;
        }

        if (repathCooldown > 0) {
            repathCooldown--;
        } else {
            repathCooldown = REPATH_INTERVAL;
            moveTowardOwner();
        }

        // ナビが終わっていてまだ少し遠いなら軽く押す
        if (mob.getNavigation().isDone() && dist2 > stopDist * stopDist) {
            nudgeTowardOwner();
        }
    }

    private void moveTowardOwner() {
        if (owner == null) return;

        // owner が高すぎる位置にいるときは、足元寄りへ向かわせる
        double targetY = owner.getY();
        double dy = targetY - mob.getY();

        if (dy > 3.0) {
            targetY = mob.getY();
        }

        mob.getNavigation().moveTo(owner.getX(), targetY, owner.getZ(), speed);
    }

    private void updateStuckCounter() {
        Vec3 cur = mob.position();

        boolean barelyMoved = cur.distanceToSqr(lastPos) < 0.0025;
        boolean tryingToMove =
                !mob.getNavigation().isDone()
                        || mob.horizontalCollision
                        || mob.getDeltaMovement().horizontalDistanceSqr() > 1.0e-5;

        if (barelyMoved && tryingToMove) {
            noMoveTicks++;
        } else {
            noMoveTicks = 0;
        }

        lastPos = cur;
    }

    private Player resolveOwner() {
        if (student instanceof AbstractStudentEntity se) {
            Player p = se.getOwnerPlayer();
            if (p != null) return p;
        }

        return mob.level().getNearestPlayer(mob, 32.0);
    }

    private void lookAtOwner() {
        if (owner == null) return;

        Vec3 to = owner.position().subtract(mob.position());
        double dx = to.x;
        double dz = to.z;

        if (dx * dx + dz * dz < 1.0e-6) return;

        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        mob.setYRot(yaw);
        mob.yBodyRot = yaw;
        mob.yHeadRot = yaw;
    }

    private void nudgeTowardOwner() {
        if (owner == null) return;

        Vec3 dir = owner.position().subtract(mob.position());
        Vec3 flat = new Vec3(dir.x, 0.0, dir.z);

        if (flat.lengthSqr() < 1.0e-6) return;

        Vec3 n = flat.normalize();
        Vec3 v = mob.getDeltaMovement();

        mob.setDeltaMovement(
                n.x * 0.18,
                v.y,
                n.z * 0.18
        );
    }

    private void tryTeleportNearOwner() {
        if (owner == null) return;

        BlockPos base = owner.blockPosition();

        // owner が少し浮いていても足元近くを優先して探す
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (isSafeTeleportPos(p)) {
                        mob.snapTo(
                                p.getX() + 0.5,
                                p.getY(),
                                p.getZ() + 0.5,
                                owner.getYRot(),
                                mob.getXRot()
                        );
                        mob.getNavigation().stop();
                        mob.setDeltaMovement(Vec3.ZERO);
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
        if (below.getCollisionShape(w, p.below()).isEmpty()) return false;

        if (!w.getFluidState(p).isEmpty()) return false;
        if (!w.getFluidState(p.above()).isEmpty()) return false;

        return w.getBlockState(p).getCollisionShape(w, p).isEmpty()
                && w.getBlockState(p.above()).getCollisionShape(w, p.above()).isEmpty();
    }
}