package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.inventory.CraftChamberMenuData;
import com.licht_meilleur.blue_student.inventory.CraftChamberScreenHandler;
import com.licht_meilleur.blue_student.inventory.StudentMenuData;
import com.licht_meilleur.blue_student.inventory.StudentScreenHandler;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.PacketCodec;
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
                Registries.MENU,
                BlueStudentMod.id("student_menu"),
                new ExtendedScreenHandlerType<>(
                        ModScreenHandlers::createStudent,
                        PacketCodec.tuple(
                                PacketCodec.VAR_INT,
                                StudentMenuData::entityId,
                                StudentMenuData::new
                        )
                )
        );

        CRAFT_CHAMBER_MENU = Registry.register(
                Registries.MENU,
                BlueStudentMod.id("craft_chamber_menu"),
                new ExtendedScreenHandlerType<>(
                        ModScreenHandlers::createCraftChamber,
                        PacketCodec.tuple(
                                BlockPos.PACKET_CODEC,
                                CraftChamberMenuData::pos,
                                CraftChamberMenuData::new
                        )
                )
        );
    }

    private static StudentScreenHandler createStudent(int syncId, Inventory inv, StudentMenuData data) {
        int entityId = data.entityId();
        var world = inv.player.level();

        IStudentEntity e = null;
        var raw = world.getEntity(entityId);
        if (raw instanceof IStudentEntity se) {
            e = se;
        }

        return new StudentScreenHandler(
                syncId,
                inv,
                e,
                new SimpleContainer(9),
                new SimpleContainer(1)
        );
    }

    private static CraftChamberScreenHandler createCraftChamber(int syncId, Inventory inv, CraftChamberMenuData data) {
        var world = inv.player.level();

        CraftChamberBlockEntity be = null;
        var raw = world.getBlockEntity(data.pos());
        if (raw instanceof CraftChamberBlockEntity cc) {
            be = cc;
        }

        return new CraftChamberScreenHandler(syncId, inv, be, data.pos());
    }
}