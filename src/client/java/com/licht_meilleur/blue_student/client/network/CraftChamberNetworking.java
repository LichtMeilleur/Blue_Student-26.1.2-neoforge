package com.licht_meilleur.blue_student.client.network;

import com.licht_meilleur.blue_student.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

public final class CraftChamberNetworking {

    private CraftChamberNetworking() {
    }

    public static void sendCraftRequest(BlockPos pos, int pageIndex) {
        ClientPlayNetworking.send(new ModPackets.CraftChamberCraftPayload(pos, pageIndex));
    }
}