package com.licht_meilleur.blue_student.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.licht_meilleur.blue_student.student.StudentForm;

public final class StudentRenderTickets {

    public static final DataTicket<StudentForm> FORM =
            DataTicket.create("blue_student_student_form", StudentForm.class);

    public static final DataTicket<Float> HEAD_YAW_RAD =
            DataTicket.create("blue_student_head_yaw_rad", Float.class);

    public static final DataTicket<Float> HEAD_PITCH_RAD =
            DataTicket.create("blue_student_head_pitch_rad", Float.class);

    public static final DataTicket<Float> ARM_YAW_RAD =
            DataTicket.create("blue_student_arm_yaw_rad", Float.class);

    public static final DataTicket<Float> ARM_PITCH_RAD =
            DataTicket.create("blue_student_arm_pitch_rad", Float.class);

    private StudentRenderTickets() {
    }
}