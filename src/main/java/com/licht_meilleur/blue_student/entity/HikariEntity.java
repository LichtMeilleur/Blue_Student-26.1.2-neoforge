package com.licht_meilleur.blue_student.entity;

import com.licht_meilleur.blue_student.ai.*;
import com.licht_meilleur.blue_student.ai.only.HikariGunTrainGoal;
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
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.geckolib.animation.RawAnimation;

import java.util.UUID;

public class HikariEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(HikariEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Boolean> GUN_TRAIN_ACTIVE =
            SynchedEntityData.defineId(HikariEntity.class, EntityDataSerializers.BOOLEAN);

    public static final String ANIM_GUN_TRAIN = "animation.model.gun_train";
    private static final RawAnimation GUN_TRAIN_LOOP =
            RawAnimation.begin().thenLoop(ANIM_GUN_TRAIN);

    private int gunTrainSkillCooldown = 0;
    private static final int GUNTRAIN_COOLDOWN_MAX = 20 * 12;

    private boolean hardLocked = false;
    private double lockX, lockY, lockZ;
    private float lockYaw;

    public HikariEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GUN_TRAIN_ACTIVE, false);
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.HIKARI;
    }

    @Override
    protected EntityDataAccessor<Integer> getAiModeTrackedData() {
        return AI_MODE;
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        this.noPhysics = false;
        this.setNoGravity(false);
    }

    public void setGunTrainSkillActive(boolean active) {
        if (this.level().isClientSide()) return;
        this.entityData.set(GUN_TRAIN_ACTIVE, active);
    }

    public boolean isGunTrainSkillActive() {
        return this.entityData.get(GUN_TRAIN_ACTIVE);
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (isGunTrainSkillActive()) return GUN_TRAIN_LOOP;
        return null;
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.HIKARI);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "hikari"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new HikariGunTrainGoal(this));
        this.goalSelector.addGoal(3, new StudentEvadeGoal(this, this));
        this.goalSelector.addGoal(4, new StudentStuckEscapeGoal(this, this));
        this.goalSelector.addGoal(5, new StudentCliffAvoidGoal(this));
        this.goalSelector.addGoal(6, new StudentReturnToOwnerGoal(this, this, 1.35, 28.0, 2.5, 48.0, 20));

        this.goalSelector.addGoal(7, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(8, new StudentCombatGoal(this, this));
        this.goalSelector.addGoal(9, new PanicGoal(this, 1.25));

        this.goalSelector.addGoal(10, new StudentFollowGoal(this, this, 1.1));
        this.goalSelector.addGoal(11, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override public BlockPos getSecurityPos() { return HikariEntity.this.getSecurityPos(); }
                    @Override public void setSecurityPos(BlockPos pos) { HikariEntity.this.setSecurityPos(pos); }
                }, 1.0));
        this.goalSelector.addGoal(12, new StudentEatGoal(this, this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (gunTrainSkillCooldown > 0) gunTrainSkillCooldown--;
        }
    }

    public boolean canUseGunTrainSkill() {
        return gunTrainSkillCooldown <= 0;
    }

    public void startGunTrainCooldown() {
        gunTrainSkillCooldown = GUNTRAIN_COOLDOWN_MAX;
    }

    @Override
    public void aiStep() {
        if (hardLocked) {
            this.setDeltaMovement(Vec3.ZERO);
            this.hurtMarked = true;

            this.setNoGravity(true);
            this.noPhysics = false;

            this.setPos(lockX, lockY, lockZ);
            this.setYRot(lockYaw);
            this.setXRot(0f);

            this.yBodyRot = lockYaw;
            this.yHeadRot = lockYaw;
            this.yRotO = lockYaw;
            this.yBodyRotO = lockYaw;
            this.yHeadRotO = lockYaw;

            if (this.getNavigation() != null) this.getNavigation().stop();
            return;
        }

        super.aiStep();
    }
}