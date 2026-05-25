package com.licht_meilleur.blue_student.inventory;

import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentEquipments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;

public class StudentScreenHandler extends AbstractContainerMenu {
    private final Container studentInv;
    private final Container equipInv;

    public final IStudentEntity entity;
    public final int entityId;

    private static final int SLOT_START_X = 48;
    private static final int SLOT_START_Y = 90;
    private static final int SLOT_SIZE = 18;

    private static final int EQUIP_X = 150;
    private static final int EQUIP_Y = 90;

    private static final int EQUIP_BG_X = 150;
    private static final int EQUIP_BG_Y = 90;

    private static final int EQUIP_SLOT_X = EQUIP_BG_X + 10;
    private static final int EQUIP_SLOT_Y = EQUIP_BG_Y + 10;

    private int equipSlotScreenIndex = -1;

    public StudentScreenHandler(int syncId, Inventory playerInv, IStudentEntity entity) {
        this(
                syncId,
                playerInv,
                entity,
                (Container) entity.getStudentInventory(),
                (Container) ((com.licht_meilleur.blue_student.inventory.StudentInventory) entity.getStudentInventory()).getEquipInv()
        );
    }

    public StudentScreenHandler(int syncId, Inventory playerInv, IStudentEntity entity, Container inv9, Container equipInv) {
        super(ModScreenHandlers.STUDENT_MENU.get(), syncId);
        this.entity = entity;
        this.entityId = (entity instanceof Entity e) ? e.getId() : -1;
        this.studentInv = inv9;
        this.equipInv = equipInv;

        this.studentInv.startOpen(playerInv.player);
        this.equipInv.startOpen(playerInv.player);

        int i = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(studentInv, i++, SLOT_START_X + col * SLOT_SIZE, SLOT_START_Y + row * SLOT_SIZE));
            }
        }

        if (entity != null && StudentEquipments.supportsBr(entity.getStudentId())) {
            this.equipSlotScreenIndex = this.slots.size();
            this.addSlot(new Slot(equipInv, 0, EQUIP_SLOT_X, EQUIP_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return StudentEquipments.isBrEquipped(entity.getStudentId(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }

        int hotbarX = 48;
        int hotbarY = 256 - 24;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, hotbarX + col * 18, hotbarY));
        }
    }

    public IStudentEntity resolveEntity(Player player) {
        if (entity != null) return entity;
        Entity raw = player.level().getEntity(entityId);
        return (raw instanceof IStudentEntity se) ? se : null;
    }

    @Override
    public boolean stillValid(Player player) {
        IStudentEntity se = resolveEntity(player);
        if (!(se instanceof Entity e)) return false;
        return e.isAlive() && player.distanceTo(e) < 8.0f;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack original = slot.getItem();
        ItemStack newStack = original.copy();

        final int studentStart = 0;
        final int studentEnd = 9;

        final boolean hasEquip = this.equipSlotScreenIndex >= 0;
        final int equipStart = hasEquip ? this.equipSlotScreenIndex : -1;
        final int equipEnd = hasEquip ? this.equipSlotScreenIndex + 1 : -1;

        final int playerStart = hasEquip ? 10 : 9;
        final int playerEnd = this.slots.size();

        if (index >= studentStart && index < studentEnd) {
            if (!this.moveItemStackTo(original, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (hasEquip && index >= equipStart && index < equipEnd) {
            if (!this.moveItemStackTo(original, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            IStudentEntity se = resolveEntity(player);
            boolean moved = false;

            if (hasEquip && se != null && StudentEquipments.isBrEquipped(se.getStudentId(), original)) {
                moved = this.moveItemStackTo(original, equipStart, equipEnd, false);
            }

            if (!moved) {
                moved = this.moveItemStackTo(original, studentStart, studentEnd, false);
            }

            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, original);
        return newStack;
    }

    public int getEquipSlotIndex() {
        return equipSlotScreenIndex;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.studentInv.stopOpen(player);
        this.equipInv.stopOpen(player);
    }
}