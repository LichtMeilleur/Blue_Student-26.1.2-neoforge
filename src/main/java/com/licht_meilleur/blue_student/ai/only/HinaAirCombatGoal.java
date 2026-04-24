package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
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

    private static final double R_MIN = 7.0;
    private static final double R_MAX = 12.0;
    private static final int REPLAN = 3;

    private static final double HOVER_AGL = 6.0;
    private static final double MIN_ABOVE_TARGET = 2.5;
    private static final double MAX_ABOVE_TARGET = 5.5;

    private static final double H_SPEED = 0.42;
    private static final double V_SPEED_MAX = 0.18;

    private int fireCd = 0;

    private static final double RAY_DOWN = 48.0;

    public HinaAirCombatGoal(HinaEntity mob, IStudentEntity student) {
        this.mob = mob;
        this.student = student;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!mob.isFlying()) return false;

        target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
        }

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
        mob.getNavigation().stop();
        mob.setNoGravity(true);
    }

    @Override
    public void stop() {
        mob.setFlyShooting(false);
        target = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel)) return;

        if (target == null || !target.isAlive()) {
            target = findNearestHostile();
            return;
        }

        student.requestLookTarget(target, 140, 4);

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
            d = 1.0;
        } else {
            flat = flat.normalize();
        }

        Vec3 tangent = cw
                ? new Vec3(-flat.z, 0, flat.x)
                : new Vec3(flat.z, 0, -flat.x);

        double desiredR;
        if (d < R_MIN) desiredR = R_MIN + 1.5;
        else if (d > R_MAX) desiredR = R_MAX - 1.5;
        else desiredR = d;

        Vec3 radialPos = tp.add(flat.scale(desiredR));
        Vec3 orbitPos = radialPos.add(tangent.scale(2.6));

        double t = mob.tickCount * 0.12;
        orbitPos = orbitPos.add(Math.cos(t) * 0.8, 0, Math.sin(t) * 0.8);

        double groundY = findGroundY();
        double desiredYFromGround = groundY + HOVER_AGL;
        double desiredYFromTarget = target.getEyeY() + 3.5;

        double desiredY = Math.min(desiredYFromGround, desiredYFromTarget);
        desiredY = Mth.clamp(desiredY, target.getY() + MIN_ABOVE_TARGET, target.getY() + MAX_ABOVE_TARGET);

        double dy = desiredY - my.y;
        double vy = Mth.clamp(dy * 0.22, -V_SPEED_MAX, V_SPEED_MAX);

        // 高く行きすぎた時は強制下降
        double aboveTarget = my.y - target.getY();
        if (aboveTarget > MAX_ABOVE_TARGET + 1.0) {
            vy = -V_SPEED_MAX;
        }

        Vec3 toGoalXZ = new Vec3(orbitPos.x - my.x, 0, orbitPos.z - my.z);
        Vec3 vxz;
        if (toGoalXZ.lengthSqr() > 1.0e-6) {
            vxz = toGoalXZ.normalize().scale(H_SPEED);
        } else {
            vxz = Vec3.ZERO;
        }

        mob.getNavigation().stop();
        mob.setNoGravity(true);
        mob.setDeltaMovement(vxz.x, vy, vxz.z);
        mob.fallDistance = 0;

        if (vxz.lengthSqr() > 1.0e-6) {
            float yaw = (float) (Math.toDegrees(Math.atan2(vxz.z, vxz.x)) - 90.0);
            mob.setYRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
        }
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
}