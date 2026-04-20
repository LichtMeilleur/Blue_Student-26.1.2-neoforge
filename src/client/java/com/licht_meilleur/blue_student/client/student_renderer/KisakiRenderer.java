package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.KisakiModel;
import com.licht_meilleur.blue_student.entity.KisakiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class KisakiRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<KisakiEntity, R> {
    public KisakiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KisakiModel(), 0.4f);
    }
}