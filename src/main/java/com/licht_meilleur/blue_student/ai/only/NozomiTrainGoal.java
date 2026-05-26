package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HikariEntity;
import net.minecraft.world.entity.LivingEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class NozomiTrainGoal extends Goal {

    private final NozomiEntity nozomi;

    private static final int CHECK_INTERVAL = 10;
    private static final double RANGE = 18.0;
    private static final double FIND_RANGE = 96.0;

    private int next = 0;

    private TrainEntity train = null;

    public NozomiTrainGoal(NozomiEntity nozomi) {
        this.nozomi = nozomi;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (nozomi.level().isClientSide()) return false;
        if (nozomi.isLifeLockedForGoal()) return false;
        if (!nozomi.canUseTrainSkill()) return false;

        LivingEntity target = nozomi.getTarget();
        if (target == null || !target.isAlive()) return false;

        if (nozomi.level() instanceof ServerLevel serverLevel) {
            UUID ownerP = nozomi.getOwnerUuid();
            if (ownerP != null) {
                if (!serverLevel.getEntitiesOfClass(
                        GoGoTrainEntity.class,
                        nozomi.getBoundingBox().inflate(96.0),
                        e -> e.isAlive() && ownerP.equals(e.getOwnerPlayerUuid())
                ).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (nozomi.level().isClientSide()) return false;
        if (nozomi.isLifeLockedForGoal()) return false;
        if (!nozomi.canUseTrainSkill()) return false;

        if (nozomi.level() instanceof ServerLevel serverLevel) {
            UUID ownerP = nozomi.getOwnerUuid();
            if (ownerP != null && existsHikari(serverLevel, ownerP)) return false;
        }
        return true;
    }

    @Override
    public void stop() {
        nozomi.setTrainSkillActive(false);
        if (nozomi.getVehicle() != null) nozomi.stopRiding();
    }

    @Override
    public void tick() {
        if (!(nozomi.level() instanceof ServerLevel serverLevel)) return;
        if (--next > 0) return;
        next = CHECK_INTERVAL;

        UUID ownerP = nozomi.getOwnerUuid();
        if (ownerP == null) return;

        if (existsHikari(serverLevel, ownerP)) {
            discardTrainOnly();
            nozomi.setTrainSkillActive(false);
            return;
        }

        LivingEntity target = findNearestHostile(serverLevel, nozomi.position(), RANGE);
        if (target == null) {
            discardTrainOnly();
            nozomi.setTrainSkillActive(false);
            return;
        }

        if (train == null || !train.isAlive()) {
            train = findTrain(serverLevel, ownerP);
            if (train == null) {
                train = new TrainEntity(ModEntities.TRAIN.get(), serverLevel)
                        .setOwnerPlayerUuid(ownerP);
                train.setPos(nozomi.getX(), nozomi.getY(), nozomi.getZ());
                serverLevel.addFreshEntity(train);
            }
        }

        train.setMode(TrainEntity.TrainMode.SINGLE_CHARGE);
        train.setGunTrainUuid(null);
        train.setClockwise(true);
        train.setTargetUuid(target.getUUID());
        train.setNozomiPassengerUuid(nozomi.getUUID());
        nozomi.setTrainSkillActive(true);

        if (nozomi.getVehicle() != train) {
            nozomi.stopRiding();
            nozomi.startRiding(train);
        }

        nozomi.requestLookTarget(target, 80, 2);
        nozomi.setTrainSkillActive(true);
    }

    private void discardTrainOnly() {
        if (train != null) {
            train.discard();
            train = null;
        }
    }

    private TrainEntity findTrain(ServerLevel serverLevel, UUID ownerP) {
        AABB box = nozomi.getBoundingBox().inflate(FIND_RANGE);
        for (TrainEntity e : serverLevel.getEntitiesOfClass(TrainEntity.class, box, x -> x.isAlive())) {
            if (ownerP.equals(e.getOwnerPlayerUuid())) return e;
        }
        return null;
    }

    private boolean existsHikari(ServerLevel serverLevel, UUID ownerP) {
        AABB box = nozomi.getBoundingBox().inflate(FIND_RANGE);
        for (HikariEntity h : serverLevel.getEntitiesOfClass(HikariEntity.class, box, x -> x.isAlive())) {
            if (ownerP.equals(h.getOwnerUuid())) return true;
        }
        return false;
    }

    private net.minecraft.world.entity.LivingEntity findNearestHostile(ServerLevel serverLevel, Vec3 center, double range) {
        AABB box = new AABB(center, center).inflate(range, 6.0, range);
        Monster best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Monster e : serverLevel.getEntitiesOfClass(Monster.class, box, net.minecraft.world.entity.LivingEntity::isAlive)) {
            double d2 = e.distanceToSqr(center);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }
}