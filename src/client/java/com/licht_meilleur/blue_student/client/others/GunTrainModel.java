package com.licht_meilleur.blue_student.client.others;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.GunTrainEntity;
import net.minecraft.resources.Identifier;

public class GunTrainModel extends GeoModel<GunTrainEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return BlueStudentMod.id("gun_train");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return BlueStudentMod.id("textures/entity/gun_train.png");
    }

    @Override
    public Identifier getAnimationResource(GunTrainEntity animatable) {
        return BlueStudentMod.id("gun_train");
    }
}