package com.licht_meilleur.blue_student.entity;

import com.geckolib.animation.RawAnimation;
import com.licht_meilleur.blue_student.ai.*;
import com.licht_meilleur.blue_student.ai.only.KisakiBuffGoal;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class KisakiEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(KisakiEntity.class, EntityDataSerializers.INT);

    public static final String ANIM_BUFF = "animation.model.buff";
    private static final RawAnimation BUFF = RawAnimation.begin().thenLoop(ANIM_BUFF);

    private static final EntityDataAccessor<Integer> BUFF_TRIGGER =
            SynchedEntityData.defineId(KisakiEntity.class, EntityDataSerializers.INT);

    private int clientBuffTicks = 0;
    private int lastBuffTrigger = 0;
    private static final int BUFF_ANIM_TICKS = 20;

    private int cooldownTicks = 0;
    private static final int COOLDOWN = 200;

    private int buffCastingTicks = 0;

    public KisakiEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BUFF_TRIGGER, 0);
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.KISAKI;
    }

    @Override
    protected EntityDataAccessor<Integer> getAiModeTrackedData() {
        return AI_MODE;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        UUID owner = getOwnerUuid();
        if (owner == null) {
            setOwnerUuid(player.getUUID());
            owner = player.getUUID();
        }

        if (!player.getUUID().equals(owner)) {
            return InteractionResult.CONSUME;
        }

        ItemStack inHand = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && inHand.isEmpty()) {
            BedLinkManager.setLinking(player.getUUID(), StudentId.KISAKI);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "kisaki"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new KisakiBuffGoal(this, this));
        this.goalSelector.addGoal(3, new StudentEvadeGoal(this, this));
        this.goalSelector.addGoal(4, new StudentStuckEscapeGoal(this, this));
        this.goalSelector.addGoal(5, new StudentCliffAvoidGoal(this));
        this.goalSelector.addGoal(6, new StudentReturnToOwnerGoal(this, this, 1.35, 28.0, 2.5, 48.0, 20));

        this.goalSelector.addGoal(7, new StudentCombatGoal(this, this));
        this.goalSelector.addGoal(8, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(9, new PanicGoal(this, 1.25));

        this.goalSelector.addGoal(10, new StudentFollowGoal(this, this, 1.1));
        this.goalSelector.addGoal(11, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override public BlockPos getSecurityPos() { return KisakiEntity.this.getSecurityPos(); }
                    @Override public void setSecurityPos(BlockPos pos) { KisakiEntity.this.setSecurityPos(pos); }
                }, 1.0));
        this.goalSelector.addGoal(12, new StudentEatGoal(this, this));
    }

    public void requestBuff() {
        if (this.level().isClientSide()) return;

        this.entityData.set(BUFF_TRIGGER, this.entityData.get(BUFF_TRIGGER) + 1);

        startBuffCasting(20);
        cooldownTicks = COOLDOWN;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (cooldownTicks > 0) cooldownTicks--;

            if (buffCastingTicks > 0) {
                buffCastingTicks--;
                this.getNavigation().stop();
                this.setDeltaMovement(0, 0, 0);
            }
        }

        if (this.level().isClientSide()) {
            int trig = this.entityData.get(BUFF_TRIGGER);

            if (trig != lastBuffTrigger) {
                lastBuffTrigger = trig;
                clientBuffTicks = BUFF_ANIM_TICKS;
            } else if (clientBuffTicks > 0) {
                clientBuffTicks--;
            }
        }
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (this.level().isClientSide() && clientBuffTicks > 0) return BUFF;
        return null;
    }

    public boolean isBuffCasting() {
        return buffCastingTicks > 0;
    }

    public void startBuffCasting(int ticks) {
        this.buffCastingTicks = ticks;
    }
}