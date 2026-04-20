package com.licht_meilleur.blue_student.ai.only;

import com.licht_meilleur.blue_student.entity.ShirokoDroneEntity;
import com.licht_meilleur.blue_student.entity.ShirokoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;

public class ShirokoDroneGoal extends Goal {

    private final ShirokoEntity shiroko;

    private static final int CHECK_INTERVAL = 20;
    private static final double RANGE = 18.0;

    private int next = 0;
    private ShirokoDroneEntity drone = null;

    public ShirokoDroneGoal(ShirokoEntity shiroko) {
        this.shiroko = shiroko;
    }

    @Override
    public boolean canUse() {
        return !shiroko.level().isClientSide() && !shiroko.isLifeLockedForGoal();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (!(shiroko.level() instanceof ServerLevel serverLevel)) return;
        if (--next > 0) return;
        next = CHECK_INTERVAL;

        boolean hasEnemy = !serverLevel.getEntitiesOfClass(
                Monster.class,
                shiroko.getBoundingBox().inflate(RANGE),
                e -> e.isAlive()
        ).isEmpty();

        if (!hasEnemy) {
            if (drone != null) {
                drone.discard();
                drone = null;
            }
            return;
        }

        if (drone == null || !drone.isAlive()) {
            drone = new ShirokoDroneEntity(serverLevel)
                    .setOwnerUuid(shiroko.getUUID());

            drone.setPos(shiroko.getX(), shiroko.getEyeY(), shiroko.getZ());
            serverLevel.addFreshEntity(drone);

            shiroko.requestDroneStart();
        }
    }
}