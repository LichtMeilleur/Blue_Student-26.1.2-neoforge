package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.HikariModel;
import com.licht_meilleur.blue_student.entity.HikariEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class HikariRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<HikariEntity, R> {
    public HikariRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HikariModel(), 0.4f);
    }
}