package com.licht_meilleur.blue_student.inventory;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.mojang.serialization.DataResult;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class StudentInventory implements Container {
    private static final int DATA_VERSION = 1;

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

    public void writeNbt(CompoundTag tag) {
        tag.putInt("DataVersion", 1);

        ListTag items = new ListTag();



        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) continue;

            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);

            DataResult<Tag> result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
            result.result().ifPresent(t -> entry.put("Stack", t));

            items.add(entry);
        }

        tag.put("Items", items);

        ItemStack equip = equipInv.getItem(0);
        if (!equip.isEmpty()) {
            CompoundTag equipTag = new CompoundTag();

            DataResult<Tag> result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, equip);
            result.result().ifPresent(t -> equipTag.put("Stack", t));

            tag.put("Equip", equipTag);
        }
    }

    public void readNbt(CompoundTag tag) {
        clearContent();



        if (tag.contains("Items")) {
            ListTag items = tag.getList("Items").orElse(new ListTag());


            for (int i = 0; i < items.size(); i++) {
                CompoundTag entry = items.getCompound(i).orElse(null);
                if (entry == null) continue;

                int slot = entry.getInt("Slot").orElse(-1);
                if (slot < 0 || slot >= stacks.size()) continue;

                if (entry.contains("Stack")) {
                    DataResult<ItemStack> result =
                            ItemStack.CODEC.parse(NbtOps.INSTANCE, entry.get("Stack"));

                    result.result().ifPresent(stack -> stacks.set(slot, stack));
                }
            }
        }

        equipInv.setItem(0, ItemStack.EMPTY);

        if (tag.contains("Equip")) {
            CompoundTag equipTag = tag.getCompound("Equip").orElse(null);

            if (equipTag != null && equipTag.contains("Stack")) {
                DataResult<ItemStack> result =
                        ItemStack.CODEC.parse(NbtOps.INSTANCE, equipTag.get("Stack"));

                result.result().ifPresent(stack -> equipInv.setItem(0, stack));
            }
        }

        setChanged();
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
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
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