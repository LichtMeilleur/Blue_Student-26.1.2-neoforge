package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItemGroups {
    private ModItemGroups() {
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, BlueStudentMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BLUE_STUDENT_GROUP =
            CREATIVE_MODE_TABS.register("blue_student", () ->
                    CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                            .icon(() -> new ItemStack(BlueStudentMod.TABLET_BLOCK_ITEM.get()))
                            .title(Component.translatable("itemGroup.blue_student"))
                            .displayItems((parameters, output) -> {
                                output.accept(BlueStudentMod.TABLET_BLOCK_ITEM.get());
                                output.accept(BlueStudentMod.CRAFT_CHAMBER_ITEM.get());
                                output.accept(BlueStudentMod.TICKET.get());
                                output.accept(BlueStudentMod.HOSHINO_BR_EQUIP_ITEM.get());
                                output.accept(BlueStudentMod.ALICE_BR_EQUIP_ITEM.get());
                            })
                            .build()
            );
}