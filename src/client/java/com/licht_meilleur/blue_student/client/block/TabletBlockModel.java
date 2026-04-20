package com.licht_meilleur.blue_student.client.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.TabletBlockEntity;
import net.minecraft.resources.Identifier;

public class TabletBlockModel extends GeoModel<TabletBlockEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return BlueStudentMod.id("tablet");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return BlueStudentMod.id("textures/entity/tablet.png");
    }

    @Override
    public Identifier getAnimationResource(TabletBlockEntity animatable) {
        return BlueStudentMod.id("tablet");
    }
}