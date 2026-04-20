package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.MarieEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.EnumSet;
import java.util.UUID;

public class MarieBuffGoal extends Goal {

    private final AbstractStudentEntity marie;
    private final IStudentEntity student;

    private static final double RANGE = 14.0;
    private static final int CHECK_INTERVAL = 20;

    private static final int BUFF_TTL_TICKS = 60;

    private static final int ABSORPTION_AMP = 0;
    private static final int REGEN_AMP = 0;

    private int nextCheck = 0;

    private UUID currentTargetPlayer = null;

    public MarieBuffGoal(AbstractStudentEntity marie, IStudentEntity student) {
        this.marie = marie;
        this.student = student;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return !marie.level().isClientSide() && !marie.isLifeLockedForGoal();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }



    @Override
    public void tick() {
        if (marie.level().isClientSide()) return;
        if (--nextCheck > 0) return;
        nextCheck = CHECK_INTERVAL;

        if (!(marie.level() instanceof ServerLevel serverLevel)) return;

        UUID owner = student.getOwnerUuid();
        ServerPlayer ownerPlayer = null;
        if (owner != null) {
            ownerPlayer = resolvePlayer(serverLevel, owner);
            if (!isValidTarget(ownerPlayer)) ownerPlayer = null;
        }

        ServerPlayer cur = resolvePlayer(serverLevel, currentTargetPlayer);

        if (ownerPlayer != null) {
            if (currentTargetPlayer == null || !ownerPlayer.getUUID().equals(currentTargetPlayer)) {
                currentTargetPlayer = ownerPlayer.getUUID();
                onTargetChanged(serverLevel, ownerPlayer);
            }
            applyAndRefresh(serverLevel, ownerPlayer);
            return;
        }

        if (isValidTarget(cur)) {
            applyAndRefresh(serverLevel, cur);
            return;
        }

        ServerPlayer best = findNearestPlayerInRange(serverLevel);
        if (best != null) {
            if (currentTargetPlayer == null || !best.getUUID().equals(currentTargetPlayer)) {
                currentTargetPlayer = best.getUUID();
                onTargetChanged(serverLevel, best);
            }
            applyAndRefresh(serverLevel, best);
        } else {
            currentTargetPlayer = null;
        }
    }

    private boolean isValidTarget(ServerPlayer p) {
        if (p == null) return false;
        if (!p.isAlive()) return false;

        double r2 = RANGE * RANGE;
        return marie.distanceToSqr(p) <= r2;
    }

    private ServerPlayer findNearestPlayerInRange(ServerLevel serverLevel) {
        ServerPlayer best = null;
        double bestD2 = Double.MAX_VALUE;

        AABB box = marie.getBoundingBox().inflate(RANGE);
        for (ServerPlayer sp : serverLevel.players()) {
            if (!box.contains(sp.position())) continue;
            if (!isValidTarget(sp)) continue;
            double d2 = marie.distanceToSqr(sp);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = sp;
            }
        }
        return best;
    }

    private ServerPlayer resolvePlayer(ServerLevel serverLevel, UUID uuid) {
        if (uuid == null) return null;

        for (ServerPlayer sp : serverLevel.players()) {
            if (uuid.equals(sp.getUUID())) return sp;
        }
        return null;
    }

    private void applyAndRefresh(ServerLevel serverLevel, ServerPlayer target) {
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, BUFF_TTL_TICKS, ABSORPTION_AMP, true, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, BUFF_TTL_TICKS, REGEN_AMP, true, true, true));

        spawnTargetParticles(serverLevel, target);
    }

    private void onTargetChanged(ServerLevel serverLevel, ServerPlayer newTarget) {
        if (marie instanceof MarieEntity me) {
            me.requestBuff();
        }

        spawnWindBurstFromMarie(serverLevel);
    }

    private void spawnTargetParticles(ServerLevel serverLevel, ServerPlayer target) {
        Vec3 p = target.position().add(0, target.getBbHeight() + 0.2, 0);
        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                p.x, p.y, p.z,
                6,
                0.25, 0.15, 0.25,
                0.01
        );
    }

    private void spawnWindBurstFromMarie(ServerLevel serverLevel) {
        Vec3 origin = marie.position().add(0, marie.getEyeHeight() - 0.1, 0);

        float yawRad = marie.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
        Vec3 driftSide = new Vec3(-forward.z, 0, forward.x).normalize().scale(-1.0);

        int n = 26;
        double sideSpeed = 0.16;
        double upSpeed = 0.07;
        double spreadVel = 0.10;

        var rand = serverLevel.getRandom();

        for (int i = 0; i < n; i++) {
            double ox = (rand.nextDouble() - 0.5) * 0.25;
            double oy = (rand.nextDouble()) * 0.25;
            double oz = (rand.nextDouble() - 0.5) * 0.25;

            Vec3 spawnPos = origin.add(ox, oy, oz);

            double rx = (rand.nextDouble() - 0.5);
            double ry = (rand.nextDouble() - 0.2) * 0.35;
            double rz = (rand.nextDouble() - 0.5);
            Vec3 radial = new Vec3(rx, ry, rz).normalize();

            Vec3 vel = driftSide.scale(sideSpeed)
                    .add(0, upSpeed, 0)
                    .add(radial.scale(spreadVel));

            serverLevel.sendParticles(
                    ParticleTypes.END_ROD,
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    0,
                    vel.x, vel.y, vel.z,
                    1.0
            );
        }

        serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                origin.x, origin.y + 0.15, origin.z,
                18,
                0.35, 0.25, 0.35,
                0.01
        );
    }
}