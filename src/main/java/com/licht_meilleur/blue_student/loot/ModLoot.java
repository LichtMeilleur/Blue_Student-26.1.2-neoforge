package com.licht_meilleur.blue_student.loot;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class ModLoot {
    private ModLoot() {
    }

    private static final Identifier DUNGEON =
            Identifier.fromNamespaceAndPath("minecraft", "chests/simple_dungeon");
    private static final Identifier MINESHAFT =
            Identifier.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft");
    private static final Identifier STRONGHOLD =
            Identifier.fromNamespaceAndPath("minecraft", "chests/stronghold_corridor");

    public static void init() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String id = key.toString();

            if (id.equals(DUNGEON.toString())
                    || id.equals(MINESHAFT.toString())
                    || id.equals(STRONGHOLD.toString())) {

                LootPool pool = LootPool.lootPool()
                        .setRolls(UniformGenerator.between(0.0f, 1.0f))
                        .add(LootItem.lootTableItem(BlueStudentMod.TICKET).setWeight(3))
                        .build();

                tableBuilder.pool(pool);
            }
        });

        BlueStudentMod.LOGGER.info("[BlueStudent] ModLoot init");
    }
}