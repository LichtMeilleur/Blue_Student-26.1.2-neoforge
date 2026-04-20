package com.licht_meilleur.blue_student.client.others.go_go_train;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

public class GoGoGunTrainRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<GoGoGunTrainEntity, R> {

    public GoGoGunTrainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GoGoGunTrainModel());
        this.shadowRadius = 0.0f;
    }

    /**
     * 旧 model.setCustomAnimations 用の絶対Yaw計算を保管
     * 後で addRenderData(...) → model 側へ戻す
     */
    @SuppressWarnings("unused")
    private static float legacySheetYawRad(float aimYawDeg) {
        return aimYawDeg * Mth.DEG_TO_RAD * -1.0f;
    }
}