package com.licht_meilleur.blue_student.bed;

import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BedLinkManager {

    private static final Map<UUID, StudentId> LINKING = new ConcurrentHashMap<>();

    // owner -> (student -> footPos)
    private static final Map<UUID, Map<StudentId, BlockPos>> BEDS = new ConcurrentHashMap<>();

    public static void setLinking(UUID playerUuid, StudentId id) {
        LINKING.put(playerUuid, id);
    }

    public static StudentId getLinking(UUID playerUuid) {
        return LINKING.get(playerUuid);
    }

    public static void clearLinking(UUID playerUuid) {
        LINKING.remove(playerUuid);
    }

    public static BlockPos getBedPos(UUID playerUuid, StudentId id) {
        Map<StudentId, BlockPos> map = BEDS.get(playerUuid);
        return map == null ? null : map.get(id);
    }

    public static void setBedPos(UUID playerUuid, StudentId id, BlockPos footPos) {
        BEDS.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>()).put(id, footPos);
    }

    public static void clearBedPos(UUID playerUuid, StudentId id) {
        Map<StudentId, BlockPos> map = BEDS.get(playerUuid);
        if (map != null) {
            map.remove(id);
        }
    }

    public static void setBedPosAndPersist(ServerLevel serverLevel, UUID playerUuid, StudentId id, BlockPos footPos) {
        // メモリにも入れる
        setBedPos(playerUuid, id, footPos);

        // Overworld 固定DBに永続保存
        StudentWorldState.get(serverLevel.getServer()).setBed(id, footPos);
    }
}