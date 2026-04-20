package com.licht_meilleur.blue_student.client.projectile;

import com.licht_meilleur.blue_student.entity.projectile.SonicBeamEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SonicBeamRenderer extends EntityRenderer<SonicBeamEntity, SonicBeamRenderer.SonicBeamRenderState> {

    public SonicBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public SonicBeamRenderState createRenderState() {
        return new SonicBeamRenderState();
    }

    @Override
    public void extractRenderState(SonicBeamEntity entity, SonicBeamRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.start = entity.getStart();
        state.end = entity.getEnd();
    }

    @Override
    public void submit(SonicBeamRenderState state,
                       PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector,
                       CameraRenderState camera) {

        Vec3 start = state.start;
        Vec3 end = state.end;
        Vec3 cam = camera.pos;

        poseStack.pushPose();
        poseStack.translate(start.x - cam.x, start.y - cam.y, start.z - cam.z);

        Vec3 dir = end.subtract(start);

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.lightning(),
                (pose, buffer) -> drawBeam(buffer, pose.pose(), dir, 0.3f)
        );

        poseStack.popPose();
    }

    private void drawBeam(VertexConsumer buffer, Matrix4f matrix, Vec3 dir, float width) {
        float y = (float) dir.y;
        float z = (float) dir.z;

        buffer.addVertex(matrix, -width, 0.0f, 0.0f).setColor(80, 200, 255, 180);
        buffer.addVertex(matrix,  width, 0.0f, 0.0f).setColor(80, 200, 255, 180);
        buffer.addVertex(matrix,  width, y,    z).setColor(80, 200, 255, 180);
        buffer.addVertex(matrix, -width, y,    z).setColor(80, 200, 255, 180);
    }

    public static class SonicBeamRenderState extends EntityRenderState {
        public Vec3 start = Vec3.ZERO;
        public Vec3 end = Vec3.ZERO;
    }
}