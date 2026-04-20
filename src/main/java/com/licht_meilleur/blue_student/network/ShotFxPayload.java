package com.licht_meilleur.blue_student.network;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ShotFxPayload(
        int shooterEntityId,
        Vec3 start,
        int fxTypeOrdinal,
        float fxWidth,
        float travelDist,
        Vec3[] dirs
) implements CustomPacketPayload {

    public static final Identifier ID = BlueStudentMod.id("s2c_shot_fx");
    public static final Type<ShotFxPayload> TYPE = new Type<>(ID);

    private static final StreamCodec<RegistryFriendlyByteBuf, Vec3> VEC3_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.DOUBLE, Vec3::x,
                    ByteBufCodecs.DOUBLE, Vec3::y,
                    ByteBufCodecs.DOUBLE, Vec3::z,
                    Vec3::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ShotFxPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, ShotFxPayload::shooterEntityId,
                    VEC3_CODEC, ShotFxPayload::start,
                    ByteBufCodecs.INT, ShotFxPayload::fxTypeOrdinal,
                    ByteBufCodecs.FLOAT, ShotFxPayload::fxWidth,
                    ByteBufCodecs.FLOAT, ShotFxPayload::travelDist,
                    VEC3_CODEC.apply(ByteBufCodecs.list()).map(
                            list -> list.toArray(new Vec3[0]),
                            arr -> List.of(arr)
                    ),
                    ShotFxPayload::dirs,
                    ShotFxPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}