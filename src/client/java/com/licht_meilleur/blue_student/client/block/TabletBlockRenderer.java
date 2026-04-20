package com.licht_meilleur.blue_student.client.block;

import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.block.entity.TabletBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class TabletBlockRenderer<R extends BlockEntityRenderState & GeoRenderState>
        extends GeoBlockRenderer<TabletBlockEntity, R> {

    public TabletBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new TabletBlockModel());
    }
}