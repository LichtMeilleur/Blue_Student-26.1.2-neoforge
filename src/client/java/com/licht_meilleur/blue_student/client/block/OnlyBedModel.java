package com.licht_meilleur.blue_student.client.block;

import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import net.minecraft.resources.Identifier;

public class OnlyBedModel extends GeoModel<OnlyBedBlockEntity> {

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        return BlueStudentMod.id("only_for_bed");
    }

    @Override
    public Identifier getAnimationResource(OnlyBedBlockEntity animatable) {
        return BlueStudentMod.id("only_for_bed");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        String path = renderState.getOrDefaultGeckolibData(
                OnlyBedRenderTickets.BED_TEXTURE_PATH,
                "textures/entity/shiroko_bed.png"
        );

        return BlueStudentMod.id(path);
    }
}