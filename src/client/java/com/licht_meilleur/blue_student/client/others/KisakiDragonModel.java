package com.licht_meilleur.blue_student.client.others;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.entity.KisakiDragonEntity;
import net.minecraft.resources.Identifier;

import static com.licht_meilleur.blue_student.BlueStudentMod.id;

public class KisakiDragonModel extends GeoModel<KisakiDragonEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return id("kisaki_dragon");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return id("textures/entity/kisaki_dragon.png");
    }

    @Override
    public Identifier getAnimationResource(KisakiDragonEntity animatable) {
        return id("kisaki_dragon");
    }
}