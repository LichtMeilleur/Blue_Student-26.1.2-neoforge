package com.licht_meilleur.blue_student.client.others;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.ShirokoDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class ShirokoDroneRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<ShirokoDroneEntity, R> {

    public ShirokoDroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShirokoDroneModel());
        this.shadowRadius = 0.0f;
    }
}