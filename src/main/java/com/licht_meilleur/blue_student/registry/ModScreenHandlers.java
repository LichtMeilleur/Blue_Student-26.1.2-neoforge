package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.inventory.CraftChamberMenuData;
import com.licht_meilleur.blue_student.inventory.CraftChamberScreenHandler;
import com.licht_meilleur.blue_student.inventory.StudentMenuData;
import com.licht_meilleur.blue_student.inventory.StudentScreenHandler;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {

    public static MenuType<StudentScreenHandler> STUDENT_MENU;
    public static MenuType<CraftChamberScreenHandler> CRAFT_CHAMBER_MENU;

    private static boolean REGISTERED = false;

    public static void register() {
        if (REGISTERED) return;
        REGISTERED = true;

        STUDENT_MENU = Registry.register(
                BuiltInRegistries.MENU,
                BlueStudentMod.id("student_menu"),
                new ExtendedMenuType<StudentScreenHandler, StudentMenuData>(
                        ModScreenHandlers::createStudent,
                        StudentMenuData.STREAM_CODEC
                )
        );

        CRAFT_CHAMBER_MENU = Registry.register(
                BuiltInRegistries.MENU,
                BlueStudentMod.id("craft_chamber_menu"),
                new ExtendedMenuType<CraftChamberScreenHandler, CraftChamberMenuData>(
                        ModScreenHandlers::createCraftChamber,
                        CraftChamberMenuData.STREAM_CODEC
                )
        );
    }

    private static StudentScreenHandler createStudent(int syncId, Inventory inv, StudentMenuData data) {
        var player = inv.player;
        var level = player.level();

        IStudentEntity student = null;
        var raw = level.getEntity(data.entityId());
        if (raw instanceof IStudentEntity se) {
            student = se;
        }

        if (student == null) {
            return new StudentScreenHandler(
                    syncId,
                    inv,
                    null,
                    new SimpleContainer(9),
                    new SimpleContainer(1)
            );
        }

        var studentInv = student.getStudentInventory();
        Container equipInv = new SimpleContainer(1);

        if (studentInv instanceof com.licht_meilleur.blue_student.inventory.StudentInventory si) {
            equipInv = si.getEquipInv();
        }

        return new StudentScreenHandler(
                syncId,
                inv,
                student,
                studentInv,
                equipInv
        );
    }

    private static CraftChamberScreenHandler createCraftChamber(int syncId, Inventory inv, CraftChamberMenuData data) {
        var player = inv.player;
        var level = player.level();

        CraftChamberBlockEntity be = null;
        var raw = level.getBlockEntity(data.pos());
        if (raw instanceof CraftChamberBlockEntity cc) {
            be = cc;
        }

        return new CraftChamberScreenHandler(syncId, inv, be, data.pos());
    }
}