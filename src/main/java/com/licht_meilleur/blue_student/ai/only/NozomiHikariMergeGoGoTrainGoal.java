package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class NozomiHikariMergeGoGoTrainGoal extends Goal {

    private final NozomiEntity nozomi;

    private static final int CHECK_INTERVAL = 10;
    private static final double RANGE = 18.0;
    private static final double FIND_RANGE = 96.0;

    private int next = 0;

    private GoGoTrainEntity gogo;
    private GoGoGunTrainEntity gogoGun;

    public NozomiHikariMergeGoGoTrainGoal(NozomiEntity nozomi) {
        this.nozomi = nozomi;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!(nozomi.level() instanceof ServerLevel serverLevel)) return false;
        if (nozomi.isLifeLockedForGoal()) return false;
        if (!nozomi.canUseUnisonSkill()) return false;

        UUID owner = nozomi.getOwnerUuid();
        if (owner == null) return false;

        HikariEntity hikari = findHikari(serverLevel, owner);
        if (hikari == null || !hikari.isAlive()) return false;

        LivingEntity target = findNearestHostile(serverLevel, nozomi.position(), RANGE);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(nozomi.level() instanceof ServerLevel serverLevel)) return false;
        if (nozomi.isLifeLockedForGoal()) return false;

        UUID owner = nozomi.getOwnerUuid();
        if (owner == null) return false;

        HikariEntity hikari = findHikari(serverLevel, owner);
        if (hikari == null || !hikari.isAlive()) return false;

        LivingEntity target = findNearestHostile(serverLevel, nozomi.position(), RANGE);
        if (target == null) return false;

        nozomi.startUnisonCooldown();
        return gogo != null && gogo.isAlive();
    }

    @Override
    public void stop() {
        if (!(nozomi.level() instanceof ServerLevel serverLevel)) return;

        UUID owner = nozomi.getOwnerUuid();
        HikariEntity hikari = (owner != null) ? findHikari(serverLevel, owner) : null;

        nozomi.setTrainSkillActive(false);
        if (hikari != null) hikari.setGunTrainSkillActive(false);

        discardMergedEntitiesIfAny();

        if (nozomi.getVehicle() != null) nozomi.stopRiding();

        next = 0;
    }

    @Override
    public void tick() {
        if (!(nozomi.level() instanceof ServerLevel serverLevel)) return;

        if (--next > 0) return;
        next = CHECK_INTERVAL;

        UUID ownerP = nozomi.getOwnerUuid();
        if (ownerP == null) return;

        HikariEntity hikari = findHikari(serverLevel, ownerP);
        if (hikari == null || !hikari.isAlive()) {
            nozomi.setTrainSkillActive(false);
            discardMergedEntitiesIfAny();
            return;
        }

        LivingEntity target = findNearestHostile(serverLevel, nozomi.position(), RANGE);
        if (target == null) {
            hikari.setGunTrainSkillActive(false);
            nozomi.setTrainSkillActive(false);
            discardMergedEntitiesIfAny();
            return;
        }

        hikari.setGunTrainSkillActive(true);
        nozomi.setTrainSkillActive(true);

        if (gogo == null || !gogo.isAlive()) {
            gogo = findGoGoTrain(serverLevel, ownerP);
        }
        if (gogo == null) {
            Vec3 spawn = computeTrainSpawnPos(nozomi);

            gogo = new GoGoTrainEntity(ModEntities.GO_GO_TRAIN, serverLevel)
                    .setOwnerPlayerUuid(ownerP)
                    .setNozomiPassengerUuid(nozomi.getUUID())
                    .setHikariPassengerUuid(hikari.getUUID())
                    .setClockwise(true);

            gogo.setPos(spawn.x, spawn.y, spawn.z);
            gogo.setYRot(nozomi.getYRot());
            gogo.setXRot(0.0f);
            serverLevel.addFreshEntity(gogo);
        }

        if (gogoGun == null || !gogoGun.isAlive()) {
            gogoGun = findGoGoGunTrain(serverLevel, ownerP);
        }
        if (gogoGun == null) {
            Vec3 gunSpawn = computeGunSpawnBehindTrain(gogo);

            gogoGun = new GoGoGunTrainEntity(ModEntities.GO_GO_GUN_TRAIN, serverLevel)
                    .setOwnerPlayerUuid(ownerP)
                    .setTrainUuid(gogo.getUUID())
                    .setPassengerStudentUuid(hikari.getUUID())
                    .setMergedMode(true);

            float yaw = gogo.getYRot();
            gogoGun.setPos(gunSpawn.x, gunSpawn.y, gunSpawn.z);
            gogoGun.setYRot(yaw);
            gogoGun.setXRot(0.0f);
            serverLevel.addFreshEntity(gogoGun);
        }

        gogo.setTargetUuid(target.getUUID());

        followGunToTrain(gogo, gogoGun);

        if (nozomi.getVehicle() != gogo) {
            nozomi.stopRiding();
            nozomi.startRiding(gogo);
        }

        if (hikari.getVehicle() != gogoGun) {
            hikari.stopRiding();
            hikari.startRiding(gogoGun);
        }
    }

    private Vec3 computeTrainSpawnPos(NozomiEntity noz) {
        return noz.position().add(0, 0.2, 0);
    }

    private Vec3 computeGunSpawnBehindTrain(GoGoTrainEntity train) {
        float yaw = train.getYRot();
        Vec3 forward = forwardFromYaw(yaw).normalize();
        Vec3 base = train.position();

        double back = 2.2;
        return base.add(forward.scale(-back));
    }

    private void followGunToTrain(GoGoTrainEntity train, GoGoGunTrainEntity gun) {
        if (train == null || gun == null) return;
        if (!train.isAlive() || !gun.isAlive()) return;

        float yaw = train.getYRot();
        Vec3 forward = forwardFromYaw(yaw).normalize();
        Vec3 base = train.position();

        double back = 2.2;
        double right = 0.0;
        double up = 0.0;

        Vec3 rightV = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 pos = base.add(0, up, 0)
                .add(forward.scale(-back))
                .add(rightV.scale(right));

        gun.setPos(pos.x, pos.y, pos.z);
        gun.setYRot(yaw);
        gun.setXRot(0.0f);
        gun.setDeltaMovement(Vec3.ZERO);
    }

    private LivingEntity findNearestHostile(ServerLevel serverLevel, Vec3 center, double range) {
        AABB box = new AABB(center, center).inflate(range, 6.0, range);

        Monster best = null;
        double bestD2 = Double.MAX_VALUE;

        for (Monster e : serverLevel.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            double d2 = e.distanceToSqr(center);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private static Vec3 forwardFromYaw(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }

    private HikariEntity findHikari(ServerLevel serverLevel, UUID ownerP) {
        AABB box = nozomi.getBoundingBox().inflate(FIND_RANGE);
        for (HikariEntity h : serverLevel.getEntitiesOfClass(HikariEntity.class, box, e -> e.isAlive())) {
            UUID ho = h.getOwnerUuid();
            if (ho != null && ho.equals(ownerP)) return h;
        }
        return null;
    }

    private GoGoTrainEntity findGoGoTrain(ServerLevel serverLevel, UUID ownerP) {
        AABB box = nozomi.getBoundingBox().inflate(FIND_RANGE);
        for (GoGoTrainEntity e : serverLevel.getEntitiesOfClass(GoGoTrainEntity.class, box, x -> x.isAlive())) {
            UUID o = e.getOwnerPlayerUuid();
            if (o != null && o.equals(ownerP)) return e;
        }
        return null;
    }

    private GoGoGunTrainEntity findGoGoGunTrain(ServerLevel serverLevel, UUID ownerP) {
        AABB box = nozomi.getBoundingBox().inflate(FIND_RANGE);
        for (GoGoGunTrainEntity e : serverLevel.getEntitiesOfClass(GoGoGunTrainEntity.class, box, x -> x.isAlive())) {
            UUID o = e.getOwnerPlayerUuid();
            if (o != null && o.equals(ownerP)) return e;
        }
        return null;
    }

    private void discardMergedEntitiesIfAny() {
        if (gogoGun != null) {
            gogoGun.discard();
            gogoGun = null;
        }
        if (gogo != null) {
            gogo.discard();
            gogo = null;
        }
    }
}