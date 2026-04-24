package com.licht_meilleur.blue_student.entity;

import com.geckolib.animation.RawAnimation;
import com.licht_meilleur.blue_student.ai.*;
import com.licht_meilleur.blue_student.ai.only.ShirokoDroneGoal;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.student.IStudentEntity;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ShirokoEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(ShirokoEntity.class, EntityDataSerializers.INT);

    public static final String ANIM_DRONE_START = "animation.model.drone_start";
    private static final RawAnimation DRONE_START = RawAnimation.begin().thenPlay(ANIM_DRONE_START);

    private static final EntityDataAccessor<Integer> DRONE_START_TRIGGER =
            SynchedEntityData.defineId(ShirokoEntity.class, EntityDataSerializers.INT);

    private int clientDroneStartTicks = 0;
    private int lastDroneStartTrigger = 0;
    private static final int DRONE_START_ANIM_TICKS = 20;

    public ShirokoEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DRONE_START_TRIGGER, 0);
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.SHIROKO;
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.SHIROKO);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "shiroko"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ShirokoDroneGoal(this));
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
                    @Override
                    public BlockPos getSecurityPos() {
                        return ShirokoEntity.this.getSecurityPos();
                    }

                    @Override
                    public void setSecurityPos(BlockPos pos) {
                        ShirokoEntity.this.setSecurityPos(pos);
                    }
                },
                1.0));
        this.goalSelector.addGoal(12, new StudentEatGoal(this, this));
    }

    public void requestDroneStart() {
        if (this.level().isClientSide()) return;
        this.entityData.set(DRONE_START_TRIGGER, this.entityData.get(DRONE_START_TRIGGER) + 1);
    }

    @Override
    public void requestShot(IStudentEntity.ShotKind kind, LivingEntity target) {
        super.requestShot(kind);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            int trig = this.entityData.get(DRONE_START_TRIGGER);
            if (trig != lastDroneStartTrigger) {
                lastDroneStartTrigger = trig;
                clientDroneStartTicks = DRONE_START_ANIM_TICKS;
            } else if (clientDroneStartTicks > 0) {
                clientDroneStartTicks--;
            }
        }
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (this.level().isClientSide() && clientDroneStartTicks > 0) {
            return DRONE_START;
        }
        return null;
    }
}