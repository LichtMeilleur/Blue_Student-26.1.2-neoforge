package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.inventory.StudentScreenHandler;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentId;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

public class StudentScreen extends AbstractContainerScreen<StudentScreenHandler> {

    private static final Identifier BG = BlueStudentMod.id("textures/gui/student_card.png");
    private static final Identifier SLOT = BlueStudentMod.id("textures/gui/student_slot.png");
    private static final Identifier ARROW = BlueStudentMod.id("textures/gui/selector_arrow.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;
    private static final int SLOT_SIZE = 18;

    private static final int STUDENT_SLOT_X = 48;
    private static final int STUDENT_SLOT_Y = 90;

    private static final int HOTBAR_X = 48;
    private static final int HOTBAR_Y = 256 - 24;

    private static final int AI_FOLLOW_X = 150;
    private static final int AI_FOLLOW_Y = 178;
    private static final int AI_SEC_X = 140;
    private static final int AI_SEC_Y = 200;

    private static final int ARROW_FOLLOW_X = 200;
    private static final int ARROW_FOLLOW_Y = 170;
    private static final int ARROW_SEC_X = 200;
    private static final int ARROW_SEC_Y = 185;

    private static final int EQUIP_BG_X = 150;
    private static final int EQUIP_BG_Y = 90;
    private static final int EQUIP_BG_SIZE = 36;

    public StudentScreen(StudentScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, BG_W, BG_H);
        this.titleLabelX = 9999;
        this.titleLabelY = 9999;
        this.inventoryLabelX = 9999;
        this.inventoryLabelY = 9999;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - BG_W) / 2;
        this.topPos = (this.height - BG_H) / 2;

        IStudentEntity se = this.menu.entity;
        StudentId sid = se != null ? se.getStudentId() : StudentId.SHIROKO;

        this.addRenderableWidget(
                Button.builder(Component.empty(), button -> this.sendModeByIndex(0))
                        .bounds(this.leftPos + AI_FOLLOW_X - 10, this.topPos + AI_FOLLOW_Y - 8, 90, 22)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.empty(), button -> this.sendModeByIndex(1))
                        .bounds(this.leftPos + AI_SEC_X - 10, this.topPos + AI_SEC_Y - 8, 110, 22)
                        .build()
        );

        this.addRenderableWidget(new StringWidget(
                this.leftPos + 95,
                this.topPos + 28,
                120,
                10,
                Component.translatable("student.blue_student." + sid.asString()),
                this.font
        ));

        if (se instanceof Entity entity && entity instanceof LivingEntity living) {
            this.addRenderableWidget(new StringWidget(
                    this.leftPos + 95,
                    this.topPos + 42,
                    150,
                    10,
                    Component.literal("HP: " + (int) living.getHealth() + " / " + (int) living.getMaxHealth()),
                    this.font
            ));
        }

        StudentAiMode[] aiList = sid.getAllowedAis();
        StudentAiMode ai1 = aiList.length >= 1 ? aiList[0] : StudentAiMode.FOLLOW;
        StudentAiMode ai2 = aiList.length >= 2 ? aiList[1] : StudentAiMode.SECURITY;

        this.addRenderableWidget(new StringWidget(
                this.leftPos + AI_FOLLOW_X,
                this.topPos + AI_FOLLOW_Y,
                80,
                10,
                ai1.getText(),
                this.font
        ));

        this.addRenderableWidget(new StringWidget(
                this.leftPos + AI_SEC_X,
                this.topPos + AI_SEC_Y,
                100,
                10,
                ai2.getText(),
                this.font
        ));

        this.addRenderableWidget(new StringWidget(
                this.leftPos + 48,
                this.topPos + 160,
                60,
                10,
                Component.literal("Skill"),
                this.font
        ));

        this.addRenderableWidget(new StringWidget(
                this.leftPos + 48,
                this.topPos + 170,
                140,
                10,
                sid.getOnlySkillText(),
                this.font
        ));

        this.addRenderableWidget(new StringWidget(
                this.leftPos + 48,
                this.topPos + 190,
                70,
                10,
                Component.literal("Weapon"),
                this.font
        ));

        this.addRenderableWidget(new StringWidget(
                this.leftPos + 48,
                this.topPos + 200,
                140,
                10,
                sid.getWeaponText(),
                this.font
        ));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, x, y, 0.0F, 0.0F, BG_W, BG_H, BG_W, BG_H);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x + STUDENT_SLOT_X + col * SLOT_SIZE;
                int sy = y + STUDENT_SLOT_Y + row * SLOT_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT, sx, sy, 0.0F, 0.0F, 18, 18, 18, 18);
            }
        }

        for (int col = 0; col < 9; col++) {
            int sx = x + HOTBAR_X + col * 18;
            int sy = y + HOTBAR_Y;
            graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT, sx, sy, 0.0F, 0.0F, 18, 18, 18, 18);
        }

        IStudentEntity se = this.menu.entity;
        StudentId sid = se != null ? se.getStudentId() : StudentId.SHIROKO;

        Identifier equipSlotTex = com.licht_meilleur.blue_student.student.StudentEquipments.getBrSlotTexture(sid);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                equipSlotTex,
                x + EQUIP_BG_X,
                y + EQUIP_BG_Y,
                0.0F,
                0.0F,
                EQUIP_BG_SIZE,
                EQUIP_BG_SIZE,
                EQUIP_BG_SIZE,
                EQUIP_BG_SIZE
        );

        StudentAiMode[] aiList = sid.getAllowedAis();
        StudentAiMode ai1 = aiList.length >= 1 ? aiList[0] : StudentAiMode.FOLLOW;

        StudentAiMode cur = se != null ? se.getAiMode() : ai1;
        boolean onFirst = cur == ai1;

        int ax = x + (onFirst ? ARROW_FOLLOW_X : ARROW_SEC_X);
        int ay = y + (onFirst ? ARROW_FOLLOW_Y : ARROW_SEC_Y);
        graphics.blit(RenderPipelines.GUI_TEXTURED, ARROW, ax, ay, 0.0F, 0.0F, 16, 16, 16, 16);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendMode(int modeId) {
        if (this.menu.entity == null) {
            return;
        }

        if (!(this.menu.entity instanceof Entity entity)) {
            return;
        }

        ClientPlayNetworking.send(new ModPackets.SetAiModePayload(entity.getId(), modeId));
    }

    private void sendModeByIndex(int index) {
        if (this.menu.entity == null) {
            return;
        }

        StudentId sid = this.menu.entity.getStudentId();
        StudentAiMode[] aiList = sid.getAllowedAis();
        if (index < 0 || index >= aiList.length) {
            return;
        }

        StudentAiMode mode = aiList[index];
        this.sendMode(mode.id);
    }
    private boolean isInsideFullGui(double mouseX, double mouseY) {
        return mouseX >= this.leftPos
                && mouseY >= this.topPos
                && mouseX < this.leftPos + BG_W
                && mouseY < this.topPos + BG_H;
    }
    @Override
    protected boolean hasClickedOutside(double mx, double my, int xo, int yo) {
        return mx < this.leftPos
                || my < this.topPos
                || mx >= this.leftPos + BG_W
                || my >= this.topPos + BG_H;
    }



}