package com.licht_meilleur.blue_student.state;

import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.student.StudentId;
import com.licht_meilleur.blue_student.student.StudentPresenceState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class StudentWorldState extends SavedData {

    private static final String NAME = "blue_student_state";

    private final Map<String, StudentData> studentById;
    private final Map<String, CompoundTag> packedNbt;
    private final Map<String, Boolean> packedFlag;

    public StudentWorldState() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    public StudentWorldState(
            Map<String, StudentData> studentById,
            Map<String, CompoundTag> packedNbt,
            Map<String, Boolean> packedFlag
    ) {
        this.studentById = new HashMap<>(studentById);
        this.packedNbt = new HashMap<>(packedNbt);
        this.packedFlag = new HashMap<>(packedFlag);
    }

    private static final Codec<StudentData> STUDENT_DATA_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .optionalFieldOf("owner")
                            .forGetter(d -> Optional.ofNullable(d.owner)),

                    Codec.STRING.xmap(UUID::fromString, UUID::toString)
                            .optionalFieldOf("uuid")
                            .forGetter(d -> Optional.ofNullable(d.uuid)),

                    Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld")
                            .forGetter(d -> d.dimension == null ? "minecraft:overworld" : d.dimension),

                    Codec.LONG.optionalFieldOf("pos")
                            .forGetter(d -> d.pos == null ? Optional.empty() : Optional.of(d.pos.asLong())),

                    Codec.LONG.optionalFieldOf("bed")
                            .forGetter(d -> d.bed == null ? Optional.empty() : Optional.of(d.bed.asLong())),

                    Codec.STRING.optionalFieldOf("form", "normal")
                            .forGetter(d -> d.form == null ? "normal" : d.form),

                    Codec.STRING.optionalFieldOf("presence", StudentPresenceState.UNSUMMONED.asString())
                            .forGetter(d -> d.presence == null ? StudentPresenceState.UNSUMMONED.asString() : d.presence),

                    Codec.LONG.optionalFieldOf("lastSeenGameTime", 0L)
                            .forGetter(d -> d.lastSeenGameTime),

                    Codec.LONG.optionalFieldOf("respawnReadyGameTime", 0L)
                            .forGetter(d -> d.respawnReadyGameTime)
            ).apply(instance, (owner, uuid, dim, posLong, bedLong, form, presence, lastSeen, respawnReady) ->
                    new StudentData(
                            owner.orElse(null),
                            uuid.orElse(null),
                            dim,
                            posLong.map(BlockPos::of).orElse(null),
                            bedLong.map(BlockPos::of).orElse(null),
                            form,
                            presence,
                            lastSeen,
                            respawnReady
                    )
            )
    );

    private static final Codec<StudentWorldState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.unboundedMap(Codec.STRING, STUDENT_DATA_CODEC)
                            .optionalFieldOf("students", Map.of())
                            .forGetter(s -> s.studentById),

                    Codec.unboundedMap(Codec.STRING, CompoundTag.CODEC)
                            .optionalFieldOf("packedNbt", Map.of())
                            .forGetter(s -> s.packedNbt),

                    Codec.unboundedMap(Codec.STRING, Codec.BOOL)
                            .optionalFieldOf("packedFlags", Map.of())
                            .forGetter(s -> s.packedFlag)
            ).apply(instance, StudentWorldState::new)
    );

    private static final SavedDataType<StudentWorldState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("blue_student", NAME),
            StudentWorldState::new,
            CODEC,
            null
    );

    public static StudentWorldState get(ServerLevel anyLevel) {
        return get(anyLevel.getServer());
    }

    public static StudentWorldState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean hasStudent(StudentId sid) {
        return studentById.containsKey(sid.asString());
    }

    public StudentData getData(StudentId sid) {
        return studentById.get(sid.asString());
    }

    public UUID getStudentUuid(StudentId sid) {
        StudentData d = getData(sid);
        return d == null ? null : d.uuid;
    }

    public void setStudent(StudentId sid, UUID uuid, UUID owner, ServerLevel level, BlockPos pos) {
        StudentData old = getData(sid);

        BlockPos bed = old != null ? old.bed : null;
        String form = old != null && old.form != null ? old.form : "normal";

        studentById.put(
                sid.asString(),
                new StudentData(
                        owner,
                        uuid,
                        dimensionId(level),
                        pos,
                        bed,
                        form,
                        StudentPresenceState.ACTIVE.asString(),
                        level.getGameTime(),
                        0L
                )
        );

        packedFlag.put(sid.asString(), false);
        setDirty();
    }

    public void updatePos(StudentId sid, ServerLevel level, BlockPos pos) {
        StudentData d = getData(sid);
        if (d == null) return;

        dimensionId(level);
        d.pos = pos;
        d.lastSeenGameTime = level.getGameTime();
        d.presence = StudentPresenceState.ACTIVE.asString();

        setDirty();
    }

    public void markMissing(StudentId sid, ServerLevel level) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.presence = StudentPresenceState.MISSING.asString();
        d.lastSeenGameTime = level.getGameTime();

        setDirty();
    }

    public void markPacked(StudentId sid, ServerLevel level) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.presence = StudentPresenceState.PACKED.asString();
        d.lastSeenGameTime = level.getGameTime();
        packedFlag.put(sid.asString(), true);

        setDirty();
    }

    public void markRespawning(StudentId sid, ServerLevel level, long delayTicks) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.presence = StudentPresenceState.RESPAWNING.asString();
        d.lastSeenGameTime = level.getGameTime();
        d.respawnReadyGameTime = level.getGameTime() + Math.max(0L, delayTicks);

        setDirty();
    }

    public void markSleeping(StudentId sid, ServerLevel level) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.presence = StudentPresenceState.SLEEPING.asString();
        d.lastSeenGameTime = level.getGameTime();

        setDirty();
    }

    public StudentPresenceState getPresence(StudentId sid) {
        StudentData d = getData(sid);
        if (d == null) return StudentPresenceState.UNSUMMONED;
        return StudentPresenceState.fromKey(d.presence);
    }

    public boolean isRespawnReady(StudentId sid, ServerLevel level) {
        StudentData d = getData(sid);
        if (d == null) return false;
        return getPresence(sid) == StudentPresenceState.RESPAWNING
                && level.getGameTime() >= d.respawnReadyGameTime;
    }

    public void setBed(StudentId sid, BlockPos bed) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.bed = bed;
        setDirty();
    }

    public BlockPos getBed(StudentId sid) {
        StudentData d = getData(sid);
        return d == null ? null : d.bed;
    }

    public void clearBed(StudentId sid) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.bed = null;
        setDirty();
    }

    public void clearStudent(StudentId sid) {
        studentById.remove(sid.asString());
        packedNbt.remove(sid.asString());
        packedFlag.remove(sid.asString());
        setDirty();
    }

    public void clearAll() {
        studentById.clear();
        packedNbt.clear();
        packedFlag.clear();
        setDirty();
    }

    public void setPacked(StudentId sid, CompoundTag nbt) {
        packedNbt.put(sid.asString(), nbt.copy());
        packedFlag.put(sid.asString(), true);
        setDirty();
    }

    public CompoundTag getPacked(StudentId sid) {
        CompoundTag tag = packedNbt.get(sid.asString());
        return tag == null ? null : tag.copy();
    }

    public void clearPacked(StudentId sid) {
        packedNbt.remove(sid.asString());
        packedFlag.put(sid.asString(), false);
        setDirty();
    }

    public void setPackedFlag(StudentId sid, boolean v) {
        packedFlag.put(sid.asString(), v);
        setDirty();
    }

    public boolean isPacked(StudentId sid) {
        return packedFlag.getOrDefault(sid.asString(), false);
    }

    public StudentForm getForm(StudentId sid) {
        StudentData d = getData(sid);
        return d == null ? StudentForm.NORMAL : StudentForm.fromKey(d.form);
    }

    public void setForm(StudentId sid, StudentForm form) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.form = form.asString();
        setDirty();
    }

    public static class StudentData {
        public UUID owner;
        public UUID uuid;
        public String dimension;
        public BlockPos pos;
        public BlockPos bed;
        public String form;
        public String presence;
        public long lastSeenGameTime;
        public long respawnReadyGameTime;

        public StudentData(
                UUID owner,
                UUID uuid,
                String dimension,
                BlockPos pos,
                BlockPos bed,
                String form,
                String presence,
                long lastSeenGameTime,
                long respawnReadyGameTime
        ) {
            this.owner = owner;
            this.uuid = uuid;
            this.dimension = dimension == null ? "minecraft:overworld" : dimension;
            this.pos = pos;
            this.bed = bed;
            this.form = form == null ? "normal" : form;
            this.presence = presence == null ? StudentPresenceState.UNSUMMONED.asString() : presence;
            this.lastSeenGameTime = lastSeenGameTime;
            this.respawnReadyGameTime = respawnReadyGameTime;
        }
    }
    public Map<String, StudentData> getAllStudentsView() {
        return Map.copyOf(studentById);
    }

    private static String dimensionId(ServerLevel level) {
        String raw = level.dimension().toString();

        // 例: ResourceKey[minecraft:dimension / minecraft:overworld]
        int idx = raw.lastIndexOf(" / ");
        if (idx >= 0 && raw.endsWith("]")) {
            return raw.substring(idx + 3, raw.length() - 1);
        }

        return raw;
    }
}