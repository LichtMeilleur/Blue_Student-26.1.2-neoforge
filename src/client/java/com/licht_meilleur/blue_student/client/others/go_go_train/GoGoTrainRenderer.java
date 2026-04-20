package com.licht_meilleur.blue_student.client.others.go_go_train;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

public class GoGoTrainRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<GoGoTrainEntity, R> {

    public GoGoTrainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoGoTrainModel());
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