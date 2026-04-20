package com.licht_meilleur.blue_student.client.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public class CraftChamberRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<CraftChamberBlockEntity, R> {

    public CraftChamberRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new CraftChamberModel());
    }

    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        float rotY = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST  -> 90f;
            case EAST  -> -90f;
            default    -> 0f;
        };

        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
    }
}