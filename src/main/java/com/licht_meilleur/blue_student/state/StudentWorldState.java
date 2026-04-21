package com.licht_meilleur.blue_student.state;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.student.StudentId;
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

    // sid -> full data
    private final Map<String, StudentData> studentById;

    // sid.asString() -> packed data / flag
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
                            .fieldOf("uuid")
                            .forGetter(d -> d.uuid),

                    Codec.STRING.optionalFieldOf("dimension", "minecraft:overworld")
                            .forGetter(d -> d.dimension == null ? "minecraft:overworld" : d.dimension),

                    Codec.LONG.optionalFieldOf("pos")
                            .forGetter(d -> d.pos == null ? Optional.empty() : Optional.of(d.pos.asLong())),

                    Codec.LONG.optionalFieldOf("bed")
                            .forGetter(d -> d.bed == null ? Optional.empty() : Optional.of(d.bed.asLong())),

                    Codec.STRING.optionalFieldOf("form", "normal")
                            .forGetter(d -> d.form == null ? "normal" : d.form)
            ).apply(instance, (owner, uuid, dim, posLong, bedLong, form) ->
                    new StudentData(
                            owner.orElse(null),
                            uuid,
                            dim,
                            posLong.map(BlockPos::of).orElse(null),
                            bedLong.map(BlockPos::of).orElse(null),
                            form
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

    // =====================================================
    // Overworld固定保存（全ディメンション共通DB）
    // =====================================================
    public static StudentWorldState get(ServerLevel anyLevel) {
        MinecraftServer server = anyLevel.getServer();
        return get(server);
    }

    public static StudentWorldState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    // =====================================================
    // API
    // =====================================================
    public boolean hasStudent(StudentId sid) {
        return studentById.containsKey(sid.asString());
    }

    public UUID getStudentUuid(StudentId sid) {
        StudentData d = studentById.get(sid.asString());
        return d == null ? null : d.uuid;
    }

    public StudentData getData(StudentId sid) {
        return studentById.get(sid.asString());
    }

    public void setStudent(StudentId sid, UUID uuid, UUID owner, ServerLevel level, BlockPos pos) {
        StudentData old = studentById.get(sid.asString());

        String form = old != null && old.form != null ? old.form : "normal";
        BlockPos bed = old != null ? old.bed : null;

        studentById.put(
                sid.asString(),
                new StudentData(
                        owner,
                        uuid,
                        level.dimension().toString(),
                        pos,
                        bed,
                        form
                )
        );
        setDirty();
    }

    public void updatePos(StudentId sid, ServerLevel level, BlockPos pos) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.dimension = level.dimension().toString();
        d.pos = pos;
        setDirty();
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

    public void clearStudent(StudentId sid) {
        studentById.remove(sid.asString());
        setDirty();
    }

    public void clearAll() {
        studentById.clear();
        packedNbt.clear();
        packedFlag.clear();
        setDirty();
    }

    public void clearBed(StudentId sid) {
        StudentData d = getData(sid);
        if (d == null) return;
        d.bed = null;
        setDirty();
    }

    public void setPacked(StudentId sid, CompoundTag nbt) {
        packedNbt.put(sid.asString(), nbt.copy());
        setDirty();
    }

    public CompoundTag getPacked(StudentId sid) {
        return packedNbt.get(sid.asString());
    }

    public void clearPacked(StudentId sid) {
        packedNbt.remove(sid.asString());
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
        StudentForm form = (d == null) ? StudentForm.NORMAL : StudentForm.fromKey(d.form);


        return form;
    }
    public void setForm(StudentId sid, StudentForm form) {
        StudentData d = getData(sid);
        if (d == null) return;

        d.form = form.asString();

        setDirty();
    }

    // =====================================================
    // StudentData
    // =====================================================
    public static class StudentData {
        public UUID owner;
        public UUID uuid;
        public String dimension;
        public BlockPos pos;
        public BlockPos bed;
        public String form;

        public StudentData(UUID owner, UUID uuid, String dim, BlockPos pos, BlockPos bed, String form) {
            this.owner = owner;
            this.uuid = uuid;
            this.dimension = dim;
            this.pos = pos;
            this.bed = bed;
            this.form = form == null ? "normal" : form;
        }
    }
}