package com.licht_meilleur.blue_student.registry;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.entity.CraftChamberBlockEntity;
import com.licht_meilleur.blue_student.inventory.CraftChamberMenuData;
import com.licht_meilleur.blue_student.inventory.CraftChamberScreenHandler;
import com.licht_meilleur.blue_student.inventory.StudentMenuData;
import com.licht_meilleur.blue_student.inventory.StudentScreenHandler;
import com.licht_meilleur.blue_student.inventory.StudentInventory;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModScreenHandlers {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, BlueStudentMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StudentScreenHandler>> STUDENT_MENU =
            MENUS.register("student_menu",
                    () -> IMenuTypeExtension.create(ModScreenHandlers::createStudent));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftChamberScreenHandler>> CRAFT_CHAMBER_MENU =
            MENUS.register("craft_chamber_menu",
                    () -> IMenuTypeExtension.create(ModScreenHandlers::createCraftChamber));

    private static StudentScreenHandler createStudent(int syncId, Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf buf) {

        StudentMenuData data = StudentMenuData.STREAM_CODEC.decode(buf);

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

        if (studentInv instanceof StudentInventory si) {
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

    private static CraftChamberScreenHandler createCraftChamber(int syncId, Inventory inv, net.minecraft.network.RegistryFriendlyByteBuf buf) {

        CraftChamberMenuData data = CraftChamberMenuData.STREAM_CODEC.decode(buf);

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