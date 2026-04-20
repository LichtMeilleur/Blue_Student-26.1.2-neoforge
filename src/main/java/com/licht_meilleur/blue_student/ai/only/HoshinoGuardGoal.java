package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HoshinoEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentForm;
import net.minecraft.world.entity.ai.goal.Goal;

public class HoshinoGuardGoal extends Goal {

    private final HoshinoEntity mob;
    private final IStudentEntity student;

    private int keepTicks = 0;

    public HoshinoGuardGoal(HoshinoEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
    }

    @Override
    public boolean canUse() {
        if (mob.getForm() == StudentForm.BR) return false;
        return mob.isGuarding() && student.hasQueuedFire(IStudentEntity.FireChannel.MAIN);
    }

    @Override
    public void start() {
        mob.setGuardShooting(true);
        keepTicks = 8;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.getForm() == StudentForm.BR) return false;
        return mob.isGuarding() && keepTicks > 0;
    }

    @Override
    public void tick() {
        keepTicks--;
    }

    @Override
    public void stop() {
        mob.setGuardShooting(false);
    }
}