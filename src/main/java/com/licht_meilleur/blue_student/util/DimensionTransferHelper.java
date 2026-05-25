package com.licht_meilleur.blue_student.util;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class DimensionTransferHelper {

    private DimensionTransferHelper() {
    }

    @Nullable
    public static AbstractStudentEntity transferStudent(
            AbstractStudentEntity entity,
            ServerLevel dest,
            BlockPos pos,
            float yaw
    ) {
        if (!(entity.level() instanceof ServerLevel src)) {
            return null;
        }

        if (src == dest) {
            entity.teleportTo(
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5
            );
            entity.setYRot(yaw);
            entity.setXRot(entity.getXRot());
            entity.setDeltaMovement(Vec3.ZERO);
            entity.getNavigation().stop();
            entity.fallDistance = 0;
            entity.noFallTicks = Math.max(entity.noFallTicks, 20);
            return entity;
        }

        entity.stopRiding();
        entity.ejectPassengers();
        entity.getNavigation().stop();
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;

        AbstractStudentEntity moved = createStudent(entity.getStudentId(), dest);
        if (moved == null) {
            return null;
        }

        moved.restoreFrom(entity);

        moved.setPos(
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5
        );
        moved.setYRot(yaw);
        moved.setXRot(entity.getXRot());

        entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        dest.addFreshEntity(moved);

        moved.setDeltaMovement(Vec3.ZERO);
        moved.getNavigation().stop();
        moved.fallDistance = 0;
        moved.noFallTicks = Math.max(moved.noFallTicks, 20);

        return moved;
    }

    public static BlockPos findSafeNear(ServerLevel world, BlockPos base) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int r = 0; r <= 2; r++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                        if (isSafeStandPos(world, m)) {
                            return m.immutable();
                        }
                    }
                }
            }
        }

        return base.above();
    }

    public static void packStudent(AbstractStudentEntity entity, ServerLevel currentWorld) {
        if (entity.level().isClientSide()) return;
        if (entity.isPackedForDimTransfer()) return;

        entity.setPackedForDimTransfer(true);

        MinecraftServer server = currentWorld.getServer();
        StudentWorldState st = StudentWorldState.get(server);

        CompoundTag full = new CompoundTag();
        entity.saveStudentDataToTagForTransfer(full);

        st.setPacked(entity.getStudentId(), full);
        st.setPackedFlag(entity.getStudentId(), true);

        entity.discard();
    }

    @Nullable
    public static AbstractStudentEntity spawnPackedStudent(
            ServerLevel dest,
            StudentId sid,
            BlockPos spawnPos,
            float yaw
    ) {
        MinecraftServer server = dest.getServer();
        StudentWorldState st = StudentWorldState.get(server);

        CompoundTag packed = st.getPacked(sid);
        if (packed == null) return null;

        AbstractStudentEntity ase = createStudent(sid, dest);
        if (ase == null) return null;

        ase.loadStudentDataFromTagForTransfer(packed);

        ase.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        ase.setYRot(yaw);
        ase.setXRot(0.0f);

        ase.setDeltaMovement(Vec3.ZERO);
        ase.fallDistance = 0;
        ase.noFallTicks = Math.max(ase.noFallTicks, 20);

        boolean ok = dest.addFreshEntity(ase);
        if (!ok) {
            return null;
        }

        st.setStudent(sid, ase.getUUID(), ase.getOwnerUuid(), dest, spawnPos);
        st.setPackedFlag(sid, false);
        st.clearPacked(sid);

        return ase;
    }

    @Nullable
    public static AbstractStudentEntity createStudent(StudentId id, ServerLevel dest) {
        Entity raw = switch (id) {
            case SHIROKO -> BlueStudentMod.SHIROKO.get().create(dest, EntitySpawnReason.LOAD);
            case HOSHINO -> BlueStudentMod.HOSHINO.get().create(dest, EntitySpawnReason.LOAD);
            case HINA -> BlueStudentMod.HINA.get().create(dest, EntitySpawnReason.LOAD);
            case ALICE -> BlueStudentMod.ALICE.get().create(dest, EntitySpawnReason.LOAD);
            case KISAKI -> BlueStudentMod.KISAKI.get().create(dest, EntitySpawnReason.LOAD);
            case MARIE -> BlueStudentMod.MARIE.get().create(dest, EntitySpawnReason.LOAD);
            case HIKARI -> BlueStudentMod.HIKARI.get().create(dest, EntitySpawnReason.LOAD);
            case NOZOMI -> BlueStudentMod.NOZOMI.get().create(dest, EntitySpawnReason.LOAD);
        };

        return raw instanceof AbstractStudentEntity ase ? ase : null;
    }

    private static boolean isSafeStandPos(ServerLevel world, BlockPos pos) {
        BlockPos below = pos.below();

        if (world.getBlockState(below).isAir()) return false;
        if (world.getBlockState(below).getCollisionShape(world, below).isEmpty()) return false;
        if (!world.getFluidState(below).isEmpty()) return false;

        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) return false;
        if (!world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()) return false;

        return true;
    }
}