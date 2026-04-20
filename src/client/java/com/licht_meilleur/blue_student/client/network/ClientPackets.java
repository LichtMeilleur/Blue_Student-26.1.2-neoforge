package com.licht_meilleur.blue_student.client.network;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class ClientPackets {

    private ClientPackets() {
    }

    public static void registerS2C() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.ShotFxPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) {
                    return;
                }

                Vec3 start = payload.start();

                Entity shooter = level.getEntity(payload.shooterEntityId());
                if (shooter instanceof AbstractStudentEntity student) {
                    start = student.getClientMuzzleWorldPosOrApprox();
                }

                WeaponSpec.FxType fxType = WeaponSpec.FxType.values()[payload.fxTypeOrdinal()];
                RandomSource random = level.getRandom();

                for (int i = 0; i < 6; i++) {
                    double sx = start.x + (random.nextDouble() - 0.5) * 0.05;
                    double sy = start.y + (random.nextDouble() - 0.5) * 0.05;
                    double sz = start.z + (random.nextDouble() - 0.5) * 0.05;
                    level.addParticle(ParticleTypes.FLAME, true, false, sx, sy, sz, 0.0, 0.0, 0.0);
                }

                switch (fxType) {
                    case BULLET -> {
                        if (!payload.dirs().isEmpty()) {
                            spawnOneTracer(level, start, payload.dirs().getFirst());
                        }
                    }
                    case SHOTGUN -> {
                        for (Vec3 dir : payload.dirs()) {
                            spawnOneTracer(level, start, dir);
                        }
                    }
                    case RAILGUN -> {
                        if (!payload.dirs().isEmpty()) {
                            spawnRailShot(level, start, payload.dirs().getFirst(), payload.fxWidth(), payload.travelDist());
                        }
                    }
                    case RAILGUN_HYPER -> {
                        if (!payload.dirs().isEmpty()) {
                            spawnHyperRailShot(level, start, payload.dirs().getFirst(), payload.fxWidth() * 1.4f, payload.travelDist());
                        }
                    }
                }
            });
        });
    }

    private static void spawnOneTracer(ClientLevel level, Vec3 start, Vec3 dir) {
        Vec3 d = dir.normalize();
        Vec3 v = d.scale(3.2);

        level.addParticle(ParticleTypes.END_ROD, true, false, start.x, start.y, start.z, v.x, v.y, v.z);
        level.addParticle(ParticleTypes.CRIT, true, false, start.x, start.y, start.z, v.x * 0.6, v.y * 0.6, v.z * 0.6);
        level.addParticle(ParticleTypes.SMOKE, true, false, start.x, start.y, start.z, v.x * 0.15, v.y * 0.15, v.z * 0.15);
    }

    private static void spawnRailShot(ClientLevel level, Vec3 start, Vec3 dir, float fxWidth, float travelDist) {
        Vec3 d = dir.normalize();

        int count = 6;
        double gap = 0.2;
        double speed = 4.0;

        for (int i = 0; i < count; i++) {
            Vec3 pos = start.add(d.scale(gap * i));

            level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    true, false,
                    pos.x, pos.y, pos.z,
                    d.x * speed, d.y * speed, d.z * speed
            );

            level.addParticle(
                    ParticleTypes.END_ROD,
                    true, false,
                    pos.x, pos.y, pos.z,
                    d.x * speed, d.y * speed, d.z * speed
            );
        }

        level.playLocalSound(
                start.x, start.y, start.z,
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                0.9f,
                0.7f,
                false
        );
    }

    private static void spawnHyperRailShot(ClientLevel level, Vec3 start, Vec3 dir, float fxWidth, float travelDist) {
        Vec3 d = dir.normalize();

        int blobs = 5;
        double blobGap = 0.45;

        for (int i = 0; i < blobs; i++) {
            Vec3 s = start.add(d.scale(blobGap * i));
            spawnHyperRailShotBlob(level, s, d, fxWidth, travelDist);
        }

        RandomSource random = level.getRandom();
        for (int i = 0; i < 18; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.2;
            double oz = (random.nextDouble() - 0.5) * 0.2;

            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    true, false,
                    start.x + ox,
                    start.y + 0.05,
                    start.z + oz,
                    0.0,
                    0.08,
                    0.0
            );
        }
    }

    private static void spawnHyperRailShotBlob(ClientLevel level, Vec3 start, Vec3 dir, float fxWidth, float travelDist) {
        Vec3 vel = dir.scale(2.8);

        for (int i = 0; i < 5; i++) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    true, false,
                    start.x, start.y, start.z,
                    vel.x, vel.y, vel.z
            );
        }

        int shellCount = 80;
        double radius = 0.05 + 0.18 * fxWidth;
        RandomSource random = level.getRandom();

        for (int i = 0; i < shellCount; i++) {
            double x = random.nextDouble() * 2.0 - 1.0;
            double y = random.nextDouble() * 2.0 - 1.0;
            double z = random.nextDouble() * 2.0 - 1.0;

            Vec3 off = new Vec3(x, y, z);
            if (off.lengthSqr() < 1.0E-6) {
                continue;
            }

            off = off.normalize().scale(radius * (0.3 + random.nextDouble() * 0.7));

            level.addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    true, false,
                    start.x + off.x,
                    start.y + off.y,
                    start.z + off.z,
                    vel.x, vel.y, vel.z
            );
        }
    }
}