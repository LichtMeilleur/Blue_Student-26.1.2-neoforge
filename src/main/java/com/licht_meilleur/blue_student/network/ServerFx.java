package com.licht_meilleur.blue_student.network;

import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerFx {
    private ServerFx() {
    }

    public static void sendShotFx(ServerLevel level,
                                  int shooterEntityId,
                                  Vec3 start,
                                  WeaponSpec.FxType fxType,
                                  float fxWidth,
                                  Vec3[] dirs,
                                  float travelDist) {

        java.util.List<Vec3> list = dirs == null
                ? java.util.List.of()
                : java.util.Arrays.asList(dirs);

        ModPackets.ShotFxPayload payload = new ModPackets.ShotFxPayload(
                shooterEntityId,
                start,
                fxType.ordinal(),
                fxWidth,
                travelDist,
                list
        );

        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    public static void sendShotFx(ServerLevel level,
                                  int shooterEntityId,
                                  Vec3 start,
                                  WeaponSpec.FxType fxType,
                                  float fxWidth,
                                  Vec3 dir,
                                  float travelDist) {
        sendShotFx(
                level,
                shooterEntityId,
                start,
                fxType,
                fxWidth,
                new Vec3[]{dir},
                travelDist
        );
    }
}