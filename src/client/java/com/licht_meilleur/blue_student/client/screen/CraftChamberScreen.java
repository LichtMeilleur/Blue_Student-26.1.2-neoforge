package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.client.network.CraftChamberNetworking;
import com.licht_meilleur.blue_student.craft_chamber.CraftChamberRecipe;
import com.licht_meilleur.blue_student.craft_chamber.CraftChamberRecipes;
import com.licht_meilleur.blue_student.craft_chamber.IngredientStack;
import com.licht_meilleur.blue_student.inventory.CraftChamberScreenHandler;
import com.licht_meilleur.blue_student.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CraftChamberScreen extends AbstractContainerScreen<CraftChamberScreenHandler> {

    private static final Identifier BG = BlueStudentMod.id("textures/gui/craft_chamber.png");
    private static final Identifier BTN_L = BlueStudentMod.id("textures/gui/left_button.png");
    private static final Identifier BTN_R = BlueStudentMod.id("textures/gui/right_button.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;

    private int pageIndex = 0;


    public CraftChamberScreen(CraftChamberScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.inventoryLabelY = 9999;
    }


    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - BG_W) / 2;
        this.topPos = (this.height - BG_H) / 2;

        this.addRenderableWidget(
                Button.builder(Component.empty(), _button -> {
                            int max = CraftChamberRecipes.ALL.size();
                            this.pageIndex = Math.floorMod(this.pageIndex - 1, max);
                        })
                        .bounds(this.leftPos - 15, this.topPos + 128, 20, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.empty(), _button -> {
                            int max = CraftChamberRecipes.ALL.size();
                            this.pageIndex = Math.floorMod(this.pageIndex + 1, max);
                        })
                        .bounds(this.leftPos + 245, this.topPos + 128, 20, 20)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Craft"), _button ->
                                ClientPlayNetworking.send(
                                        new ModPackets.CraftChamberCraftPayload(
                                                this.menu.getPos(),
                                                this.pageIndex
                                        )
                                ))
                        .bounds(this.leftPos + 12, this.topPos + 228, 60, 20)
                        .build()
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, this.leftPos, this.topPos, 0.0F, 0.0F, BG_W, BG_H, BG_W, BG_H);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BTN_L, this.leftPos - 15, this.topPos + 128, 0.0F, 0.0F, 18, 18, 18, 18);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BTN_R, this.leftPos + 245, this.topPos + 128, 0.0F, 0.0F, 18, 18, 18, 18);

        CraftChamberRecipe recipe = CraftChamberRecipes.byIndex(this.pageIndex);

        int cX = this.leftPos + 128;
        int cY = this.topPos + 128;

        int slot12X = cX - 12;
        int slot12Y = cY - 93;

        int slot3X = cX + 70;
        int slot3Y = cY - 8;

        int slot6X = cX - 12;
        int slot6Y = cY + 72;

        int slot9X = cX - 95;
        int slot9Y = cY - 8;

        this.drawSlotIcon(graphics, recipe.slot12(), slot12X, slot12Y);
        this.drawSlotIcon(graphics, recipe.slot3(), slot3X, slot3Y);
        this.drawSlotIcon(graphics, recipe.slot6(), slot6X, slot6Y);
        this.drawSlotIcon(graphics, recipe.slot9(), slot9X, slot9Y);

        int outX = this.leftPos + 122;
        int outY = this.topPos + 125;
        this.drawItem(graphics, recipe.output(), outX, outY);

        int listX = this.leftPos + 200;
        int listY = this.topPos + 150;
        int stepY = 18;

        int i = 0;
        for (IngredientStack cost : recipe.costs()) {
            ItemStack stack = cost.toStack();
            int ix = listX;
            int iy = listY + i * stepY;
            this.drawItem(graphics, stack, ix, iy);
            i++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawSlotIcon(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        this.drawItem(graphics, stack, x, y);
    }

    private void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        /*
         * TODO:
         * GuiGraphicsExtractor 側の item 描画 API 名がまだ未確定。
         * 候補: drawItem / renderItem 系
         * ここは IDE の候補に合わせて 1 行だけ置換する。
         */
        // graphics.drawItem(stack, x, y);
    }
}