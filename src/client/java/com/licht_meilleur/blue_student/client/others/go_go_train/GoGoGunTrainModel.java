package com.licht_meilleur.blue_student.client.others.go_go_train;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoGunTrainEntity;
import net.minecraft.resources.Identifier;

import static com.licht_meilleur.blue_student.BlueStudentMod.id;

public class GoGoGunTrainModel extends GeoModel<GoGoGunTrainEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return id("gun_train");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return id("textures/entity/gun_train.png");
    }

    @Override
    public Identifier getAnimationResource(GoGoGunTrainEntity animatable) {
        return id("gun_train");
    }
}