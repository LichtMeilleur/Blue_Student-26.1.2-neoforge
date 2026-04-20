package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.AliceEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.HitscanWeaponAction;
import com.licht_meilleur.blue_student.weapon.WeaponAction;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

import java.util.EnumSet;

public class AliceHyperShotGoal extends Goal {

    private final AbstractStudentEntity mob;
    private final IStudentEntity student;

    private final WeaponAction hitscan = new HitscanWeaponAction();

    private static final int COOLDOWN = 160;
    private static final int CHARGE_TICKS = 20;

    private int cooldown = 0;
    private int charging = 0;

    private LivingEntity target;

    public AliceHyperShotGoal(AbstractStudentEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.getForm() == StudentForm.BR) return false;
        if (mob.level().isClientSide()) return false;
        if (mob.isLifeLockedForGoal()) return false;

        if (cooldown > 0) {
            cooldown--;
            return false;
        }

        target = findNearestEnemy();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return charging > 0 && target != null && target.isAlive();
    }

    @Override
    public void start() {
        charging = CHARGE_TICKS;
    }

    @Override
    public void stop() {
        charging = 0;
        target = null;
    }

    @Override
    public void tick() {
        if (charging <= 0) return;

        charging--;

        mob.getNavigation().stop();
        mob.setDeltaMovement(0, 0, 0);

        if (target != null) {
            student.requestLookTarget(target, 5, 5);
        }

        if (charging == 0 && target != null && target.isAlive()) {
            if (mob instanceof AliceEntity ae) {
                ae.requestHyperShot();
            }
            hitscan.shoot(student, target, WeaponSpecs.ALICE_HYPER);
            cooldown = COOLDOWN;
        }
    }

    private LivingEntity findNearestEnemy() {
        var world = mob.level();
        double range = 40.0;

        LivingEntity best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Monster e : world.getEntitiesOfClass(
                Monster.class,
                mob.getBoundingBox().inflate(range),
                x -> x.isAlive()
        )) {
            double d2 = mob.distanceToSqr(e);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }

        return best;
    }
}