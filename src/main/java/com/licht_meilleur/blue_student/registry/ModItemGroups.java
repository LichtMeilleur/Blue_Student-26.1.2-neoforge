package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
    public static CreativeModeTab BLUE_STUDENT_GROUP;

    private ModItemGroups() {
    }

    public static void register() {
        BLUE_STUDENT_GROUP = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                BlueStudentMod.id("blue_student"),
                new CreativeModeTab.Builder(CreativeModeTab.Row.TOP, 0)
                        .icon(() -> new ItemStack(BlueStudentMod.TABLET_BLOCK_ITEM))
                        .title(Component.translatable("itemGroup.blue_student"))
                        .displayItems((parameters, output) -> {
                            output.accept(BlueStudentMod.TABLET_BLOCK_ITEM);
                            output.accept(BlueStudentMod.CRAFT_CHAMBER_ITEM);
                            output.accept(BlueStudentMod.TICKET);
                            output.accept(BlueStudentMod.HOSHINO_BR_EQUIP_ITEM);
                            output.accept(BlueStudentMod.ALICE_BR_EQUIP_ITEM);
                        })
                        .build()
        );
    }
}