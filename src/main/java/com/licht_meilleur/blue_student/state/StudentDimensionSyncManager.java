package com.licht_meilleur.blue_student.state;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentId;
import com.licht_meilleur.blue_student.student.StudentPresenceState;
import com.licht_meilleur.blue_student.util.DimensionTransferHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class StudentDimensionSyncManager {

    private StudentDimensionSyncManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(StudentDimensionSyncManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) return;

        // 1秒に1回で十分
        if (overworld.getGameTime() % 20 != 0) return;

        StudentWorldState state = StudentWorldState.get(server);

        for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
            for (StudentId sid : StudentId.values()) {
                StudentWorldState.StudentData data = state.getData(sid);
                if (data == null) continue;
                if (data.owner == null || !data.owner.equals(owner.getUUID())) continue;

                if (state.getPresence(sid) == StudentPresenceState.RESPAWNING
                        || state.getPresence(sid) == StudentPresenceState.SLEEPING) {
                    continue;
                }

                // FOLLOW以外まで強制同期するとSECURITY中の生徒も飛んでくるので、
                // packedがある場合だけ復元、それ以外は実体が見つかった時にAIを確認する
                syncOne(server, owner, sid, data, state);
            }
        }
    }

    private static void syncOne(
            MinecraftServer server,
            ServerPlayer owner,
            StudentId sid,
            StudentWorldState.StudentData data,
            StudentWorldState state
    ) {
        ServerLevel ownerLevel = owner.level();

        // すでに記録上同じディメンションなら何もしない
        String ownerDim = dimensionId(ownerLevel);
        if (ownerDim.equals(data.dimension)) {
            return;
        }

        BlockPos spawn = DimensionTransferHelper.findSafeNear(ownerLevel, owner.blockPosition());

        // 1. 記録ディメンションに実体がロード済みなら、同個体優先で転送
        AbstractStudentEntity found = findLoadedStudent(server, data);
        if (found != null && found.isAlive()) {
            if (found.getAiMode() != StudentAiMode.FOLLOW) {
                return;
            }

            if (found.isLifeLockedForGoal()) {
                return;
            }

            boolean ok = found.teleportToWorldForCallback(ownerLevel, spawn, owner.getYRot());
            if (ok) {
                state.setStudent(sid, found.getUUID(), owner.getUUID(), ownerLevel, spawn);
            }
            return;
        }

        // 2. packedがあるなら復元
        if (state.isPacked(sid) || state.getPacked(sid) != null) {
            AbstractStudentEntity spawned =
                    DimensionTransferHelper.spawnPackedStudent(ownerLevel, sid, spawn, owner.getYRot());

            if (spawned != null) {
                spawned.setDeltaMovement(Vec3.ZERO);
                spawned.getNavigation().stop();
                spawned.noFallTicks = Math.max(spawned.noFallTicks, 20);
                state.setStudent(sid, spawned.getUUID(), owner.getUUID(), ownerLevel, spawn);
                return;
            }
        }

        // 3. 見つからない場合はMISSINGにする
        state.markMissing(sid, ownerLevel);
    }

    private static AbstractStudentEntity findLoadedStudent(MinecraftServer server, StudentWorldState.StudentData data) {
        if (data == null || data.uuid == null || data.dimension == null) return null;

        ResourceKey<Level> key = ResourceKey.create(
                Registries.DIMENSION,
                Identifier.parse(data.dimension)
        );

        ServerLevel level = server.getLevel(key);
        if (level == null) return null;

        Entity e = level.getEntity(data.uuid);
        return e instanceof AbstractStudentEntity ase ? ase : null;
    }

    private static String dimensionId(ServerLevel level) {
        String raw = level.dimension().toString();

        int idx = raw.lastIndexOf(" / ");
        if (idx >= 0 && raw.endsWith("]")) {
            return raw.substring(idx + 3, raw.length() - 1);
        }

        return raw;
    }
}