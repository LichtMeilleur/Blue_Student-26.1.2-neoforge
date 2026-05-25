package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.licht_meilleur.blue_student.entity.projectile.HyperCannonEntity;
import com.licht_meilleur.blue_student.entity.projectile.SonicBeamEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private ModEntities() {
    }

    private static boolean REGISTERED = false;

    public static EntityType<HyperCannonEntity> HYPER_CANNON;
    public static EntityType<TrainEntity> TRAIN;
    public static EntityType<GunTrainEntity> GUN_TRAIN;
    public static EntityType<GoGoTrainEntity> GO_GO_TRAIN;
    public static EntityType<GoGoGunTrainEntity> GO_GO_GUN_TRAIN;
    public static EntityType<SonicBeamEntity> SONIC_BEAM;
    public static EntityType<GunTrainShellEntity> GUN_TRAIN_SHELL;

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, BlueStudentMod.id(name));
    }

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        HYPER_CANNON = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("hyper_cannon"),
                EntityType.Builder.<HyperCannonEntity>of(HyperCannonEntity::new, MobCategory.MISC)
                        .sized(0.1f, 0.1f)
                        .clientTrackingRange(64)
                        .updateInterval(10)
                        .build(key("hyper_cannon"))
        );

        TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("train"),
                EntityType.Builder.<TrainEntity>of(TrainEntity::new, MobCategory.MISC)
                        .sized(1.2f, 1.1f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("train"))
        );

        GUN_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("gun_train"),
                EntityType.Builder.<GunTrainEntity>of(GunTrainEntity::new, MobCategory.MISC)
                        .sized(1.2f, 1.1f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("gun_train"))
        );

        GO_GO_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("go_go_train"),
                EntityType.Builder.<GoGoTrainEntity>of(GoGoTrainEntity::new, MobCategory.MISC)
                        .sized(1.2f, 1.1f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("go_go_train"))
        );

        GO_GO_GUN_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("go_go_gun_train"),
                EntityType.Builder.<GoGoGunTrainEntity>of(GoGoGunTrainEntity::new, MobCategory.MISC)
                        .sized(1.2f, 1.1f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("go_go_gun_train"))
        );

        SONIC_BEAM = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("sonic_beam"),
                EntityType.Builder.<SonicBeamEntity>of(SonicBeamEntity::new, MobCategory.MISC)
                        .sized(0.1f, 0.1f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("sonic_beam"))
        );

        GUN_TRAIN_SHELL = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("gun_train_shell"),
                EntityType.Builder.<GunTrainShellEntity>of(GunTrainShellEntity::new, MobCategory.MISC)
                        .sized(0.25f, 0.25f)
                        .clientTrackingRange(64)
                        .updateInterval(1)
                        .build(key("gun_train_shell"))
        );
    }
}