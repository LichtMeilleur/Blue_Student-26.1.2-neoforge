package com.licht_meilleur.blue_student.state;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.StudentId;
import com.licht_meilleur.blue_student.student.StudentPresenceState;
import com.licht_meilleur.blue_student.util.DimensionTransferHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class StudentRespawnManager {

    private StudentRespawnManager() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        tick(event.getServer());
    }

    private static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();

        if (overworld.getGameTime() % 20 != 0) return;

        StudentWorldState state = StudentWorldState.get(server);

        for (StudentId sid : StudentId.values()) {
            if (state.getPresence(sid) != StudentPresenceState.RESPAWNING) continue;
            if (!state.isRespawnReady(sid, overworld)) continue;

            StudentWorldState.StudentData data = state.getData(sid);
            if (data == null) continue;

            BlockPos bed = state.getBed(sid);
            if (bed == null) {
                state.markMissing(sid, overworld);
                continue;
            }

            BlockPos spawn = findSafeRespawnPosNearBed(overworld, bed);

            AbstractStudentEntity spawned =
                    DimensionTransferHelper.spawnPackedStudent(overworld, sid, spawn, 0.0f);

            if (spawned == null) {
                state.markMissing(sid, overworld);
                continue;
            }

            spawned.setHealth(1.0f);
            spawned.setDeltaMovement(Vec3.ZERO);
            spawned.getNavigation().stop();
            spawned.fallDistance = 0;
            spawned.noFallTicks = Math.max(spawned.noFallTicks, 20);

            state.setStudent(
                    sid,
                    spawned.getUUID(),
                    spawned.getOwnerUuid(),
                    overworld,
                    spawn
            );
        }
    }

    private static BlockPos findSafeRespawnPosNearBed(ServerLevel world, BlockPos bedFootPos) {
        BlockPos base = bedFootPos.above();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(base.getX() + dx, base.getY(), base.getZ() + dz);
                    if (isSafeStandPos(world, m)) {
                        return m.immutable();
                    }
                }
            }
        }

        return base;
    }

    private static boolean isSafeStandPos(ServerLevel world, BlockPos pos) {
        BlockPos below = pos.below();

        if (world.getBlockState(below).isAir()) return false;
        if (world.getBlockState(below).getCollisionShape(world, below).isEmpty()) return false;
        if (!world.getFluidState(below).isEmpty()) return false;

        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) return false;
        return world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty();
    }
}