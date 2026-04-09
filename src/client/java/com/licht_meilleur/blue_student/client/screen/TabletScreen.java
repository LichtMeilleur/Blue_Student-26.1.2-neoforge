package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class TabletScreen extends Screen {

    private final BlockPos tabletPos;

    private static final Identifier BG = BlueStudentMod.id("textures/gui/tablet_screen.png");
    private static final Identifier EMPTY_FACE = BlueStudentMod.id("textures/gui/empty_face.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;

    private int x0;
    private int y0;

    // 10枠（2x5）: 実生徒 + 空き
    private final @Nullable StudentId[] slots = new StudentId[10];

    public TabletScreen(BlockPos tabletPos) {
        super(Component.empty());
        this.tabletPos = tabletPos;

        slots[0] = StudentId.SHIROKO;
        slots[1] = StudentId.HOSHINO;
        slots[2] = StudentId.HINA;
        slots[3] = StudentId.KISAKI;
        slots[4] = StudentId.ALICE;
        slots[5] = StudentId.MARIE;
        slots[6] = StudentId.HIKARI;
        slots[7] = StudentId.NOZOMI;
    }

    public static void open(BlockPos tabletPos) {
        Minecraft.getInstance().setScreen(new TabletScreen(tabletPos));
    }

    @Override
    protected void init() {
        super.init();

        this.x0 = (this.width - BG_W) / 2;
        this.y0 = (this.height - BG_H) / 2;

        int startX = x0 + 25;
        int startY = y0 + 60;
        int cell = 45;

        int i = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int bx = startX + col * cell;
                int by = startY + row * cell;
                final int index = i++;
                this.addRenderableWidget(new FaceButton(bx, by, index));
            }
        }

        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), b -> this.onClose())
                        .bounds(x0 + (BG_W / 2) - 30, y0 + 220, 60, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.blit(BG, x0, y0, 0, 0, BG_W, BG_H, BG_W, BG_H);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class FaceButton extends AbstractButton {
        private final int index;

        public FaceButton(int x, int y, int index) {
            super(x, y, 32, 32, Component.empty());
            this.index = index;
        }

        @Override
        public void onPress() {
            StudentId id = slots[index];
            if (id == null) {
                return;
            }
            if (TabletScreen.this.minecraft != null) {
                TabletScreen.this.minecraft.setScreen(new TabletStudentScreen(tabletPos, id));
            }
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            StudentId id = slots[index];
            Identifier tex = (id != null) ? id.getFaceTexture() : EMPTY_FACE;

            guiGraphics.blit(tex, this.getX(), this.getY(), 0, 0, 32, 32, 32, 32);

            if (this.isHovered()) {
                guiGraphics.renderOutline(this.getX(), this.getY(), this.width, this.height, 0x80FFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}