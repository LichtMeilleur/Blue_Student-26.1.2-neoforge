package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.MarieModel;
import com.licht_meilleur.blue_student.entity.MarieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MarieRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<MarieEntity, R> {
    public MarieRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MarieModel(), 0.4f);
    }
}