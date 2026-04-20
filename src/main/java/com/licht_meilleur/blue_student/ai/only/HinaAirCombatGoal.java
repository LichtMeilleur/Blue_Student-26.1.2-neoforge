package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class HinaAirCombatGoal extends Goal {
    private final HinaEntity mob;
    private final IStudentEntity student;

    private LivingEntity target;
    private int replanTicks = 0;
    private boolean cw;

    private static final double ALT = 4.0;
    private static final double R_MIN = 7.0;
    private static final double R_MAX = 12.0;
    private static final int REPLAN = 5;
    private static final double SPEED = 1.2;

    private int fireCd = 0;

    private static final double RAY_DOWN = 48.0;
    private static final double HOVER_AGL = 6.0;

    public HinaAirCombatGoal(HinaEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isFlying()) return false;

        target = findNearestHostile();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return mob.isFlying() && target != null && target.isAlive();
    }

    @Override
    public void start() {
        cw = mob.getRandom().nextBoolean();
        replanTicks = 0;
        fireCd = 0;
        mob.setFlyShooting(true);
    }

    @Override
    public void stop() {
        mob.setFlyShooting(false);
        target = null;
    }

    @Override
    public void tick() {
        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
            return;
        }

        student.requestLookTarget(target, 80, 2);

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double dist = mob.distanceTo(target);

        if (fireCd > 0) fireCd--;

        if (dist <= spec.range) {
            if (fireCd == 0) {
                student.queueFire(target);
                fireCd = Math.max(1, spec.cooldownTicks);
            }
        }

        if (replanTicks-- > 0) return;
        replanTicks = REPLAN;

        Vec3 tp = target.position();
        Vec3 my = mob.position();

        Vec3 toMe = my.subtract(tp);
        Vec3 flat = new Vec3(toMe.x, 0, toMe.z);
        double d = Math.sqrt(flat.lengthSqr());

        if (d < 1e-3) {
            flat = new Vec3(1, 0, 0);
            d = 1;
        } else {
            flat = flat.normalize();
        }

        Vec3 tangent = cw
                ? new Vec3(-flat.z, 0, flat.x)
                : new Vec3(flat.z, 0, -flat.x);

        double desiredR = (d < R_MIN) ? R_MAX : (d > R_MAX ? R_MIN : d);
        Vec3 radialPos = tp.add(flat.scale(desiredR));
        Vec3 orbitPos = radialPos.add(tangent.scale(3.0));

        double t = mob.tickCount * 0.15;
        orbitPos = orbitPos.add(Math.cos(t) * 1.2, 0, Math.sin(t) * 1.2);

        double groundY = findGroundY();
        double desiredY = groundY + HOVER_AGL;
        double myY = mob.getY();

        double err = desiredY - myY;
        double step = clamp(err * 0.25, -0.35, 0.35);
        double goalY = myY + step;

        double maxAboveTarget = 6.0;
        goalY = Math.min(goalY, target.getEyeY() + maxAboveTarget);

        Vec3 goal = new Vec3(orbitPos.x, goalY, orbitPos.z);
        mob.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, SPEED);
    }

    private LivingEntity findNearestHostile() {
        if (!(mob.level() instanceof ServerLevel sw)) return null;

        WeaponSpec spec = WeaponSpecs.forStudent(student.getStudentId());
        double r = Math.max(12.0, spec.range);

        AABB box = mob.getBoundingBox().inflate(r);

        return sw.getEntitiesOfClass(LivingEntity.class, box, e ->
                        e.isAlive() && e instanceof Monster && e != mob)
                .stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private double findGroundY() {
        var w = mob.level();

        Vec3 from = mob.position().add(0, 0.2, 0);
        Vec3 to = from.add(0, -RAY_DOWN, 0);

        var hit = w.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation().y;
        }

        return mob.getY() - 2.0;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}