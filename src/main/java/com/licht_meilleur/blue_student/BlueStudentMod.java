package com.licht_meilleur.blue_student;

import com.licht_meilleur.blue_student.bed.BedLinkEvents;
import com.licht_meilleur.blue_student.block.CraftChamberBlock;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.block.TabletBlock;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import com.licht_meilleur.blue_student.block.entity.TabletBlockEntity;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.entity.AliceEntity;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import com.licht_meilleur.blue_student.entity.HinaEntity;
import com.licht_meilleur.blue_student.entity.HoshinoEntity;
import com.licht_meilleur.blue_student.entity.KisakiDragonEntity;
import com.licht_meilleur.blue_student.entity.KisakiEntity;
import com.licht_meilleur.blue_student.entity.MarieEntity;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import com.licht_meilleur.blue_student.entity.ShirokoDroneEntity;
import com.licht_meilleur.blue_student.entity.ShirokoEntity;
import com.licht_meilleur.blue_student.entity.projectile.StudentBulletEntity;
import com.licht_meilleur.blue_student.loot.ModLoot;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.registry.ModEntities;
import com.licht_meilleur.blue_student.registry.ModItemGroups;
import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import com.licht_meilleur.blue_student.state.StudentDimensionSyncManager;
import com.licht_meilleur.blue_student.state.StudentRespawnManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class BlueStudentMod implements ModInitializer {
    public static final String MOD_ID = "blue_student";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // ===== student entities =====
    public static final EntityType<ShirokoEntity> SHIROKO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("shiroko"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, ShirokoEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("shiroko"))
    );

    public static final EntityType<HoshinoEntity> HOSHINO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("hoshino"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, HoshinoEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("hoshino"))
    );

    public static final EntityType<HinaEntity> HINA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("hina"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, HinaEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("hina"))
    );

    public static final EntityType<KisakiEntity> KISAKI = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("kisaki"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, KisakiEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("kisaki"))
    );

    public static final EntityType<AliceEntity> ALICE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("alice"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, AliceEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("alice"))
    );

    public static final EntityType<MarieEntity> MARIE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("marie"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, MarieEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("marie"))
    );

    public static final EntityType<HikariEntity> HIKARI = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("hikari"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, HikariEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("hikari"))
    );

    public static final EntityType<NozomiEntity> NOZOMI = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("nozomi"),
            FabricEntityTypeBuilder.create(MobCategory.CREATURE, NozomiEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build(entityKey("nozomi"))
    );

    // ===== misc entities =====
    public static final EntityType<ShirokoDroneEntity> SHIROKO_DRONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("shiroko_drone"),
            FabricEntityTypeBuilder.<ShirokoDroneEntity>create(MobCategory.MISC, ShirokoDroneEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.35f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build(entityKey("shiroko_drone"))
    );

    public static final EntityType<KisakiDragonEntity> KISAKI_DRAGON = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("kisaki_dragon"),
            FabricEntityTypeBuilder.<KisakiDragonEntity>create(MobCategory.MISC, KisakiDragonEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.35f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(1)
                    .build(entityKey("kisaki_dragon"))
    );

    public static final EntityType<StudentBulletEntity> STUDENT_BULLET = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            id("student_bullet"),
            FabricEntityTypeBuilder.<StudentBulletEntity>create(MobCategory.MISC, StudentBulletEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(64)
                    .trackedUpdateRate(10)
                    .build(entityKey("student_bullet"))
    );

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id(name));
    }

    // ===== blocks =====
    public static final OnlyBedBlock ONLY_BED_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            id("only_bed"),
            new OnlyBedBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(Registries.BLOCK, id("only_bed")))
                            .strength(1.0f)
                            .noOcclusion()
            )
    );

    public static final Block TABLET_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            id("tablet"),
            new TabletBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(Registries.BLOCK, id("tablet")))
                            .strength(1.0f)
                            .noOcclusion()
            )
    );

    public static final Block CRAFT_CHAMBER_BLOCK = Registry.register(
            BuiltInRegistries.BLOCK,
            id("craft_chamber"),
            new CraftChamberBlock(
                    BlockBehaviour.Properties.of()
                            .setId(ResourceKey.create(Registries.BLOCK, id("craft_chamber")))
                            .strength(1.0f)
                            .noOcclusion()
            )
    );
//BlockEntityType
    public static final BlockEntityType<OnlyBedBlockEntity> ONLY_BED_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("only_bed"),
            FabricBlockEntityTypeBuilder.<OnlyBedBlockEntity>create(OnlyBedBlockEntity::new, ONLY_BED_BLOCK).build()
    );

    public static final BlockEntityType<TabletBlockEntity> TABLET_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("tablet"),
            FabricBlockEntityTypeBuilder.<TabletBlockEntity>create(TabletBlockEntity::new, TABLET_BLOCK).build()
    );

    public static final BlockEntityType<CraftChamberBlockEntity> CRAFT_CHAMBER_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("craft_chamber"),
            FabricBlockEntityTypeBuilder.<CraftChamberBlockEntity>create(CraftChamberBlockEntity::new, CRAFT_CHAMBER_BLOCK).build()
    );


    // ===== block items =====
    public static final Item TABLET_BLOCK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id("tablet"),
            new BlockItem(
                    TABLET_BLOCK,
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id("tablet")))
                            .stacksTo(64)
            )
    );

    public static final Item CRAFT_CHAMBER_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id("craft_chamber"),
            new BlockItem(
                    CRAFT_CHAMBER_BLOCK,
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id("craft_chamber")))
                            .stacksTo(64)
            )
    );

    // ===== normal items =====
    public static final Item HOSHINO_BR_EQUIP_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id("hoshino_br_equip_item"),
            new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id("hoshino_br_equip_item")))
                    .stacksTo(1))
    );

    public static final Item ALICE_BR_EQUIP_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            id("alice_br_equip_item"),
            new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id("alice_br_equip_item")))
                    .stacksTo(1))
    );

    public static final Item TICKET = Registry.register(
            BuiltInRegistries.ITEM,
            id("ticket"),
            new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id("ticket")))
                    .stacksTo(64))
    );

    public static Consumer<BlockPos> OPEN_TABLET_SCREEN = null;

    @Override
    public void onInitialize() {
        LOGGER.info("[BlueStudent] onInitialize start");

        ModEntities.register();
        ModLoot.init();
        StudentRespawnManager.register();
        StudentDimensionSyncManager.register();

        FabricDefaultAttributeRegistry.register(SHIROKO, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HOSHINO, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HINA, HinaEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KISAKI, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALICE, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(MARIE, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(HIKARI, AbstractStudentEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(NOZOMI, AbstractStudentEntity.createAttributes());

        ModScreenHandlers.register();

        ModPackets.registerPayloads();
        ModPackets.registerC2S();

        BedLinkEvents.register();
        ModItemGroups.register();

        LOGGER.info("[BlueStudent] onInitialize end");
    }
}