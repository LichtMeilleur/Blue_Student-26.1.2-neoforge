package com.licht_meilleur.blue_student.entity;

import com.geckolib.animation.RawAnimation;
import com.licht_meilleur.blue_student.ai.*;
import com.licht_meilleur.blue_student.ai.only.NozomiHikariMergeGoGoTrainGoal;
import com.licht_meilleur.blue_student.ai.only.NozomiTrainGoal;
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
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class NozomiEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(NozomiEntity.class, EntityDataSerializers.INT);

    public static final String ANIM_TRAIN = "animation.model.train";
    private static final RawAnimation TRAIN_LOOP =
            RawAnimation.begin().thenLoop(ANIM_TRAIN);

    private static final EntityDataAccessor<Boolean> TRAIN_ACTIVE =
            SynchedEntityData.defineId(NozomiEntity.class, EntityDataSerializers.BOOLEAN);

    private int trainSkillCooldown = 0;
    private static final int TRAIN_COOLDOWN_MAX = 20 * 12;

    private int unisonCooldown = 0;
    private static final int UNISON_COOLDOWN_MAX = 20 * 12;

    public NozomiEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRAIN_ACTIVE, false);
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    public void setTrainSkillActive(boolean active) {
        if (this.level().isClientSide()) return;
        this.entityData.set(TRAIN_ACTIVE, active);
    }

    public boolean isTrainSkillActive() {
        return this.entityData.get(TRAIN_ACTIVE);
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (isTrainSkillActive()) return TRAIN_LOOP;
        return null;
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.NOZOMI;
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.NOZOMI);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "nozomi"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new NozomiHikariMergeGoGoTrainGoal(this));
        this.goalSelector.addGoal(3, new NozomiTrainGoal(this));
        this.goalSelector.addGoal(4, new StudentEvadeGoal(this, this));
        this.goalSelector.addGoal(5, new StudentStuckEscapeGoal(this, this));
        this.goalSelector.addGoal(6, new StudentCliffAvoidGoal(this));
        this.goalSelector.addGoal(7, new StudentReturnToOwnerGoal(this, this, 1.35, 28.0, 2.5, 48.0, 20));

        this.goalSelector.addGoal(8, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(9, new StudentCombatGoal(this, this));
        this.goalSelector.addGoal(10, new PanicGoal(this, 1.25));

        this.goalSelector.addGoal(11, new StudentFollowGoal(this, this, 1.1));
        this.goalSelector.addGoal(12, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override public BlockPos getSecurityPos() { return NozomiEntity.this.getSecurityPos(); }
                    @Override public void setSecurityPos(BlockPos pos) { NozomiEntity.this.setSecurityPos(pos); }
                }, 1.0));
        this.goalSelector.addGoal(13, new StudentEatGoal(this, this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (unisonCooldown > 0) unisonCooldown--;
        }

        if (!this.level().isClientSide()) {
            if (trainSkillCooldown > 0) trainSkillCooldown--;
        }
    }

    public boolean canUseTrainSkill() {
        return trainSkillCooldown <= 0;
    }

    public void startTrainCooldown() {
        trainSkillCooldown = TRAIN_COOLDOWN_MAX;
    }

    public boolean canUseUnisonSkill() {
        return unisonCooldown <= 0;
    }

    public void startUnisonCooldown() {
        unisonCooldown = UNISON_COOLDOWN_MAX;
    }
}