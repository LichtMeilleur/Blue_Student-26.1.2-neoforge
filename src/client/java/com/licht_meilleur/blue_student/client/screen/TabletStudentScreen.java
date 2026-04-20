package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentId;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TabletStudentScreen extends Screen {

    private static final Identifier BG = BlueStudentMod.id("textures/gui/student_card.png");
    private static final Identifier SLOT = BlueStudentMod.id("textures/gui/student_slot.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;
    private static final int SLOT_SIZE = 18;

    private static final int FACE_X = 33;
    private static final int FACE_Y = 20;

    private static final int NAME_X = 95;
    private static final int NAME_Y = 28;
    private static final int HP_X = 95;
    private static final int HP_Y = 42;

    private static final int STUDENT_SLOT_X = 48;
    private static final int STUDENT_SLOT_Y = 90;

    private static final int AI1_X = 150;
    private static final int AI1_Y = 178;
    private static final int AI2_X = 140;
    private static final int AI2_Y = 200;

    private static final int SKILL_LABEL_X = 48;
    private static final int SKILL_LABEL_Y = 160;
    private static final int SKILL_X = 48;
    private static final int SKILL_Y = 170;

    private static final int WEAPON_LABEL_X = 48;
    private static final int WEAPON_LABEL_Y = 190;
    private static final int WEAPON_X = 48;
    private static final int WEAPON_Y = 200;

    private static final int EQUIP_SLOT_X = 150;
    private static final int EQUIP_SLOT_Y = 90;

    private final BlockPos tabletPos;
    private final StudentId sid;

    private int x0;
    private int y0;

    public TabletStudentScreen(BlockPos tabletPos, StudentId sid) {
        super(Component.empty());
        this.tabletPos = tabletPos;
        this.sid = sid;
    }

    @Override
    protected void init() {
        super.init();

        this.x0 = (this.width - BG_W) / 2;
        this.y0 = (this.height - BG_H) / 2;

        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), button ->
                                this.minecraft.setScreen(new TabletScreen(this.tabletPos)))
                        .bounds(this.x0 + 256 - 10 - 45, this.y0 + 228, 45, 18)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Call"), button -> {
                            this.sendCall();
                            this.onClose();
                        })
                        .bounds(this.x0 + 10, this.y0 + 228, 45, 18)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("CallBack"), button -> {
                            this.sendCallBack();
                            this.onClose();
                        })
                        .bounds(this.x0 + 60, this.y0 + 228, 70, 18)
                        .build()
        );

        StudentAiMode[] ai = this.sid.getAllowedAis();
        StudentAiMode ai1 = ai.length >= 1 ? ai[0] : StudentAiMode.FOLLOW;
        StudentAiMode ai2 = ai.length >= 2 ? ai[1] : StudentAiMode.SECURITY;

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + NAME_X,
                        this.y0 + NAME_Y,
                        120,
                        10,
                        this.sid.getNameText(),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + HP_X,
                        this.y0 + HP_Y,
                        150,
                        10,
                        Component.literal("HP: ? / " + this.sid.getBaseMaxHp() + "  DEF: " + this.sid.getBaseDefense()),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + AI1_X,
                        this.y0 + AI1_Y,
                        90,
                        10,
                        ai1.getText(),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + AI2_X,
                        this.y0 + AI2_Y,
                        100,
                        10,
                        ai2.getText(),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + SKILL_LABEL_X,
                        this.y0 + SKILL_LABEL_Y,
                        60,
                        10,
                        Component.literal("Skill"),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + SKILL_X,
                        this.y0 + SKILL_Y,
                        140,
                        10,
                        this.sid.getOnlySkillText(),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + WEAPON_LABEL_X,
                        this.y0 + WEAPON_LABEL_Y,
                        70,
                        10,
                        Component.literal("Weapon"),
                        this.font
                )
        );

        this.addRenderableWidget(
                new StringWidget(
                        this.x0 + WEAPON_X,
                        this.y0 + WEAPON_Y,
                        140,
                        10,
                        this.sid.getWeaponText(),
                        this.font
                )
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, this.x0, this.y0, 0.0F, 0.0F, BG_W, BG_H, BG_W, BG_H);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = this.x0 + STUDENT_SLOT_X + col * SLOT_SIZE;
                int sy = this.y0 + STUDENT_SLOT_Y + row * SLOT_SIZE;
                graphics.blit(RenderPipelines.GUI_TEXTURED, SLOT, sx, sy, 0.0F, 0.0F, 18, 18, 18, 18);
            }
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.sid.getFaceTexture(),
                this.x0 + FACE_X,
                this.y0 + FACE_Y,
                0.0F,
                0.0F,
                50,
                50,
                50,
                50
        );

        Identifier equipSlotTex = com.licht_meilleur.blue_student.student.StudentEquipments.getBrSlotTexture(this.sid);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                equipSlotTex,
                this.x0 + EQUIP_SLOT_X,
                this.y0 + EQUIP_SLOT_Y,
                0.0F,
                0.0F,
                36,
                36,
                36,
                36
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendCall() {
        ClientPlayNetworking.send(new ModPackets.CallStudentPayload(
                this.sid.asString(),
                this.tabletPos
        ));
    }

    private void sendCallBack() {
        ClientPlayNetworking.send(new ModPackets.CallBackStudentPayload(
                this.sid.asString(),
                this.tabletPos
        ));
    }
}