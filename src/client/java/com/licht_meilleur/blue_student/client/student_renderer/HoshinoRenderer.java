package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.HoshinoModel;
import com.licht_meilleur.blue_student.entity.HoshinoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HoshinoRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<HoshinoEntity, R> {
    public HoshinoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HoshinoModel(), 0.4f);
    }
}