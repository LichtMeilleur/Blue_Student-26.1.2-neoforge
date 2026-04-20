package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.KisakiDragonEntity;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

public class KisakiBuffGoal extends Goal {

    private final AbstractStudentEntity kisaki;
    private final IStudentEntity student;

    private static final double RANGE = 14.0;
    private static final int CHECK_INTERVAL = 20;

    private static final double ADD_MAX_HP = 4.0;
    private static final double ADD_ARMOR = 0.0;
    private static final float HEAL_ON_APPLY = 2.0f;

    private static final int BUFF_TTL_TICKS = 60;

    private int nextCheck = 0;

    private StudentId currentTargetId = null;

    public KisakiBuffGoal(AbstractStudentEntity kisaki, IStudentEntity student) {
        this.kisaki = kisaki;
        this.student = student;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return !kisaki.level().isClientSide() && !kisaki.isLifeLockedForGoal();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (kisaki.level().isClientSide()) return;
        if (--nextCheck > 0) return;
        nextCheck = CHECK_INTERVAL;

        if (!(kisaki.level() instanceof ServerLevel serverLevel)) return;

        AbstractStudentEntity cur = resolveStudentById(serverLevel, currentTargetId);
        if (isValidTarget(cur, null)) {
            UUID owner = student.getOwnerUuid();

            if (owner != null && owner.equals(cur.getOwnerUuid())) {
                applyAndRefresh(cur);
                return;
            }

            if (owner != null) {
                AbstractStudentEntity ownerBest = findBestInRange(serverLevel, owner);
                if (ownerBest != null) {
                    StudentId prev = currentTargetId;

                    cur.applyKisakiSupportBuff(false, 0, 0, 0);

                    currentTargetId = ownerBest.getStudentId();
                    applyAndRefresh(ownerBest);

                    if (prev == null || prev != currentTargetId) {
                        spawnBuffBurst(serverLevel, ownerBest);

                        if (kisaki instanceof com.licht_meilleur.blue_student.entity.KisakiEntity ke) {
                            ke.requestBuff();
                        }

                        var dragon = new KisakiDragonEntity(
                                BlueStudentMod.KISAKI_DRAGON,
                                serverLevel
                        ).setOwnerAndTarget(kisaki.getUUID(), ownerBest.getUUID());

                        Vec3 spawn = kisaki.position().add(0, kisaki.getBbHeight() * 0.6, 0);
                        dragon.setPos(spawn.x, spawn.y, spawn.z);
                        serverLevel.addFreshEntity(dragon);
                    }

                    return;
                }
            }

            applyAndRefresh(cur);
            return;
        }

        if (cur != null) cur.applyKisakiSupportBuff(false, 0, 0, 0);
        currentTargetId = null;

        UUID owner = student.getOwnerUuid();
        AbstractStudentEntity best = null;

        if (owner != null) {
            best = findBestInRange(serverLevel, owner);
        }

        if (best == null) {
            best = findBestInRange(serverLevel, null);
        }

        if (best != null) {
            StudentId prev = currentTargetId;

            currentTargetId = best.getStudentId();
            applyAndRefresh(best);

            if (prev == null || prev != currentTargetId) {
                spawnBuffBurst(serverLevel, best);

                if (kisaki instanceof com.licht_meilleur.blue_student.entity.KisakiEntity ke) {
                    ke.requestBuff();
                }

                var dragon = new KisakiDragonEntity(
                        BlueStudentMod.KISAKI_DRAGON,
                        serverLevel
                ).setOwnerAndTarget(kisaki.getUUID(), best.getUUID());

                Vec3 spawn = kisaki.position().add(0, kisaki.getBbHeight() * 0.6, 0);
                dragon.setPos(spawn.x, spawn.y, spawn.z);

                serverLevel.addFreshEntity(dragon);
            }
        }
    }

    private AbstractStudentEntity findBestInRange(ServerLevel serverLevel, UUID ownerOrNull) {
        AbstractStudentEntity best = null;
        double bestD2 = Double.MAX_VALUE;

        AABB box = kisaki.getBoundingBox().inflate(RANGE);
        for (AbstractStudentEntity e : serverLevel.getEntitiesOfClass(AbstractStudentEntity.class, box, x -> true)) {
            if (!isValidTarget(e, ownerOrNull)) continue;
            double d2 = kisaki.distanceToSqr(e);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    private boolean isValidTarget(AbstractStudentEntity e, UUID ownerOrNull) {
        if (e == null) return false;
        if (!e.isAlive()) return false;
        if (e == kisaki) return false;
        if (e.isLifeLockedForGoal()) return false;

        double r2 = RANGE * RANGE;
        if (kisaki.distanceToSqr(e) > r2) return false;

        if (ownerOrNull != null) {
            if (e.getOwnerUuid() == null) return false;
            if (!ownerOrNull.equals(e.getOwnerUuid())) return false;
        }

        return true;
    }

    private void applyAndRefresh(AbstractStudentEntity target) {
        target.applyKisakiSupportBuff(true, ADD_ARMOR, ADD_MAX_HP, HEAL_ON_APPLY);
        target.setKisakiSupportTicks(BUFF_TTL_TICKS);

        spawnBuffParticles((ServerLevel) kisaki.level(), target);
    }

    private void spawnBuffParticles(ServerLevel serverLevel, AbstractStudentEntity target) {
        Vec3 p = target.position().add(0, target.getBbHeight() + 0.25, 0);

        serverLevel.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                p.x, p.y, p.z,
                6,
                0.25, 0.15, 0.25,
                0.01
        );
    }

    private void spawnBuffBurst(ServerLevel serverLevel, AbstractStudentEntity target) {
        Vec3 p = target.position().add(0, target.getBbHeight() + 0.35, 0);

        serverLevel.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                p.x, p.y, p.z,
                18,
                0.35, 0.25, 0.35,
                0.02
        );
    }

    private AbstractStudentEntity resolveStudentById(ServerLevel serverLevel, StudentId id) {
        if (id == null) return null;
        var st = StudentWorldState.get(serverLevel);
        UUID uuid = st.getStudentUuid(id);
        if (uuid == null) return null;

        var e = serverLevel.getEntity(uuid);
        return (e instanceof AbstractStudentEntity ase) ? ase : null;
    }
}