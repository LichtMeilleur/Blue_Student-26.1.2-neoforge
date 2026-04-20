package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.NozomiModel;
import com.licht_meilleur.blue_student.entity.NozomiEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class NozomiRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<NozomiEntity, R> {
    public NozomiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NozomiModel(), 0.4f);
    }
}