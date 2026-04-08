package com.licht_meilleur.blue_student.inventory;

import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class CraftChamberScreenHandler extends AbstractContainerMenu {

    private final BlockPos pos;
    private final CraftChamberBlockEntity be; // client側はnull許容

    public CraftChamberScreenHandler(int syncId, Inventory inv, CraftChamberBlockEntity be, BlockPos pos) {
        super(ModScreenHandlers.CRAFT_CHAMBER_MENU, syncId);
        this.be = be;
        this.pos = pos;
    }

    public BlockPos getPos() {
        return pos;
    }

    public CraftChamberBlockEntity getBlockEntity() {
        return be;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}