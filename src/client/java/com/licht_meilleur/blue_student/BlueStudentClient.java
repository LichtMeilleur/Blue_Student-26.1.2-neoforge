package com.licht_meilleur.blue_student;

import com.licht_meilleur.blue_student.client.block.CraftChamberRenderer;
import com.licht_meilleur.blue_student.client.block.*;
import com.licht_meilleur.blue_student.client.network.ClientPackets;
import com.licht_meilleur.blue_student.client.others.go_go_train.GoGoGunTrainRenderer;
import com.licht_meilleur.blue_student.client.others.go_go_train.GoGoTrainRenderer;
import com.licht_meilleur.blue_student.client.others.GunTrainRenderer;
import com.licht_meilleur.blue_student.client.others.TrainRenderer;
import com.licht_meilleur.blue_student.client.projectile.BulletRenderer;
import com.licht_meilleur.blue_student.client.projectile.GunTrainShellRenderer;
import com.licht_meilleur.blue_student.client.projectile.SonicBeamRenderer;
import com.licht_meilleur.blue_student.client.screen.CraftChamberScreen;
import com.licht_meilleur.blue_student.client.screen.StudentScreen;
import com.licht_meilleur.blue_student.client.screen.TabletScreen;
import com.licht_meilleur.blue_student.client.student_renderer.*;
import com.licht_meilleur.blue_student.client.others.*;
import com.licht_meilleur.blue_student.registry.ModEntities;
import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class BlueStudentClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[BlueStudent] onInitializeClient PRINT");

        ModScreenHandlers.register();

        EntityRenderers.register(BlueStudentMod.SHIROKO, ShirokoRenderer::new);
        EntityRenderers.register(BlueStudentMod.HOSHINO, HoshinoRenderer::new);
        EntityRenderers.register(BlueStudentMod.HINA, HinaRenderer::new);
        EntityRenderers.register(BlueStudentMod.KISAKI, KisakiRenderer::new);
        EntityRenderers.register(BlueStudentMod.ALICE, AliceRenderer::new);
        EntityRenderers.register(BlueStudentMod.MARIE, MarieRenderer::new);
        EntityRenderers.register(BlueStudentMod.HIKARI, HikariRenderer::new);
        EntityRenderers.register(BlueStudentMod.NOZOMI, NozomiRenderer::new);

        EntityRenderers.register(BlueStudentMod.KISAKI_DRAGON, KisakiDragonRenderer::new);
        EntityRenderers.register(BlueStudentMod.SHIROKO_DRONE, ShirokoDroneRenderer::new);
        EntityRenderers.register(ModEntities.TRAIN, TrainRenderer::new);
        EntityRenderers.register(ModEntities.GUN_TRAIN, GunTrainRenderer::new);
        EntityRenderers.register(ModEntities.GO_GO_TRAIN, GoGoTrainRenderer::new);
        EntityRenderers.register(ModEntities.GO_GO_GUN_TRAIN, GoGoGunTrainRenderer::new);

        EntityRenderers.register(BlueStudentMod.STUDENT_BULLET, BulletRenderer::new);
        EntityRenderers.register(ModEntities.SONIC_BEAM, SonicBeamRenderer::new);
        EntityRenderers.register(ModEntities.GUN_TRAIN_SHELL, GunTrainShellRenderer::new);

        BlockEntityRenderers.register(BlueStudentMod.TABLET_BE, TabletBlockRenderer::new);
        BlockEntityRenderers.register(BlueStudentMod.CRAFT_CHAMBER_BE, CraftChamberRenderer::new);

        BlockEntityRenderers.register(BlueStudentMod.ONLY_BED_BE, OnlyBedRenderer::new);

        MenuScreens.register(ModScreenHandlers.STUDENT_MENU, StudentScreen::new);
        MenuScreens.register(ModScreenHandlers.CRAFT_CHAMBER_MENU, CraftChamberScreen::new);

        BlueStudentMod.OPEN_TABLET_SCREEN = TabletScreen::open;

        ClientPackets.registerS2C();
    }
}