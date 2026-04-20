package com.licht_meilleur.blue_student.client.student_model;

import com.licht_meilleur.blue_student.client.BaseStudentModel;
import com.licht_meilleur.blue_student.entity.HikariEntity;

public class HikariModel extends BaseStudentModel<HikariEntity> {
    public HikariModel() {
        super(
                "hikari",
                "textures/entity/hikari.png",
                "hikari"
        );
    }
}