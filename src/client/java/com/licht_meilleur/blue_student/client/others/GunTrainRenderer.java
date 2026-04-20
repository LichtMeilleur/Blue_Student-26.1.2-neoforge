package com.licht_meilleur.blue_student.client.others;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;

public class GunTrainRenderer<R extends EntityRenderState & GeoRenderState>
        extends GeoEntityRenderer<GunTrainEntity, R> {

    public GunTrainRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GunTrainModel());
        this.shadowRadius = 0.0f;
    }

    /**
     * 旧 model.setCustomAnimations 用の砲塔相対Yaw計算を保管
     * 後で addRenderData(...) → model 側へ戻す
     */
    @SuppressWarnings("unused")
    private static float legacySheetRelativeYawRad(float bodyYawDeg, float aimYawDeg) {
        float sheetOffsetDeg = 0.0f;
        float sheetSign = -1.0f;
        float relDeg = Mth.wrapDegrees((aimYawDeg - bodyYawDeg) + sheetOffsetDeg);
        return relDeg * Mth.DEG_TO_RAD * sheetSign;
    }
}