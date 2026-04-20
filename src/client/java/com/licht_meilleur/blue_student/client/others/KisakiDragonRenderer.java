package com.licht_meilleur.blue_student.client.others;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.KisakiDragonEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class KisakiDragonRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<KisakiDragonEntity, R> {

    public KisakiDragonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KisakiDragonModel());
        this.shadowRadius = 0.0f;
    }
}