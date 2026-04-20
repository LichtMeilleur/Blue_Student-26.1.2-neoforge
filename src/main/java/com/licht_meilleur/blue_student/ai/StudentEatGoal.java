package com.licht_meilleur.blue_student.ai;

import com.licht_meilleur.blue_student.entity.AbstractStudentEntity;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

public class StudentEatGoal extends Goal {

    private final PathfinderMob mob;
    private final IStudentEntity student;

    private final float triggerHpRatio;
    private final double hostileRadius;
    private final int eatDurationTicks;
    private final int eatCooldownTicks;
    private final int healAmount;

    private int eatTicksLeft = 0;
    private int cooldown = 0;
    private int eatingSlot = -1;

    public StudentEatGoal(PathfinderMob mob, IStudentEntity student) {
        this(mob, student, 0.85f, 10.0, 16, 40, 6);
    }

    public StudentEatGoal(
            PathfinderMob mob,
            IStudentEntity student,
            float triggerHpRatio,
            double hostileRadius,
            int eatDurationTicks,
            int eatCooldownTicks,
            int healAmount
    ) {
        this.mob = mob;
        this.student = student;
        this.triggerHpRatio = triggerHpRatio;
        this.hostileRadius = hostileRadius;
        this.eatDurationTicks = eatDurationTicks;
        this.eatCooldownTicks = eatCooldownTicks;
        this.healAmount = healAmount;

        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return false;

        if (mob instanceof AbstractStudentEntity se) {
            if (se.isLifeLockedForGoal()) return false;
        }

        StudentAiMode mode = student.getAiMode();
        if (mode != StudentAiMode.FOLLOW && mode != StudentAiMode.SECURITY) return false;

        if (cooldown > 0) return false;

        float max = mob.getMaxHealth();
        if (max <= 0.01f) return false;
        float ratio = mob.getHealth() / max;
        if (ratio >= triggerHpRatio) return false;

        if (hasHostileNearby()) return false;

        eatingSlot = findFoodSlot();
        return eatingSlot >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (eatTicksLeft <= 0) return false;
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return false;
        if (hasHostileNearby()) return false;
        if (eatingSlot < 0) return false;

        ItemStack st = getInvStack(eatingSlot);
        return !st.isEmpty() && st.has(DataComponents.FOOD);
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);

        eatTicksLeft = eatDurationTicks;

        if (mob instanceof AbstractStudentEntity se) {
            se.requestEatFromGoal();
            se.startEatingVisualFromGoal(eatingSlot, eatDurationTicks);
        }
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0);

        eatTicksLeft--;

        if (eatTicksLeft == 0) {
            consumeAndHeal();
            cooldown = eatCooldownTicks;
            eatingSlot = -1;
        }
    }

    @Override
    public void stop() {
        eatTicksLeft = 0;
        eatingSlot = -1;
    }

    private boolean hasHostileNearby() {
        AABB box = mob.getBoundingBox().inflate(hostileRadius);
        return !mob.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive).isEmpty();
    }

    private int findFoodSlot() {
        if (!(mob instanceof AbstractStudentEntity se)) return -1;

        for (int i = 0; i < se.getStudentInventory().getContainerSize(); i++) {
            ItemStack st = se.getStudentInventory().getItem(i);
            if (st.isEmpty()) continue;
            if (!st.has(DataComponents.FOOD)) continue;
            if (se.isBadFoodItemForGoal(st)) continue;

            FoodProperties food = st.get(DataComponents.FOOD);
            if (food == null) continue;

            return i;
        }
        return -1;
    }

    private ItemStack getInvStack(int slot) {
        if (!(mob instanceof AbstractStudentEntity se)) return ItemStack.EMPTY;
        if (slot < 0 || slot >= se.getStudentInventory().getContainerSize()) return ItemStack.EMPTY;
        return se.getStudentInventory().getItem(slot);
    }

    private void consumeAndHeal() {
        if (!(mob instanceof AbstractStudentEntity se)) return;
        if (eatingSlot < 0 || eatingSlot >= se.getStudentInventory().getContainerSize()) return;

        ItemStack st = se.getStudentInventory().getItem(eatingSlot);
        if (st.isEmpty()) return;
        if (!st.has(DataComponents.FOOD)) return;
        if (se.isBadFoodItemForGoal(st)) return;

        se.heal(healAmount);
        st.shrink(1);
        se.getStudentInventory().setChanged();
    }

    public void tickCooldown() {
        if (cooldown > 0) cooldown--;
    }
}