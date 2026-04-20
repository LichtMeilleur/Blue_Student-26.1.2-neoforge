package com.licht_meilleur.blue_student.client.student_model;

import com.geckolib.renderer.base.GeoRenderState;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.client.StudentRenderTickets;
import com.licht_meilleur.blue_student.entity.AliceEntity;
import com.licht_meilleur.blue_student.student.StudentForm;
import net.minecraft.resources.Identifier;

public class AliceModel extends BaseStudentModel<AliceEntity> {

    public AliceModel() {
        super(
                "geo/alice",
                "textures/entity/alice.png",
                "animations/alice.animation.json"
        );
    }

    @Override
    public Identifier getModelResource(GeoRenderState renderState) {
        StudentForm form = renderState.getOrDefaultGeckolibData(StudentRenderTickets.FORM, StudentForm.NORMAL);
        if (form == StudentForm.BR) {
            return BlueStudentMod.id("alice_br");
        }
        return BlueStudentMod.id("alice");
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        StudentForm form = renderState.getOrDefaultGeckolibData(StudentRenderTickets.FORM, StudentForm.NORMAL);
        if (form == StudentForm.BR) {
            return BlueStudentMod.id("textures/entity/alice_br.png");
        }
        return BlueStudentMod.id("textures/entity/alice.png");
    }

    @Override
    public Identifier getAnimationResource(AliceEntity animatable) {
        if (animatable.getRenderForm() == StudentForm.BR) {
            return BlueStudentMod.id("alice_br");
        }
        return BlueStudentMod.id("alice");
    }
}