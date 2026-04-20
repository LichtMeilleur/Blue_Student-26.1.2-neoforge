package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class HoshinoBrEvadeGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private LivingEntity threat;
    private int repathCooldown = 0;

    private static final int REPATH_INTERVAL = 8;
    private static final double EVADE_SPEED = 1.35;
    private static final double TOO_CLOSE_PAD = 0.75;

    public HoshinoBrEvadeGoal(PathfinderMob mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (mob instanceof AbstractStudentEntity ase && ase.isBrActionActiveServer()) return false;

        if (student instanceof AbstractStudentEntity ase) {
            if (ase.getForm() != StudentForm.BR) return false;
        }

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        threat = mob.getTarget();
        if (threat == null || !threat.isAlive()) {
            threat = findNearestHostile();
        }
        if (threat == null) return false;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(threat);

        return dist < (spec.preferredMinRange + TOO_CLOSE_PAD) || dist < spec.panicRange;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (threat == null || !threat.isAlive()) return false;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(threat);

        return dist < (spec.preferredMinRange + 2.0);
    }

    @Override
    public void start() {
        student.setEvading(true);
        repathCooldown = 0;
        student.requestLookAwayFrom(threat, 80, 6);
    }

    @Override
    public void stop() {
        student.setEvading(false);
        threat = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;
        if (threat == null || !threat.isAlive()) return;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(threat);

        if (repathCooldown > 0) repathCooldown--;

        student.requestLookAwayFrom(threat, 80, 2);

        if (repathCooldown <= 0) {
            repathCooldown = REPATH_INTERVAL;

            Vec3 away = mob.position().subtract(threat.position());
            away = new Vec3(away.x, 0, away.z);
            if (away.lengthSqr() > 1.0e-6) {
                Vec3 desired = mob.position().add(away.normalize().scale(6.0));

                Vec3 right = new Vec3(-away.z, 0, away.x).normalize();
                double side = (mob.getRandom().nextBoolean() ? 1.0 : -1.0) * 2.0;
                desired = desired.add(right.scale(side));

                Vec3 pos = DefaultRandomPos.getPosTowards(mob, 12, 7, desired, Math.PI / 2);
                if (pos == null) pos = desired;

                mob.getNavigation().moveTo(pos.x, pos.y, pos.z, EVADE_SPEED);
            }
        }
    }

    private LivingEntity findNearestHostile() {
        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        AABB box = mob.getBoundingBox().inflate(spec.range + 6.0);

        return mob.level()
                .getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e instanceof Monster)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}