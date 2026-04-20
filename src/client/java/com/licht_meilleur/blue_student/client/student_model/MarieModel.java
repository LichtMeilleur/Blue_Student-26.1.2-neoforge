package com.licht_meilleur.blue_student.client.student_model;

import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.entity.MarieEntity;

public class MarieModel extends BaseStudentModel<MarieEntity> {
    public MarieModel() {
        super(
                "marie",
                "textures/entity/marie.png",
                "marie"
        );
    }
}