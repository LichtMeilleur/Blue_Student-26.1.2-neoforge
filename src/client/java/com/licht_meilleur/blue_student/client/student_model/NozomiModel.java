package com.licht_meilleur.blue_student.client.student_model;

import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.entity.NozomiEntity;

public class NozomiModel extends BaseStudentModel<NozomiEntity> {
    public NozomiModel() {
        super(
                "nozomi",
                "textures/entity/nozomi.png",
                "nozomi"
        );
    }
}