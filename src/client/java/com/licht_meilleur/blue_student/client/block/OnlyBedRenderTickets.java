package com.licht_meilleur.blue_student.client.block;

import com.geckolib.constant.dataticket.DataTicket;

public final class OnlyBedRenderTickets {
    public static final DataTicket<String> BED_TEXTURE_PATH =
            DataTicket.create("blue_student_only_bed_texture_path", String.class);

    private OnlyBedRenderTickets() {
    }
}