package com.licht_meilleur.blue_student.client.others;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

public class TrainRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<TrainEntity, R> {

    public TrainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TrainModel());
        this.shadowRadius = 0.0f;
    }

    /**
     * 旧 applyRotations 用の計算式を保管
     * 後で submit(...) の PoseStack 回転へ戻す
     */
    @SuppressWarnings("unused")
    private static float legacyModelYaw(float partialTick, float prevYaw, float yaw, float modelOffset) {
        float y = Mth.lerp(partialTick, prevYaw, yaw);
        return 180.0f - y + modelOffset;
    }
}