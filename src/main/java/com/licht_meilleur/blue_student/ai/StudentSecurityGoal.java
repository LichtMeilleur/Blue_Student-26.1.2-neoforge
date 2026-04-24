package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class StudentSecurityGoal extends Goal {
    private final PathfinderMob mob;
    private final ISecurityPosProvider security;
    private final IStudentEntity student;
    private final double speed;

    private int repathCooldown = 0;

    private static final int REPATH_INTERVAL = 10;
    private static final double RETURN_DIST = 3.0;

    public interface ISecurityPosProvider {
        BlockPos getSecurityPos();
        void setSecurityPos(BlockPos pos);
    }

    public StudentSecurityGoal(PathfinderMob mob, IStudentEntity student, ISecurityPosProvider security, double speed) {
        this.mob = mob;
        this.student = student;
        this.security = security;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (student.getAiMode() != StudentAiMode.SECURITY) return false;

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) return false;

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (student.getAiMode() != StudentAiMode.SECURITY) return false;

        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) return false;

        return true;
    }

    @Override
    public void start() {
        repathCooldown = 0;
    }

    @Override
    public void tick() {
        BlockPos p = security.getSecurityPos();
        if (p == null) {
            security.setSecurityPos(mob.blockPosition());
            return;
        }

        double dist2 = mob.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);

        if (dist2 > RETURN_DIST * RETURN_DIST) {
            if (repathCooldown > 0) {
                repathCooldown--;
                return;
            }
            repathCooldown = REPATH_INTERVAL;

            mob.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, speed);
        } else {
            mob.getNavigation().stop();
        }
    }
}