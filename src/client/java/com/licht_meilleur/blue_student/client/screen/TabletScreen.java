package com.licht_meilleur.blue_student.client.screen;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class TabletScreen extends Screen {

    private static final Identifier BG = BlueStudentMod.id("textures/gui/tablet_screen.png");
    private static final Identifier EMPTY_FACE = BlueStudentMod.id("textures/gui/empty_face.png");

    private static final int BG_W = 256;
    private static final int BG_H = 256;

    private final BlockPos tabletPos;
    private final @Nullable StudentId[] slots = new StudentId[10];

    private int x0;
    private int y0;

    public TabletScreen(BlockPos tabletPos) {
        super(Component.empty());
        this.tabletPos = tabletPos;

        this.slots[0] = StudentId.SHIROKO;
        this.slots[1] = StudentId.HOSHINO;
        this.slots[2] = StudentId.HINA;
        this.slots[3] = StudentId.KISAKI;
        this.slots[4] = StudentId.ALICE;
        this.slots[5] = StudentId.MARIE;
        this.slots[6] = StudentId.HIKARI;
        this.slots[7] = StudentId.NOZOMI;
    }

    public static void open(BlockPos tabletPos) {
        Minecraft.getInstance().setScreen(new TabletScreen(tabletPos));
    }

    @Override
    protected void init() {
        super.init();

        this.x0 = (this.width - BG_W) / 2;
        this.y0 = (this.height - BG_H) / 2;

        int startX = this.x0 + 25;
        int startY = this.y0 + 60;
        int cell = 45;

        int i = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 5; col++) {
                int bx = startX + col * cell;
                int by = startY + row * cell;
                int index = i++;
                this.addRenderableWidget(new FaceButton(bx, by, index));
            }
        }

        this.addRenderableWidget(
                Button.builder(Component.literal("Back"), button -> this.onClose())
                        .bounds(this.x0 + (BG_W / 2) - 30, this.y0 + 220, 60, 20)
                        .build()
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, this.x0, this.y0, 0.0F, 0.0F, BG_W, BG_H, BG_W, BG_H);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private class FaceButton extends AbstractButton {
        private final int index;

        protected FaceButton(int x, int y, int index) {
            super(x, y, 32, 32, Component.empty());
            this.index = index;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            StudentId id = slots[this.index];
            if (id == null) {
                return;
            }

            TabletScreen.this.minecraft.setScreen(new TabletStudentScreen(tabletPos, id));
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            StudentId id = slots[this.index];
            Identifier tex = id != null ? id.getFaceTexture() : EMPTY_FACE;

            graphics.blit(RenderPipelines.GUI_TEXTURED, tex, this.getX(), this.getY(), 0.0F, 0.0F, 32, 32, 32, 32);

            if (this.isHovered()) {
                int x = this.getX();
                int y = this.getY();
                int w = this.width;
                int h = this.height;
                int c = 0x80FFFFFF;

                graphics.fill(x, y, x + w, y + 1, c);
                graphics.fill(x, y + h - 1, x + w, y + h, c);
                graphics.fill(x, y, x + 1, y + h, c);
                graphics.fill(x + w - 1, y, x + w, y + h, c);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }
}