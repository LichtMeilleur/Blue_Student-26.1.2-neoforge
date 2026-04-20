package com.licht_meilleur.blue_student.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class DebugChat {
    private DebugChat() {}

    public static void near(Entity e, double radius, String msg) {
        if (!(e.level() instanceof ServerLevel sw)) return;

        double r2 = radius * radius;

        for (ServerPlayer p : sw.players()) {
            if (p.distanceToSqr(e) <= r2) {
                p.sendSystemMessage(Component.literal(msg));
            }
        }
    }
}