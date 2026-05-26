package com.licht_meilleur.blue_student;

import com.licht_meilleur.blue_student.bed.BedLinkEvents;
import com.licht_meilleur.blue_student.block.CraftChamberBlock;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.block.TabletBlock;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import com.licht_meilleur.blue_student.block.entity.TabletBlockEntity;
import com.licht_meilleur.blue_student.entity.*;
import com.licht_meilleur.blue_student.entity.projectile.StudentBulletEntity;
import com.licht_meilleur.blue_student.loot.ModLoot;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.registry.ModEntities;
import com.licht_meilleur.blue_student.registry.ModItemGroups;
import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import com.licht_meilleur.blue_student.state.StudentDimensionSyncManager;
import com.licht_meilleur.blue_student.state.StudentRespawnManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Mod(BlueStudentMod.MOD_ID)
public class BlueStudentMod {
    public static final String MOD_ID = "blue_student";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MOD_ID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);

    // ===== entities =====

    public static final DeferredHolder<EntityType<?>, EntityType<ShirokoEntity>> SHIROKO =
            ENTITY_TYPES.register("shiroko", () ->
                    EntityType.Builder.of(ShirokoEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("shiroko"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<HoshinoEntity>> HOSHINO =
            ENTITY_TYPES.register("hoshino", () ->
                    EntityType.Builder.of(HoshinoEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("hoshino"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<HinaEntity>> HINA =
            ENTITY_TYPES.register("hina", () ->
                    EntityType.Builder.of(HinaEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("hina"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<KisakiEntity>> KISAKI =
            ENTITY_TYPES.register("kisaki", () ->
                    EntityType.Builder.of(KisakiEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("kisaki"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<AliceEntity>> ALICE =
            ENTITY_TYPES.register("alice", () ->
                    EntityType.Builder.of(AliceEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("alice"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<MarieEntity>> MARIE =
            ENTITY_TYPES.register("marie", () ->
                    EntityType.Builder.of(MarieEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("marie"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<HikariEntity>> HIKARI =
            ENTITY_TYPES.register("hikari", () ->
                    EntityType.Builder.of(HikariEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("hikari"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<NozomiEntity>> NOZOMI =
            ENTITY_TYPES.register("nozomi", () ->
                    EntityType.Builder.of(NozomiEntity::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.95f)
                            .build(entityKey("nozomi"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ShirokoDroneEntity>> SHIROKO_DRONE =
            ENTITY_TYPES.register("shiroko_drone", () ->
                    EntityType.Builder.<ShirokoDroneEntity>of(ShirokoDroneEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.35f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(entityKey("shiroko_drone"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<KisakiDragonEntity>> KISAKI_DRAGON =
            ENTITY_TYPES.register("kisaki_dragon", () ->
                    EntityType.Builder.<KisakiDragonEntity>of(KisakiDragonEntity::new, MobCategory.MISC)
                            .sized(0.6f, 0.35f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build(entityKey("kisaki_dragon"))
            );

    public static final DeferredHolder<EntityType<?>, EntityType<StudentBulletEntity>> STUDENT_BULLET =
            ENTITY_TYPES.register("student_bullet", () ->
                    EntityType.Builder.<StudentBulletEntity>of(StudentBulletEntity::new, MobCategory.MISC)
                            .sized(0.25f, 0.25f)
                            .clientTrackingRange(64)
                            .updateInterval(10)
                            .build(entityKey("student_bullet"))
            );

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, id(name));
    }

    // ===== blocks =====

    public static final DeferredBlock<OnlyBedBlock> ONLY_BED_BLOCK =
            BLOCKS.register("only_bed", () ->
                    new OnlyBedBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(ResourceKey.create(Registries.BLOCK, id("only_bed")))
                                    .strength(1.0f)
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<Block> TABLET_BLOCK =
            BLOCKS.register("tablet", () ->
                    new TabletBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(ResourceKey.create(Registries.BLOCK, id("tablet")))
                                    .strength(1.0f)
                                    .noOcclusion()
                    )
            );

    public static final DeferredBlock<Block> CRAFT_CHAMBER_BLOCK =
            BLOCKS.register("craft_chamber", () ->
                    new CraftChamberBlock(
                            BlockBehaviour.Properties.of()
                                    .setId(ResourceKey.create(Registries.BLOCK, id("craft_chamber")))
                                    .strength(1.0f)
                                    .noOcclusion()
                    )
            );

    // ===== block entities =====

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OnlyBedBlockEntity>> ONLY_BED_BE =
            BLOCK_ENTITY_TYPES.register("only_bed", () ->
                    new BlockEntityType<>(OnlyBedBlockEntity::new, ONLY_BED_BLOCK.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TabletBlockEntity>> TABLET_BE =
            BLOCK_ENTITY_TYPES.register("tablet", () ->
                    new BlockEntityType<>(TabletBlockEntity::new, TABLET_BLOCK.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftChamberBlockEntity>> CRAFT_CHAMBER_BE =
            BLOCK_ENTITY_TYPES.register("craft_chamber", () ->
                    new BlockEntityType<>(CraftChamberBlockEntity::new, CRAFT_CHAMBER_BLOCK.get())
            );

    // ===== items =====

    public static final DeferredItem<Item> TABLET_BLOCK_ITEM =
            ITEMS.register("tablet", () ->
                    new BlockItem(
                            TABLET_BLOCK.get(),
                            new Item.Properties()
                                    .setId(ResourceKey.create(Registries.ITEM, id("tablet")))
                                    .stacksTo(64)
                    )
            );

    public static final DeferredItem<Item> CRAFT_CHAMBER_ITEM =
            ITEMS.register("craft_chamber", () ->
                    new BlockItem(
                            CRAFT_CHAMBER_BLOCK.get(),
                            new Item.Properties()
                                    .setId(ResourceKey.create(Registries.ITEM, id("craft_chamber")))
                                    .stacksTo(64)
                    )
            );

    public static final DeferredItem<Item> HOSHINO_BR_EQUIP_ITEM =
            ITEMS.register("hoshino_br_equip_item", () ->
                    new Item(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id("hoshino_br_equip_item")))
                            .stacksTo(1))
            );

    public static final DeferredItem<Item> ALICE_BR_EQUIP_ITEM =
            ITEMS.register("alice_br_equip_item", () ->
                    new Item(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id("alice_br_equip_item")))
                            .stacksTo(1))
            );

    public static final DeferredItem<Item> TICKET =
            ITEMS.register("ticket", () ->
                    new Item(new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id("ticket")))
                            .stacksTo(64))
            );

    public static Consumer<BlockPos> OPEN_TABLET_SCREEN = null;

    public BlueStudentMod(IEventBus modBus) {
        LOGGER.info("[BlueStudent] NeoForge init start");

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);

        modBus.addListener(this::registerAttributes);
        modBus.addListener(ModPackets::register);

        ModEntities.ENTITY_TYPES.register(modBus);

        ModLoot.init();
        ModScreenHandlers.MENUS.register(modBus);

        NeoForge.EVENT_BUS.addListener(StudentRespawnManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(StudentDimensionSyncManager::onServerTick);
        NeoForge.EVENT_BUS.register(BedLinkEvents.class);
        ModItemGroups.CREATIVE_MODE_TABS.register(modBus);

        LOGGER.info("[BlueStudent] NeoForge init end");
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(SHIROKO.get(), AbstractStudentEntity.createAttributes().build());
        event.put(HOSHINO.get(), AbstractStudentEntity.createAttributes().build());
        event.put(HINA.get(), HinaEntity.createAttributes().build());
        event.put(KISAKI.get(), AbstractStudentEntity.createAttributes().build());
        event.put(ALICE.get(), AbstractStudentEntity.createAttributes().build());
        event.put(MARIE.get(), AbstractStudentEntity.createAttributes().build());
        event.put(HIKARI.get(), AbstractStudentEntity.createAttributes().build());
        event.put(NOZOMI.get(), AbstractStudentEntity.createAttributes().build());
    }
}