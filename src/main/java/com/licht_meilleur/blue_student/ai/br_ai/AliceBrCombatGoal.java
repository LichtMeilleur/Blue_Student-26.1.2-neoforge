package com.licht_meilleur.blue_student.ai.br_ai;

import com.licht_meilleur.blue_student.entity.AliceEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentBrAction;
import com.licht_meilleur.blue_student.student.StudentForm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class AliceBrCombatGoal extends Goal {
    private final PathfinderMob mob;
    private final AliceEntity alice;

    private LivingEntity target;

    private int orbitDir = 1;
    private int orbitSwitchTicks = 0;
    private double orbitRadius = 7.5;

    private static final double HIGH_ABOVE = 6.0;
    private static final double LOW_ABOVE = 1.2;
    private static final double AGL_MIN = 1.5;
    private static final double AGL_MAX = 10.0;

    private static final double ORBIT_SPEED = 0.35;
    private static final double ALT_SPEED = 0.28;

    private int cdMain = 0;
    private int cdHyper = 0;

    private int hyperSetTicks = 0;
    private int hyperBeamTicks = 0;

    public AliceBrCombatGoal(PathfinderMob mob, AliceEntity alice) {
        this.mob = mob;
        this.alice = alice;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean isBr() {
        return alice.getForm() == StudentForm.BR;
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!isBr()) return false;
        if (alice.isLifeLockedForGoal()) return false;

        target = findTarget();
        if (target != null) mob.setTarget(target);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel)) return false;
        if (!isBr()) return false;
        if (alice.isLifeLockedForGoal()) return false;
        if (target == null || !target.isAlive()) return false;

        double keep = 28.0;
        return mob.distanceToSqr(target) <= keep * keep;
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        orbitSwitchTicks = 0;
        cdMain = 0;
        cdHyper = 0;
        hyperSetTicks = 0;
        hyperBeamTicks = 0;
    }

    @Override
    public void stop() {
        target = null;
        mob.setTarget(null);
        mob.getNavigation().stop();
        hyperSetTicks = 0;
        hyperBeamTicks = 0;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        if (cdMain > 0) cdMain--;
        if (cdHyper > 0) cdHyper--;

        if (target == null || !target.isAlive()) {
            target = findTarget();
            mob.setTarget(target);
            return;
        }

        boolean canSee = mob.getSensing().hasLineOfSight(target);

        alice.requestLookTarget(target, 80, 2);
        mob.getLookControl().setLookAt(target, 90.0f, 90.0f);

        if (hyperSetTicks > 0 || hyperBeamTicks > 0) {
            freezeInPlaceForHyper();
            tickHyperCannon(serverLevel);
            return;
        }

        tickOrbitRandom();
        tickOrbitMoveAndAltitude(serverLevel, canSee);

        double dist = mob.distanceTo(target);
        if (cdHyper <= 0 && canSee && dist <= 18.0) {
            startHyperCannon();
            return;
        }

        if (cdMain <= 0 && canSee) {
            alice.requestBrAction(
                    orbitDir > 0 ? StudentBrAction.RIGHT_MOVE_SHOT : StudentBrAction.LEFT_MOVE_SHOT,
                    6
            );
            alice.queueFire(target, IStudentEntity.FireChannel.MAIN);
            cdMain = 6;
        } else {
            alice.requestBrAction(
                    orbitDir > 0 ? StudentBrAction.RIGHT_MOVE : StudentBrAction.LEFT_MOVE,
                    6
            );
        }
    }

    private void startHyperCannon() {
        hyperSetTicks = 12;
        hyperBeamTicks = 0;

        cdHyper = 20 * 8;
        alice.requestBrAction(StudentBrAction.HYPER_CANNON_SET, hyperSetTicks);
    }

    private void tickHyperCannon(ServerLevel serverLevel) {
        if (hyperSetTicks > 0) {
            alice.requestBrAction(StudentBrAction.HYPER_CANNON_SET, hyperSetTicks);
            hyperSetTicks--;

            if (hyperSetTicks == 0) {
                hyperBeamTicks = 20;
            }
            return;
        }

        if (hyperBeamTicks > 0) {
            alice.requestBrAction(StudentBrAction.HYPER_CANNON, hyperBeamTicks);
            spawnBeamsEveryTick(serverLevel);
            hyperBeamTicks--;
        }
    }

    private void freezeInPlaceForHyper() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setNoGravity(true);
        mob.fallDistance = 0;
    }

    private void spawnOneBeam(ServerLevel serverLevel, int sideSign) {
        Vec3 start = muzzleSideTowardTarget(sideSign);
        Vec3 end = target.getEyePosition();

        BlockHitResult hit = serverLevel.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
        }

        spawnBeamParticles(serverLevel, start, end);
        damageAlongSegment(serverLevel, start, end);
    }

    private void spawnBeamsEveryTick(ServerLevel serverLevel) {
        if (target == null || !target.isAlive()) return;

        spawnOneBeam(serverLevel, -1);
        spawnOneBeam(serverLevel, 1);
    }

    private void spawnBeamParticles(ServerLevel serverLevel, Vec3 start, Vec3 end) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.001) return;

        Vec3 forward = dir.normalize();
        Vec3 step = forward.scale(0.35);
        Vec3 pos = start;
        var rand = serverLevel.getRandom();

        for (double traveled = 0; traveled < length; traveled += 0.35) {
            for (int i = 0; i < 3; i++) {
                double ox = (rand.nextDouble() - 0.5) * 0.25;
                double oy = (rand.nextDouble() - 0.5) * 0.25;
                double oz = (rand.nextDouble() - 0.5) * 0.25;

                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.END_ROD,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        1,
                        0, 0, 0,
                        0
                );

                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        pos.x + ox, pos.y + oy, pos.z + oz,
                        1,
                        0, 0, 0,
                        0
                );
            }

            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                    pos.x, pos.y, pos.z,
                    1,
                    0, 0, 0,
                    0
            );

            pos = pos.add(step);
        }
    }

    private void damageAlongSegment(ServerLevel serverLevel, Vec3 start, Vec3 end) {
        double radius = 0.8;
        AABB box = new AABB(start, end).inflate(radius);

        var entities = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != alice
        );

        for (LivingEntity e : entities) {
            double distSq = distanceSqPointToSegment(
                    e.position().add(0, e.getBbHeight() * 0.5, 0),
                    start, end
            );
            if (distSq <= radius * radius) {
                e.hurtServer(serverLevel, serverLevel.damageSources().magic(), 6.0f);
            }
        }
    }

    private void tickOrbitRandom() {
        if (orbitSwitchTicks > 0) {
            orbitSwitchTicks--;
            return;
        }
        orbitSwitchTicks = 40 + mob.getRandom().nextInt(60);
        orbitDir = mob.getRandom().nextBoolean() ? 1 : -1;
        orbitRadius = 6.5 + mob.getRandom().nextDouble() * 2.5;
    }

    private void tickOrbitMoveAndAltitude(ServerLevel serverLevel, boolean canSee) {
        Vec3 my = mob.position();
        Vec3 tp = target.position();

        Vec3 to = tp.subtract(my);
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1e-6) flat = new Vec3(1, 0, 0);
        Vec3 radial = flat.normalize();

        Vec3 tangent = (orbitDir > 0)
                ? new Vec3(-radial.z, 0, radial.x)
                : new Vec3(radial.z, 0, -radial.x);

        double d = Math.sqrt(flat.lengthSqr());
        double radialPush = (d - orbitRadius) * 0.09;
        Vec3 velXZ = tangent.scale(ORBIT_SPEED).add(radial.scale(radialPush));

        Vec3 forward = velXZ.lengthSqr() < 1e-6 ? mob.getViewVector(1.0f) : velXZ.normalize();
        Vec3 checkPos = my.add(forward.scale(1.6));

        BlockHitResult frontHit = serverLevel.clip(new ClipContext(
                my,
                checkPos,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        boolean frontBlocked = (frontHit.getType() == HitResult.Type.BLOCK);

        if (frontBlocked) {
            orbitDir *= -1;
            orbitRadius += 1.2;
            velXZ = radial.scale(-0.45);
        }

        double desiredY = pickClearanceAltitude(serverLevel, my, tp, canSee);

        double agl = estimateGroundDistance(serverLevel, my);
        if (agl < AGL_MIN) desiredY += (AGL_MIN - agl);
        if (agl > AGL_MAX) desiredY -= Math.min(agl - AGL_MAX, 1.0);

        double dy = desiredY - my.y;
        double velY = clamp(dy * 0.15, -ALT_SPEED, ALT_SPEED);

        if (frontBlocked) velY = Math.max(velY, 0.22);

        mob.getNavigation().stop();
        mob.setNoGravity(true);
        mob.setDeltaMovement(velXZ.x, velY, velXZ.z);
        mob.fallDistance = 0;
    }

    private double pickClearanceAltitude(ServerLevel serverLevel, Vec3 my, Vec3 tp, boolean canSee) {
        double base = tp.y + (canSee ? HIGH_ABOVE : LOW_ABOVE);

        for (int i = 0; i <= 10; i++) {
            double y = base + i * 0.8;
            Vec3 from = new Vec3(my.x, y, my.z);
            Vec3 to = tp.add(0, 1.2, 0);

            BlockHitResult hit = serverLevel.clip(new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mob
            ));
            if (hit.getType() == HitResult.Type.MISS) return y;
        }

        return base + 8.0;
    }

    private double estimateGroundDistance(ServerLevel serverLevel, Vec3 from) {
        Vec3 to = from.add(0, -32, 0);
        var hit = serverLevel.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob
        ));
        if (hit.getType() == HitResult.Type.MISS) return 32.0;
        return from.y - hit.getLocation().y;
    }

    private LivingEntity findTarget() {
        double r = 24.0;
        AABB box = mob.getBoundingBox().inflate(r);
        return mob.level().getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        e -> e.isAlive() && e instanceof Monster
                ).stream()
                .min(Comparator.comparingDouble(mob::distanceToSqr))
                .orElse(null);
    }

    private Vec3 muzzleSideTowardTarget(int sign) {
        Vec3 base = alice.getEyePosition().subtract(0, 0.10, 0);

        Vec3 toT = target.getEyePosition().subtract(base);
        Vec3 forward = toT.lengthSqr() < 1e-6 ? alice.getViewVector(1.0f) : toT.normalize();

        Vec3 right = new Vec3(0, 1, 0).cross(forward);
        if (right.lengthSqr() < 1e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();

        double side = 0.25 * sign;
        double fwd = 0.15;
        return base.add(right.scale(side)).add(forward.scale(fwd));
    }

    private static double distanceSqPointToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double abLenSq = ab.lengthSqr();
        if (abLenSq < 1e-6) return p.distanceToSqr(a);

        double t = p.subtract(a).dot(ab) / abLenSq;
        t = Math.max(0, Math.min(1, t));
        Vec3 proj = a.add(ab.scale(t));
        return p.distanceToSqr(proj);
    }

    private static double clamp(double v, double mn, double mx) {
        return Math.max(mn, Math.min(mx, v));
    }
}