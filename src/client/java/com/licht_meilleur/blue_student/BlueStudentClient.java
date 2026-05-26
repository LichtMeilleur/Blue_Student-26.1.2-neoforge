package com.licht_meilleur.blue_student;

import com.licht_meilleur.blue_student.client.block.CraftChamberRenderer;
import com.licht_meilleur.blue_student.client.block.OnlyBedRenderer;
import com.licht_meilleur.blue_student.client.block.TabletBlockRenderer;
import com.licht_meilleur.blue_student.client.others.GunTrainRenderer;
import com.licht_meilleur.blue_student.client.others.TrainRenderer;
import com.licht_meilleur.blue_student.client.others.go_go_train.GoGoGunTrainRenderer;
import com.licht_meilleur.blue_student.client.others.go_go_train.GoGoTrainRenderer;
import com.licht_meilleur.blue_student.client.others.KisakiDragonRenderer;
import com.licht_meilleur.blue_student.client.others.ShirokoDroneRenderer;
import com.licht_meilleur.blue_student.client.projectile.BulletRenderer;
import com.licht_meilleur.blue_student.client.projectile.GunTrainShellRenderer;
import com.licht_meilleur.blue_student.client.projectile.SonicBeamRenderer;
import com.licht_meilleur.blue_student.client.screen.CraftChamberScreen;
import com.licht_meilleur.blue_student.client.screen.StudentScreen;
import com.licht_meilleur.blue_student.client.screen.TabletScreen;
import com.licht_meilleur.blue_student.client.student_renderer.*;
import com.licht_meilleur.blue_student.registry.ModEntities;
import com.licht_meilleur.blue_student.registry.ModScreenHandlers;
import com.licht_meilleur.blue_student.client.network.ClientPackets;
import com.licht_meilleur.blue_student.network.ModPackets;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;


@EventBusSubscriber(
        modid = BlueStudentMod.MOD_ID,
        value = Dist.CLIENT
)

public final class BlueStudentClient {

    private BlueStudentClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        BlueStudentMod.LOGGER.info("[BlueStudentClient] registerRenderers");

        event.registerEntityRenderer(BlueStudentMod.SHIROKO.get(), ShirokoRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.HOSHINO.get(), HoshinoRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.HINA.get(), HinaRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.KISAKI.get(), KisakiRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.ALICE.get(), AliceRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.MARIE.get(), MarieRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.HIKARI.get(), HikariRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.NOZOMI.get(), NozomiRenderer::new);

        event.registerEntityRenderer(BlueStudentMod.KISAKI_DRAGON.get(), KisakiDragonRenderer::new);
        event.registerEntityRenderer(BlueStudentMod.SHIROKO_DRONE.get(), ShirokoDroneRenderer::new);

        event.registerEntityRenderer(ModEntities.TRAIN.get(), TrainRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_TRAIN.get(), GunTrainRenderer::new);
        event.registerEntityRenderer(ModEntities.GO_GO_TRAIN.get(), GoGoTrainRenderer::new);
        event.registerEntityRenderer(ModEntities.GO_GO_GUN_TRAIN.get(), GoGoGunTrainRenderer::new);

        event.registerEntityRenderer(BlueStudentMod.STUDENT_BULLET.get(), BulletRenderer::new);
        event.registerEntityRenderer(ModEntities.SONIC_BEAM.get(), SonicBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.GUN_TRAIN_SHELL.get(), GunTrainShellRenderer::new);

        event.registerBlockEntityRenderer(BlueStudentMod.TABLET_BE.get(), TabletBlockRenderer::new);
        event.registerBlockEntityRenderer(BlueStudentMod.CRAFT_CHAMBER_BE.get(), CraftChamberRenderer::new);
        event.registerBlockEntityRenderer(BlueStudentMod.ONLY_BED_BE.get(), OnlyBedRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModScreenHandlers.STUDENT_MENU.get(), StudentScreen::new);
        event.register(ModScreenHandlers.CRAFT_CHAMBER_MENU.get(), CraftChamberScreen::new);

        BlueStudentMod.OPEN_TABLET_SCREEN = TabletScreen::open;
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(BlueStudentMod.MOD_ID).versioned("1")
                .playToClient(
                        ModPackets.ShotFxPayload.TYPE,
                        ModPackets.ShotFxPayload.CODEC,
                        (payload, context) ->
                                context.enqueueWork(() ->
                                        ClientPackets.handleShotFx(payload)
                                )
                );
    }
}