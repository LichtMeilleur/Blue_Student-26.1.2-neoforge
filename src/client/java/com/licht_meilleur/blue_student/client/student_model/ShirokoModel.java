package com.licht_meilleur.blue_student.client.student_model;

import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.entity.ShirokoEntity;

public class ShirokoModel extends BaseStudentModel<ShirokoEntity> {
    public ShirokoModel() {
        super(
                "shiroko",
                "textures/entity/shiroko.png",
                "shiroko"
        );
    }
}