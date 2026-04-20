package com.licht_meilleur.blue_student.client.student_model;

import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.entity.HinaEntity;

public class HinaModel extends BaseStudentModel<HinaEntity> {
    public HinaModel() {
        super(
                "hina",
                "textures/entity/hina.png",
                "hina"
        );
    }
}