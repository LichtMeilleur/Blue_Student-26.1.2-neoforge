package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class StudentCliffAvoidGoal extends Goal {

    private final AbstractStudentEntity student;
    private int avoidTicks;
    private Vec3 backDir = Vec3.ZERO;

    private static final int MAX_SAFE_DROP = 3;

    public StudentCliffAvoidGoal(AbstractStudentEntity student) {
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (student.level().isClientSide()) return false;
        if (!student.onGround()) return false;
        if (student.isPassenger()) return false;
        if (student.isLifeLockedForGoal()) return false;

        Vec3 v = student.getDeltaMovement();
        double h2 = v.x * v.x + v.z * v.z;

        if (h2 < 0.001) {
            Vec3 look = student.getLookAngle();
            v = new Vec3(look.x, 0, look.z);
            h2 = v.x * v.x + v.z * v.z;
        }

        if (h2 < 0.001) return false;

        Vec3 dir = new Vec3(v.x, 0, v.z).normalize();

        if (!hasGroundAhead(dir)) {
            backDir = dir.reverse();
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return avoidTicks > 0;
    }

    @Override
    public void start() {
        avoidTicks = 8;
        student.getNavigation().stop();
        student.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        avoidTicks--;

        student.getNavigation().stop();

        Vec3 push = backDir.scale(0.08);
        student.setDeltaMovement(push.x, student.getDeltaMovement().y, push.z);
    }

    @Override
    public void stop() {
        avoidTicks = 0;
        student.setDeltaMovement(0, student.getDeltaMovement().y, 0);
    }

    private boolean hasGroundAhead(Vec3 dir) {
        Vec3 pos = student.position();

        // 0.6〜1.4ブロック先を見る
        for (double d = 0.6; d <= 1.4; d += 0.4) {
            double x = pos.x + dir.x * d;
            double z = pos.z + dir.z * d;

            BlockPos foot = BlockPos.containing(x, student.getY() - 0.05, z);

            boolean foundGround = false;

            for (int drop = 0; drop <= MAX_SAFE_DROP; drop++) {
                BlockPos check = foot.below(drop);
                if (!student.level().getBlockState(check).getCollisionShape(student.level(), check).isEmpty()) {
                    foundGround = true;
                    break;
                }
            }

            if (!foundGround) {
                return false;
            }
        }

        return true;
    }
}