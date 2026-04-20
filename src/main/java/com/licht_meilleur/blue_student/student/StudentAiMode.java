package com.licht_meilleur.blue_student.student;

import net.minecraft.network.chat.Component;

public enum StudentAiMode {
    FOLLOW(0, "aimode.blue_student.follow"),
    SECURITY(1, "aimode.blue_student.security");

    public final int id;
    private final String langKey;

    StudentAiMode(int id, String langKey) {
        this.id = id;
        this.langKey = langKey;
    }

    public Component getText() {
        return Component.translatable(langKey);
    }

    public static StudentAiMode fromId(int id) {
        for (StudentAiMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return FOLLOW;
    }
}