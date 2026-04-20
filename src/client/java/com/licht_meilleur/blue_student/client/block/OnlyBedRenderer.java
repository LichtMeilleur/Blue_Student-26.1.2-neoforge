package com.licht_meilleur.blue_student.client.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class OnlyBedRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<OnlyBedBlockEntity, R> {

    public OnlyBedRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new OnlyBedModel());
    }

    @Override
    public void addRenderData(OnlyBedBlockEntity animatable, Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(animatable, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(
                OnlyBedRenderTickets.BED_TEXTURE_PATH,
                animatable.getBedTexturePath()
        );
    }
}