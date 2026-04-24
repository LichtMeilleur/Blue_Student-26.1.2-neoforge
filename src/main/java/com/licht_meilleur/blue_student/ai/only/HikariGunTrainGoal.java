package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class HikariGunTrainGoal extends Goal {

    private final HikariEntity hikari;

    private static final int CHECK_INTERVAL = 10;
    private static final double RANGE = 18.0;
    private static final double FIND_RANGE = 96.0;

    private int next = 0;
    private GunTrainEntity gun = null;

    public HikariGunTrainGoal(HikariEntity hikari) {
        this.hikari = hikari;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (hikari.level().isClientSide()) return false;
        if (hikari.isLifeLockedForGoal()) return false;
        if (!hikari.canUseGunTrainSkill()) return false;

        LivingEntity target = hikari.getTarget();
        if (target == null || !target.isAlive()) return false;

        if (hikari.level() instanceof ServerLevel sw) {
            UUID ownerP = hikari.getOwnerUuid();
            if (ownerP != null) {
                if (!sw.getEntitiesOfClass(
                        GoGoTrainEntity.class,
                        hikari.getBoundingBox().inflate(96.0),
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
        if (hikari.level().isClientSide()) return false;
        if (hikari.isLifeLockedForGoal()) return false;
        if (!hikari.canUseGunTrainSkill()) return false;

        if (hikari.level() instanceof ServerLevel sw) {
            UUID ownerP = hikari.getOwnerUuid();
            if (ownerP != null && existsNozomi(sw, ownerP)) return false;
        }
        return true;
    }

    @Override
    public void stop() {
        hikari.setGunTrainSkillActive(false);
        if (hikari.isPassenger()) {
            hikari.stopRiding();
        }
    }

    @Override
    public void tick() {
        if (!(hikari.level() instanceof ServerLevel sw)) return;
        if (--next > 0) return;
        next = CHECK_INTERVAL;

        UUID ownerP = hikari.getOwnerUuid();
        if (ownerP == null) return;

        if (existsNozomi(sw, ownerP)) {
            discardGunOnly();
            hikari.setGunTrainSkillActive(false);
            return;
        }

        LivingEntity target = findNearestHostile(sw, hikari.position(), RANGE);
        if (target == null) {
            discardGunOnly();
            hikari.setGunTrainSkillActive(false);
            return;
        }

        if (gun == null || !gun.isAlive()) {
            gun = findGun(sw, ownerP);
            if (gun == null) {
                gun = new GunTrainEntity(ModEntities.GUN_TRAIN, sw);
                gun.setOwnerPlayerUuid(ownerP);
                gun.setPassengerStudentUuid(hikari.getUUID());
                gun.setMergedMode(false);

                Vec3 spawn = computeGunSpawnPos(sw, hikari);
                gun.setPos(spawn.x, spawn.y, spawn.z);
                gun.setYRot(hikari.getYRot());
                gun.setXRot(0.0f);

                sw.addFreshEntity(gun);

                gun.setAnchorPos(gun.position());
            }
        }

        if (hikari.getVehicle() != gun) {
            hikari.stopRiding();
            hikari.startRiding(gun);
        }

        hikari.setGunTrainSkillActive(true);
    }

    private void discardGunOnly() {
        if (gun != null) {
            gun.discard();
            gun = null;
            hikari.startGunTrainCooldown();
        }
    }

    private GunTrainEntity findGun(ServerLevel sw, UUID ownerP) {
        AABB box = hikari.getBoundingBox().inflate(FIND_RANGE);
        for (GunTrainEntity e : sw.getEntitiesOfClass(GunTrainEntity.class, box, x -> x.isAlive())) {
            if (ownerP.equals(e.getOwnerPlayerUuid())) {
                return e;
            }
        }
        return null;
    }

    private boolean existsNozomi(ServerLevel sw, UUID ownerP) {
        AABB box = hikari.getBoundingBox().inflate(FIND_RANGE);
        for (NozomiEntity n : sw.getEntitiesOfClass(NozomiEntity.class, box, x -> x.isAlive())) {
            if (ownerP.equals(n.getOwnerUuid())) {
                return true;
            }
        }
        return false;
    }

    private LivingEntity findNearestHostile(ServerLevel sw, Vec3 center, double range) {
        AABB box = new AABB(center, center).inflate(range, 6.0, range);
        Monster best = null;
        double bestD2 = 1.0E18;

        for (Monster e : sw.getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive)) {
            double d2 = e.distanceToSqr(center);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private Vec3 computeGunSpawnPos(ServerLevel sw, HikariEntity h) {
        Vec3 forward = forwardFromYaw(h.getYRot()).normalize();
        Vec3 base = h.position().add(0, 0.2, 0);

        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();

        Vec3[] candidates = new Vec3[] {
                base.add(forward.scale(1.2)),
                base.add(forward.scale(1.0)).add(right.scale(0.9)),
                base.add(forward.scale(1.0)).add(right.scale(-0.9)),
                base,
                base.add(forward.scale(-0.8))
        };

        for (Vec3 p : candidates) {
            Vec3 grounded = snapToGround(sw, p);
            if (isFree(sw, grounded)) return grounded;
        }

        return snapToGround(sw, base);
    }

    private Vec3 snapToGround(ServerLevel sw, Vec3 p) {
        int x = Mth.floor(p.x);
        int z = Mth.floor(p.z);

        int yTop = sw.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new Vec3(p.x, yTop + 0.5, p.z);
    }

    private boolean isFree(ServerLevel sw, Vec3 p) {
        AABB box = new AABB(p.x - 0.45, p.y, p.z - 0.45, p.x + 0.45, p.y + 1.2, p.z + 0.45);
        return sw.noCollision(box);
    }

    private static Vec3 forwardFromYaw(float yawDeg) {
        float r = yawDeg * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
    }
}