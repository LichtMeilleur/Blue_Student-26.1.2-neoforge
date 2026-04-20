package com.licht_meilleur.blue_student.craft_chamber;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record IngredientStack(Item item, int count) {
    public ItemStack toStack() {
        return new ItemStack(item, count);
    }
}