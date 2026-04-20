package com.licht_meilleur.blue_student.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record CraftChamberMenuData(BlockPos pos) {

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftChamberMenuData> STREAM_CODEC =
            new StreamCodec<>() {

                @Override
                public CraftChamberMenuData decode(RegistryFriendlyByteBuf buf) {
                    return new CraftChamberMenuData(buf.readBlockPos());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, CraftChamberMenuData value) {
                    buf.writeBlockPos(value.pos());
                }
            };
}