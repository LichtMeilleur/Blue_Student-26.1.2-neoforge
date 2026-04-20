package com.licht_meilleur.blue_student.client.projectile;

import com.licht_meilleur.blue_student.entity.projectile.GunTrainShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;

public class GunTrainShellRenderer extends EntityRenderer<GunTrainShellEntity, EntityRenderState> {

    public GunTrainShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0f;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }

    @Override
    public void extractRenderState(GunTrainShellEntity entity, EntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }

    @Override
    public void submit(EntityRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {
        // TODO:
        // submitItem(...) で rocket 表示を戻す
    }
}