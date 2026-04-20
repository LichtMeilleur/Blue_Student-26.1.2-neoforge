package com.licht_meilleur.blue_student.client.others.go_go_train;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.go_go_train.GoGoTrainEntity;
import net.minecraft.resources.Identifier;

import static com.licht_meilleur.blue_student.BlueStudentMod.id;

public class GoGoTrainModel extends GeoModel<GoGoTrainEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return id("train");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return id("textures/entity/train.png");
    }

    @Override
    public Identifier getAnimationResource(GoGoTrainEntity animatable) {
        return id("train");
    }
}