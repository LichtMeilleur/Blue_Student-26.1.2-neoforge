package com.licht_meilleur.blue_student.client.network;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
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

    public static void handleShotFx(ModPackets.ShotFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 start = payload.start();

        Entity shooter = level.getEntity(payload.shooterEntityId());
        if (shooter instanceof AbstractStudentEntity student) {
            start = student.getClientMuzzleWorldPosOrApprox();
        }

        WeaponSpec.FxType fxType = WeaponSpec.FxType.values()[payload.fxTypeOrdinal()];

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
                    spawnHyperRailShot(level, start, payload.dirs().getFirst(), payload.fxWidth(), payload.travelDist());
                }
            }
        }
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

        for (int i = 0; i < 6; i++) {
            Vec3 pos = start.add(d.scale(0.2 * i));

            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, true, false,
                    pos.x, pos.y, pos.z,
                    d.x * 4.0, d.y * 4.0, d.z * 4.0);

            level.addParticle(ParticleTypes.END_ROD, true, false,
                    pos.x, pos.y, pos.z,
                    d.x * 4.0, d.y * 4.0, d.z * 4.0);
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
        RandomSource random = level.getRandom();

        for (int i = 0; i < 5; i++) {
            Vec3 s = start.add(d.scale(0.45 * i));
            Vec3 vel = d.scale(2.8);

            for (int j = 0; j < 12; j++) {
                double ox = (random.nextDouble() - 0.5) * fxWidth * 0.4;
                double oy = (random.nextDouble() - 0.5) * fxWidth * 0.4;
                double oz = (random.nextDouble() - 0.5) * fxWidth * 0.4;

                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, true, false,
                        s.x + ox, s.y + oy, s.z + oz,
                        vel.x, vel.y, vel.z);

                level.addParticle(ParticleTypes.END_ROD, true, false,
                        s.x + ox, s.y + oy, s.z + oz,
                        vel.x, vel.y, vel.z);
            }
        }
    }
}