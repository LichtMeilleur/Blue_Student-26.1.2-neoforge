package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.AliceModel;
import com.licht_meilleur.blue_student.entity.AliceEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class AliceRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<AliceEntity, R> {
    public AliceRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new AliceModel(), 0.4f);
    }
}