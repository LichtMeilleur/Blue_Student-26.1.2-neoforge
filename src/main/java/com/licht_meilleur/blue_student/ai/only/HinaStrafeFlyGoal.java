package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class HinaStrafeFlyGoal extends Goal {
    private final HinaEntity mob;
    private final IStudentEntity student;

    private LivingEntity target;

    private int tick;
    private double angle;
    private int dir = 1;

    private static final double RADIUS = 8.0;
    private static final double SPEED = 4.0;
    private static final double ALT_MIN = 3.0;
    private static final double ALT_MAX = 6.0;
    private static final int UPDATE_INTERVAL = 5;

    public HinaStrafeFlyGoal(HinaEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isFlying()) return false;

        target = findNearestHostile();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isFlying() && target != null && target.isAlive();
    }

    @Override
    public void start() {
        tick = 0;
        angle = mob.getRandom().nextDouble() * Math.PI * 2.0;
        dir = mob.getRandom().nextBoolean() ? 1 : -1;
    }

    @Override
    public void stop() {
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        tick++;

        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
            if (target == null) return;
        }

        if (tick % 60 == 0 && mob.getRandom().nextFloat() < 0.4f) {
            dir *= -1;
        }

        if (tick % UPDATE_INTERVAL != 0) return;

        double baseY = target.getY() + ALT_MIN + mob.getRandom().nextDouble() * (ALT_MAX - ALT_MIN);

        angle += dir * 0.25;

        double dx = Math.cos(angle) * RADIUS;
        double dz = Math.sin(angle) * RADIUS;

        Vec3 desired = new Vec3(target.getX() + dx, baseY, target.getZ() + dz);

        if (isBlocked(desired)) {
            angle += dir * 0.9;
            dx = Math.cos(angle) * RADIUS;
            dz = Math.sin(angle) * RADIUS;
            desired = new Vec3(target.getX() + dx, baseY, target.getZ() + dz);
        }

        mob.getNavigation().moveTo(desired.x, desired.y, desired.z, SPEED);
    }

    private boolean isBlocked(Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, p.y, p.z);
        var w = mob.level();
        return !w.getBlockState(bp).getCollisionShape(w, bp).isEmpty()
                || !w.getBlockState(bp.above()).getCollisionShape(w, bp.above()).isEmpty();
    }

    private LivingEntity findNearestHostile() {
        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double r = Math.max(12.0, spec.range + 8.0);

        AABB box = mob.getBoundingBox().inflate(r);
        return mob.level().getEntitiesOfClass(LivingEntity.class, box, e ->
                        e.isAlive() && e instanceof Monster
                ).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }
}