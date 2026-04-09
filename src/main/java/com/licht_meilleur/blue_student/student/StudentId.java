package com.licht_meilleur.blue_student.student;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.Vec3;

public enum StudentId implements StringRepresentable {
    SHIROKO("shiroko", 35, 20,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/shiroko_face.png"),
            new Vec3(0.0, -0.5, 1.0)),
    HOSHINO("hoshino", 40, 20,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/hoshino_face.png"),
            new Vec3(0.0, -0.80, 1.00)),
    HINA("hina", 40, 20,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/hina_face.png"),
            new Vec3(0.0, -0.8, 1.00)),
    ALICE("alice", 30, 20,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/alice_face.png"),
            new Vec3(0.0, -0.50, 1.00)),
    KISAKI("kisaki", 28, 18,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/kisaki_face.png"),
            new Vec3(0.0, -0.50, 1.00)),
    MARIE("marie", 25, 18,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/marie_face.png"),
            new Vec3(0.0, -0.50, 1.00)),
    HIKARI("hikari", 25, 18,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/hikari_face.png"),
            new Vec3(0.0, -0.50, 1.00)),
    NOZOMI("nozomi", 25, 18,
            new StudentAiMode[]{StudentAiMode.FOLLOW, StudentAiMode.SECURITY},
            BlueStudentMod.id("textures/gui/nozomi_face.png"),
            new Vec3(0.0, -0.50, 1.00));

    private final String key;
    private final int baseMaxHp;
    private final int baseDefense;
    private final StudentAiMode[] allowedAis;
    private final ResourceLocation faceTexture;
    private final Vec3 muzzleOffset;

    StudentId(String key, int baseMaxHp, int baseDefense, StudentAiMode[] allowedAis, ResourceLocation faceTexture, Vec3 muzzleOffset) {
        this.key = key;
        this.baseMaxHp = baseMaxHp;
        this.baseDefense = baseDefense;
        this.allowedAis = allowedAis;
        this.faceTexture = faceTexture;
        this.muzzleOffset = muzzleOffset;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public String asString() {
        return key;
    }

    public int getBaseMaxHp() {
        return baseMaxHp;
    }

    public int getBaseDefense() {
        return baseDefense;
    }

    public StudentAiMode[] getAllowedAis() {
        return allowedAis;
    }

    public ResourceLocation getFaceTexture() {
        return faceTexture;
    }

    public Component getNameText() {
        return Component.translatable("student.blue_student." + key);
    }

    public Component getOnlySkillText() {
        return Component.translatable("skill.blue_student." + key + ".only");
    }

    public Component getWeaponText() {
        return Component.translatable("weapon.blue_student." + key);
    }

    public Vec3 getMuzzleOffset() {
        return muzzleOffset;
    }

    public static StudentId fromKey(String key) {
        for (StudentId id : values()) {
            if (id.key.equals(key)) return id;
        }
        return SHIROKO;
    }

    public boolean hasBrForm() {
        return switch (this) {
            case HOSHINO -> true;
            case ALICE -> true;
            default -> false;
        };
    }
}