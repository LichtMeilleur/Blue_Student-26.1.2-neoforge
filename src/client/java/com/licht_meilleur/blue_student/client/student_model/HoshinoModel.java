package com.licht_meilleur.blue_student.client.student_model;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.client.StudentRenderTickets;
import com.licht_meilleur.blue_student.entity.HoshinoEntity;
import com.licht_meilleur.blue_student.student.StudentForm;
import net.minecraft.resources.Identifier;

public class HoshinoModel extends BaseStudentModel<HoshinoEntity> {

    public HoshinoModel() {
        super(
                "geo/hoshino",
                "textures/entity/hoshino.png",
                "animations/hoshino.animation.json"
        );
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        StudentForm form = renderState.getOrDefaultGeckolibData(StudentRenderTickets.FORM, StudentForm.NORMAL);
        if (form == StudentForm.BR) {
            return BlueStudentMod.id("hoshino_br");
        }
        return BlueStudentMod.id("hoshino");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        StudentForm form = renderState.getOrDefaultGeckolibData(StudentRenderTickets.FORM, StudentForm.NORMAL);
        if (form == StudentForm.BR) {
            return BlueStudentMod.id("textures/entity/hoshino_br.png");
        }
        return BlueStudentMod.id("textures/entity/hoshino.png");
    }

    @Override
    public Identifier getAnimationResource(HoshinoEntity animatable) {
        if (animatable.getRenderForm() == StudentForm.BR) {
            return BlueStudentMod.id("hoshino_br");
        }
        return BlueStudentMod.id("hoshino");
    }
}