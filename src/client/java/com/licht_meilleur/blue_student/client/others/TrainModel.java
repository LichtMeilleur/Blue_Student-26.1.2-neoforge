package com.licht_meilleur.blue_student.client.others;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.TrainEntity;
import net.minecraft.resources.Identifier;

import static com.licht_meilleur.blue_student.BlueStudentMod.id;

public class TrainModel extends GeoModel<TrainEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return id("train");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return id("textures/entity/train.png");
    }

    @Override
    public Identifier getAnimationResource(TrainEntity animatable) {
        return id("train");
    }
}