package com.licht_meilleur.blue_student.client.projectile;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class GunTrainShellRenderState extends EntityRenderState {
    public float shellYaw;
    public float shellPitch;

    public final ItemStackRenderState item = new ItemStackRenderState();
}