package com.licht_meilleur.blue_student.client.student_renderer;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.client.StudentRenderer;
import com.licht_meilleur.blue_student.client.student_model.ShirokoModel;
import com.licht_meilleur.blue_student.entity.ShirokoEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ShirokoRenderer<R extends LivingEntityRenderState & GeoRenderState> extends StudentRenderer<ShirokoEntity, R> {
    public ShirokoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ShirokoModel(), 0.4f);
    }
}