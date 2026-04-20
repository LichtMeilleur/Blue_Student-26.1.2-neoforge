package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.HinaModel;
import com.licht_meilleur.blue_student.entity.HinaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HinaRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<HinaEntity, R> {
    public HinaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HinaModel(), 0.4f);
    }
}