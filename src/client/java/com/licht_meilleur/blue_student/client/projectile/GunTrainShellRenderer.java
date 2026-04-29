package com.licht_meilleur.blue_student.client.projectile;

import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;




public class GunTrainShellRenderer extends EntityRenderer<GunTrainShellEntity, GunTrainShellRenderState> {


    private final ItemModelResolver itemModelResolver;


    public GunTrainShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;

        this.itemModelResolver = ctx.getItemModelResolver(); // ★ここ
    }



    @Override
    public @NonNull GunTrainShellRenderState createRenderState() {
        return new GunTrainShellRenderState();
    }

    @Override
    public void extractRenderState(GunTrainShellEntity entity,
                                   GunTrainShellRenderState state,
                                   float partialTick) {

        super.extractRenderState(entity, state, partialTick);

        var v = entity.getDeltaMovement();

        if (v.lengthSqr() > 1e-6) {
            state.shellYaw = (float)(Math.toDegrees(Math.atan2(v.z, v.x)) - 90f);

            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            state.shellPitch = (float)(-Math.toDegrees(Math.atan2(v.y, h)));
        }

        // ★これが重要（Itemモデル化）
        this.itemModelResolver.updateForNonLiving(
                state.item,
                new ItemStack(Items.FIREWORK_ROCKET),
                ItemDisplayContext.GROUND,
                entity
        );
    }

    @Override
    public void submit(GunTrainShellRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector collector,
                       CameraRenderState camera) {

        poseStack.pushPose();

        // 向き
        poseStack.mulPose(Axis.YP.rotationDegrees(state.shellYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.shellPitch));

        // サイズ
        poseStack.scale(0.8f, 0.8f, 0.8f);

        // ★描画（これが正解）
        state.item.submit(
                poseStack,
                collector,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor
        );

        poseStack.popPose();

        super.submit(state, poseStack, collector, camera);
    }
}