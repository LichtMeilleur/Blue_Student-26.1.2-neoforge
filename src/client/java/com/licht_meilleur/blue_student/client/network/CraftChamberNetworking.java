package com.licht_meilleur.blue_student.client.network;

import com.licht_meilleur.blue_student.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class CraftChamberNetworking {

    private CraftChamberNetworking() {
    }

    public static void sendCraftRequest(BlockPos pos, int pageIndex) {
        Minecraft.getInstance().getConnection().send(
                new ModPackets.CraftChamberCraftPayload(pos, pageIndex)
        );
    }
}