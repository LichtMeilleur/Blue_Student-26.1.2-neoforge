package com.licht_meilleur.blue_student.student;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class LookRequest {
    public LookIntentType type = LookIntentType.NONE;
    public LivingEntity target = null;
    public Vec3 dir = null;
    public int priority = 0;
    public int holdTicks = 0;
    public Vec3 pos = null;

    public void clear() {
        type = LookIntentType.NONE;
        target = null;
        dir = null;
        priority = 0;
        holdTicks = 0;
        pos = null;
    }
}