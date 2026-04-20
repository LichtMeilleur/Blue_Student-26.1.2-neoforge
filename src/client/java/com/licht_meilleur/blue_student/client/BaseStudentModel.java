package com.licht_meilleur.blue_student.client;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import net.minecraft.resources.Identifier;

public abstract class BaseStudentModel<T extends AbstractStudentEntity> extends GeoModel<T> {

    private final String normalGeoPath;
    private final String normalTexturePath;
    private final String normalAnimationPath;

    protected BaseStudentModel(String normalGeoPath, String normalTexturePath, String normalAnimationPath) {
        this.normalGeoPath = normalGeoPath;
        this.normalTexturePath = normalTexturePath;
        this.normalAnimationPath = normalAnimationPath;
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return BlueStudentMod.id(this.normalGeoPath);
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        return BlueStudentMod.id(this.normalTexturePath);
    }

    @Override
    public Identifier getAnimationResource(T animatable) {
        return BlueStudentMod.id(this.normalAnimationPath);
    }
}