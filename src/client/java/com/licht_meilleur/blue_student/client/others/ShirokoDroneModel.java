package com.licht_meilleur.blue_student.client.others;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.ShirokoDroneEntity;
import net.minecraft.resources.Identifier;

import static com.licht_meilleur.blue_student.BlueStudentMod.id;

public class ShirokoDroneModel extends GeoModel<ShirokoDroneEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return id("shiroko_drone");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return id("textures/entity/shiroko.png");
    }

    @Override
    public Identifier getAnimationResource(ShirokoDroneEntity animatable) {
        return id("shiroko_drone");
    }
}