package com.licht_meilleur.blue_student.ai.prediction;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnemyIntentAnalyzer {

    private EnemyIntentAnalyzer() {}

    private static final Map<UUID, TargetState> CACHE = new ConcurrentHashMap<>();

    private static final double CLOSE_EPS = 0.08;
    private static final double BACK_EPS = 0.08;
    private static final double STILL_EPS = 0.045;
    private static final int STILL_TICKS = 6;

    public static EnemyIntentRead analyze(LivingEntity self, LivingEntity target) {

        EnemyIntentRead out = new EnemyIntentRead();

        if (self == null || target == null || !self.isAlive() || !target.isAlive()) {
            return out;
        }

        UUID id = target.getUUID();
        TargetState prev = CACHE.computeIfAbsent(id, k -> new TargetState());

        double dist = self.distanceTo(target);
        Vec3 pos = target.position();
        Vec3 motion = pos.subtract(prev.lastPos);
        double speed = motion.length();

        double deltaDist = prev.init ? (dist - prev.lastDist) : 0.0;

        int stillTicks = speed <= STILL_EPS ? prev.stillTicks + 1 : 0;

        boolean canSee = self.hasLineOfSight(target);

        boolean targetingSelf = false;
        if (target instanceof Mob mob) {
            targetingSelf = mob.getTarget() == self;
        }

        boolean facing = isFacing(target, self);

        boolean closing = deltaDist < -CLOSE_EPS;
        boolean backing = deltaDist > BACK_EPS;
        boolean still = stillTicks >= STILL_TICKS;

        out.distance = dist;
        out.deltaDistance = deltaDist;
        out.targetSpeed = speed;
        out.canSee = canSee;
        out.targetFacingSelf = facing;
        out.targetingSelf = targetingSelf;
        out.hurtTime = target.hurtTime;
        out.stillTicks = stillTicks;

        out.closingIn = closing;
        out.backingOff = backing;
        out.holdingStill = still;

        // -------------------
        // 近接圧
        // -------------------
        double melee = 0;

        if (dist <= 2.2) melee += 0.45;
        else if (dist <= 3.2) melee += 0.30;

        if (closing) melee += 0.25;
        if (speed > 0.18) melee += 0.15;
        if (facing) melee += 0.10;
        if (targetingSelf) melee += 0.10;
        if (still && dist <= 3) melee += 0.10;
        if (target.hurtTime > 0) melee -= 0.12;

        // -------------------
        // 射撃圧
        // -------------------
        double ranged = 0;

        if (dist >= 4 && dist <= 10) ranged += 0.30;
        if (still) ranged += 0.20;
        if (backing) ranged += 0.20;
        if (facing) ranged += 0.15;
        if (targetingSelf) ranged += 0.10;

        if (closing) ranged -= 0.12;
        if (dist <= 3) ranged -= 0.18;

        // -------------------
        // 溜め予兆
        // -------------------
        double charge = 0;

        if (still) charge += 0.35;
        if (facing) charge += 0.20;
        if (targetingSelf) charge += 0.15;
        if (speed < 0.03) charge += 0.15;
        if (target.hurtTime == 0) charge += 0.10;
        if (backing) charge -= 0.10;

        // -------------------
        // 隙
        // -------------------
        double open = 0;

        if (!facing) open += 0.25;
        if (!targetingSelf) open += 0.15;
        if (target.hurtTime > 0) open += 0.30;
        if (backing) open += 0.20;
        if (still && !closing) open += 0.15;
        if (dist <= 2.5 && closing) open -= 0.20;

        out.meleeThreat = clamp(melee);
        out.rangedThreat = clamp(ranged);
        out.chargeThreat = clamp(charge);
        out.opening = clamp(open);

        prev.init = true;
        prev.lastDist = dist;
        prev.lastPos = pos;
        prev.stillTicks = stillTicks;

        return out;
    }

    private static boolean isFacing(LivingEntity src, LivingEntity tgt) {
        Vec3 look = src.getLookAngle();
        Vec3 to = tgt.position().subtract(src.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);

        if (flat.lengthSqr() < 1e-6) return true;

        double dot = look.normalize().dot(flat.normalize());
        return dot > 0.5;
    }

    private static double clamp(double v) {
        if (v < 0) return 0;
        return Math.min(v, 1);
    }

    private static class TargetState {
        boolean init = false;
        double lastDist;
        Vec3 lastPos = Vec3.ZERO;
        int stillTicks;
    }
    public static void clear(net.minecraft.world.entity.LivingEntity target) {
        if (target == null) return;
        CACHE.remove(target.getUUID());
    }

    public static void clearAll() {
        CACHE.clear();
    }
}