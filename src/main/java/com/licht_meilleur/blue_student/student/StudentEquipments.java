package com.licht_meilleur.blue_student.student;

import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class StudentEquipments {

    private StudentEquipments() {
    }

    public static Identifier getBrSlotTexture(StudentId sid) {
        return switch (sid) {
            case HOSHINO -> BlueStudentMod.id("textures/gui/hoshino_br_equip_item.png");
            case ALICE -> BlueStudentMod.id("textures/gui/alice_br_equip_item.png");
            default -> BlueStudentMod.id("textures/gui/empty_br.png");
        };
    }

    public static boolean supportsBr(StudentId sid) {
        return switch (sid) {
            case HOSHINO -> true;
            case ALICE -> true;
            default -> false;
        };
    }

    public static boolean isBrEquipped(StudentId sid, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!sid.hasBrForm()) return false;

        return switch (sid) {
            case HOSHINO -> stack.is(BlueStudentMod.HOSHINO_BR_EQUIP_ITEM);
            case ALICE -> stack.is(BlueStudentMod.ALICE_BR_EQUIP_ITEM);
            default -> false;
        };
    }
}