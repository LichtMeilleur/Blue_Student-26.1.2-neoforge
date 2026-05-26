package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.licht_meilleur.blue_student.entity.projectile.HyperCannonEntity;
import com.licht_meilleur.blue_student.entity.projectile.SonicBeamEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private ModEntities() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, BlueStudentMod.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<HyperCannonEntity>> HYPER_CANNON =
            ENTITY_TYPES.register("hyper_cannon", () ->
                    EntityType.Builder.<HyperCannonEntity>of(HyperCannonEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(key("hyper_cannon"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<TrainEntity>> TRAIN =
            ENTITY_TYPES.register("train", () ->
                    EntityType.Builder.<TrainEntity>of(TrainEntity::new, MobCategory.MISC)
                            .sized(1.2f, 1.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("train"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GunTrainEntity>> GUN_TRAIN =
            ENTITY_TYPES.register("gun_train", () ->
                    EntityType.Builder.<GunTrainEntity>of(GunTrainEntity::new, MobCategory.MISC)
                            .sized(1.2f, 1.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("gun_train"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GoGoTrainEntity>> GO_GO_TRAIN =
            ENTITY_TYPES.register("go_go_train", () ->
                    EntityType.Builder.<GoGoTrainEntity>of(GoGoTrainEntity::new, MobCategory.MISC)
                            .sized(1.2f, 1.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("go_go_train"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GoGoGunTrainEntity>> GO_GO_GUN_TRAIN =
            ENTITY_TYPES.register("go_go_gun_train", () ->
                    EntityType.Builder.<GoGoGunTrainEntity>of(GoGoGunTrainEntity::new, MobCategory.MISC)
                            .sized(1.2f, 1.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("go_go_gun_train"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<SonicBeamEntity>> SONIC_BEAM =
            ENTITY_TYPES.register("sonic_beam", () ->
                    EntityType.Builder.<SonicBeamEntity>of(SonicBeamEntity::new, MobCategory.MISC)
                            .sized(0.1f, 0.1f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("sonic_beam"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<GunTrainShellEntity>> GUN_TRAIN_SHELL =
            ENTITY_TYPES.register("gun_train_shell", () ->
                    EntityType.Builder.<GunTrainShellEntity>of(GunTrainShellEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(key("gun_train_shell"))
            );

    private static ResourceKey<EntityType<?>> key(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, BlueStudentMod.id(name));
    }
}