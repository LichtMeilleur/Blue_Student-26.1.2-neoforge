package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.licht_meilleur.blue_student.entity.projectile.HyperCannonEntity;
import com.licht_meilleur.blue_student.entity.projectile.SonicBeamEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
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
                FabricEntityTypeBuilder.<HyperCannonEntity>create(MobCategory.MISC, HyperCannonEntity::new)
                        .dimensions(EntityDimensions.fixed(0.1f, 0.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(10)
                        .build(key("hyper_cannon"))
        );

        TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("train"),
                FabricEntityTypeBuilder.<TrainEntity>create(MobCategory.MISC, TrainEntity::new)
                        .dimensions(EntityDimensions.fixed(1.2f, 1.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("train"))
        );

        GUN_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("gun_train"),
                FabricEntityTypeBuilder.<GunTrainEntity>create(MobCategory.MISC, GunTrainEntity::new)
                        .dimensions(EntityDimensions.fixed(1.2f, 1.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("gun_train"))
        );

        GO_GO_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("go_go_train"),
                FabricEntityTypeBuilder.<GoGoTrainEntity>create(MobCategory.MISC, GoGoTrainEntity::new)
                        .dimensions(EntityDimensions.fixed(1.2f, 1.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("go_go_train"))
        );

        GO_GO_GUN_TRAIN = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("go_go_gun_train"),
                FabricEntityTypeBuilder.<GoGoGunTrainEntity>create(MobCategory.MISC, GoGoGunTrainEntity::new)
                        .dimensions(EntityDimensions.fixed(1.2f, 1.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("go_go_gun_train"))
        );

        SONIC_BEAM = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("sonic_beam"),
                FabricEntityTypeBuilder.<SonicBeamEntity>create(MobCategory.MISC, SonicBeamEntity::new)
                        .dimensions(EntityDimensions.fixed(0.1f, 0.1f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("sonic_beam"))
        );

        GUN_TRAIN_SHELL = Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                BlueStudentMod.id("gun_train_shell"),
                FabricEntityTypeBuilder.<GunTrainShellEntity>create(MobCategory.MISC, GunTrainShellEntity::new)
                        .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                        .trackRangeBlocks(64)
                        .trackedUpdateRate(1)
                        .build(key("gun_train_shell"))
        );
    }
}