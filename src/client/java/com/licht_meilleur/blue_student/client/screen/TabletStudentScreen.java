package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.network.ModPackets;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentId;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class TabletStudentScreen extends Screen {

    private final BlockPos tabletPos;
    private final StudentId sid;

    private static final Identifier BG = BlueStudentMod.id("textures/gui/student_card.png");
    private static final Identifier SLOT = BlueStudentMod.id("textures/gui/student_slot.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;
    private static final int SLOT_SIZE = 18;

    private int x0;
    private int y0;

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

    private static final int SKILL_X = 48;
    private static final int SKILL_Y = 170;
    private static final int WEAPON_X = 48;
    private static final int WEAPON_Y = 200;

    private static final int EQUIP_SLOT_X = 150;
    private static final int EQUIP_SLOT_Y = 90;

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
                Button.builder(Component.literal("Back"), b -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new TabletScreen(tabletPos));
                            }
                        })
                        .bounds(x0 + 256 - 10 - 45, y0 + 228, 45, 18)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("Call"), b -> {
                            sendCall();
                            this.onClose();
                        })
                        .bounds(x0 + 10, y0 + 228, 45, 18)
                        .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("CallBack"), b -> {
                            sendCallBack();
                            this.onClose();
                        })
                        .bounds(x0 + 10 + 50, y0 + 228, 70, 18)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.blit(BG, x0, y0, 0, 0, BG_W, BG_H, BG_W, BG_H);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = x0 + STUDENT_SLOT_X + col * SLOT_SIZE;
                int sy = y0 + STUDENT_SLOT_Y + row * SLOT_SIZE;
                guiGraphics.blit(SLOT, sx, sy, 0, 0, 18, 18, 18, 18);
            }
        }

        guiGraphics.blit(sid.getFaceTexture(), x0 + FACE_X, y0 + FACE_Y, 0, 0, 50, 50, 50, 50);

        guiGraphics.drawString(this.font, sid.getNameText(), x0 + NAME_X, y0 + NAME_Y, 0x1A1A1A, false);

        guiGraphics.drawString(
                this.font,
                "HP: ? / " + sid.getBaseMaxHp() + "  DEF: " + sid.getBaseDefense(),
                x0 + HP_X,
                y0 + HP_Y,
                0x1A1A1A,
                false
        );

        StudentAiMode[] ai = sid.getAllowedAis();
        StudentAiMode ai1 = (ai.length >= 1) ? ai[0] : StudentAiMode.FOLLOW;
        StudentAiMode ai2 = (ai.length >= 2) ? ai[1] : StudentAiMode.SECURITY;

        drawScaledText(guiGraphics, ai1.getText().getString(), x0 + AI1_X, y0 + AI1_Y, 0x101010, 1.6f);
        drawScaledText(guiGraphics, ai2.getText().getString(), x0 + AI2_X, y0 + AI2_Y, 0x101010, 1.6f);

        drawScaledText(guiGraphics, "Skill", x0 + 48, y0 + 160, 0x101010, 1.0f);
        guiGraphics.drawString(this.font, sid.getOnlySkillText(), x0 + SKILL_X, y0 + SKILL_Y, 0x1A1A1A, false);

        drawScaledText(guiGraphics, "Weapon", x0 + 48, y0 + 190, 0x101010, 1.0f);
        guiGraphics.drawString(this.font, sid.getWeaponText(), x0 + WEAPON_X, y0 + WEAPON_Y, 0x1A1A1A, false);

        Identifier equipSlotTex = com.licht_meilleur.blue_student.student.StudentEquipments.getBrSlotTexture(sid);
        guiGraphics.blit(equipSlotTex, x0 + EQUIP_SLOT_X, y0 + EQUIP_SLOT_Y, 0, 0, 36, 36, 36, 36);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendCall() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(sid.asString());
        buf.writeBlockPos(tabletPos);
        ClientPlayNetworking.send(ModPackets.CALL_STUDENT, buf);
    }

    private void sendCallBack() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(sid.asString());
        buf.writeBlockPos(tabletPos);
        ClientPlayNetworking.send(ModPackets.CALL_BACK_STUDENT, buf);
    }

    private void drawScaledText(GuiGraphics guiGraphics, String text, int x, int y, int color, float scale) {
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1.0f);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        pose.popPose();
    }
}