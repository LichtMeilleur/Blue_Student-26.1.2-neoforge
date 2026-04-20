package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class HinaFlyGoal extends Goal {
    private final HinaEntity mob;
    private final IStudentEntity student;

    public HinaFlyGoal(HinaEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
    }

    @Override
    public boolean canUse() {
        return mob.isFlying() && student.hasQueuedFire();
    }

    @Override
    public void start() {
        mob.setFlyShooting(true);
    }

    @Override
    public void stop() {
        mob.setFlyShooting(false);
    }

    @Override
    public void tick() {
        if (!student.hasQueuedFire()) {
            mob.setFlyShooting(false);
        }
    }
}