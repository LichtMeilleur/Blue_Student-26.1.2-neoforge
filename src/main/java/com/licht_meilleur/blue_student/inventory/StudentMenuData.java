package com.licht_meilleur.blue_student.inventory;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record StudentMenuData(int entityId) {
    public static final StreamCodec<RegistryFriendlyByteBuf, StudentMenuData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    StudentMenuData::entityId,
                    StudentMenuData::new
            );
}