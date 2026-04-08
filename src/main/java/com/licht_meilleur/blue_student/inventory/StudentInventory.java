package com.licht_meilleur.blue_student.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StudentInventory implements Container {
    private final NonNullList<ItemStack> stacks;
    private final Runnable markDirtyCallback;

    private final SimpleContainer equipInv = new SimpleContainer(1);

    public StudentInventory(int size, Runnable markDirtyCallback) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.markDirtyCallback = markDirtyCallback;
    }

    public StudentInventory(Runnable markDirtyCallback) {
        this(9, markDirtyCallback);
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    // いったん保留
    public void writeNbt(Object tag) {
    }

    // いったん保留
    public void readNbt(Object tag) {
    }

    @Override
    public int getContainerSize() {
        return stacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return equipInv.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return stacks.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = stacks.get(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack result = current.split(amount);
        if (!result.isEmpty()) {
            setChanged();
        }

        if (current.isEmpty()) {
            stacks.set(slot, ItemStack.EMPTY);
        }

        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = stacks.get(slot);
        stacks.set(slot, ItemStack.EMPTY);
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        if (markDirtyCallback != null) {
            markDirtyCallback.run();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        stacks.clear();
        equipInv.clearContent();
        setChanged();
    }

    public Container getEquipInv() {
        return equipInv;
    }

    public ItemStack getBrEquipStack() {
        return equipInv.getItem(0);
    }
}