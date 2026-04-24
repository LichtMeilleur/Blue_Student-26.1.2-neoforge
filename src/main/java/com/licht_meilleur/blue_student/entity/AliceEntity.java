package com.licht_meilleur.blue_student.entity;

import com.licht_meilleur.blue_student.ai.*;
import com.licht_meilleur.blue_student.ai.br_ai.AliceBrCombatGoal;
import com.licht_meilleur.blue_student.ai.only.AliceHyperShotGoal;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentBrAction;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;

import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import com.geckolib.animation.RawAnimation;

import java.util.EnumSet;
import java.util.UUID;

public class AliceEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(AliceEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> HYPER_TRIGGER =
            SynchedEntityData.defineId(AliceEntity.class, EntityDataSerializers.INT);

    // normal
    public static final String ANIM_HYPER_SHOT = "animation.model.hiper_shot";

    // BR
    public static final String ANIM_HYPER_CANNON_SET = "animation.model.hyper_cannon_set";
    public static final String ANIM_HYPER_CANNON     = "animation.model.hyper_cannon";
    public static final String ANIM_LEFT_MOVE_SHOT   = "animation.model.left_move_shot";
    public static final String ANIM_LEFT_MOVE        = "animation.model.left_move";
    public static final String ANIM_RIGHT_MOVE_SHOT  = "animation.model.right_move_shot";
    public static final String ANIM_RIGHT_MOVE       = "animation.model.right_move";

    private static final RawAnimation HYPER_SHOT = RawAnimation.begin().thenPlay(ANIM_HYPER_SHOT);

    private static final RawAnimation HYPER_CANNON_SET = RawAnimation.begin().thenPlay(ANIM_HYPER_CANNON_SET);
    private static final RawAnimation HYPER_CANNON     = RawAnimation.begin().thenLoop(ANIM_HYPER_CANNON);

    private static final RawAnimation LEFT_MOVE_SHOT  = RawAnimation.begin().thenPlay(ANIM_LEFT_MOVE_SHOT);
    private static final RawAnimation LEFT_MOVE       = RawAnimation.begin().thenLoop(ANIM_LEFT_MOVE);
    private static final RawAnimation RIGHT_MOVE_SHOT = RawAnimation.begin().thenPlay(ANIM_RIGHT_MOVE_SHOT);
    private static final RawAnimation RIGHT_MOVE      = RawAnimation.begin().thenLoop(ANIM_RIGHT_MOVE);

    private int clientHyperTicks = 0;
    private int lastHyperTrigger = 0;
    private static final int HYPER_ANIM_TICKS = 20;

    private StudentForm lastForm = null;

    public AliceEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HYPER_TRIGGER, 0);
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.ALICE;
    }

    @Override
    protected EntityDataAccessor<Integer> getAiModeTrackedData() {
        return AI_MODE;
    }

    public Vec3 getMuzzleLeft() {
        return getMuzzleSide(-1);
    }

    public Vec3 getMuzzleRight() {
        return getMuzzleSide(+1);
    }

    private Vec3 getMuzzleSide(int sideSign) {
        Vec3 base = this.getEyePosition().subtract(0, 0.10, 0);

        float yawRad = (this.getYRot() + 90.0f) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(Mth.cos(yawRad), 0, Mth.sin(yawRad));

        double side = 0.22 * sideSign;
        double forward = 0.12;

        Vec3 forwardVec = this.getViewVector(1.0f).normalize().scale(forward);
        return base.add(right.scale(side)).add(forwardVec);
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.ALICE);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "alice"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new StudentEvadeGoal(this, this));
        this.goalSelector.addGoal(3, new StudentStuckEscapeGoal(this, this));
        this.goalSelector.addGoal(4, new StudentCliffAvoidGoal(this));
        this.goalSelector.addGoal(5, new StudentReturnToOwnerGoal(this, this, 1.35, 28.0, 2.5, 48.0, 20));
        this.goalSelector.addGoal(6, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(7, new AliceHyperShotGoal(this, this));
        this.goalSelector.addGoal(8, new AliceBrCombatGoal(this, this));
        this.goalSelector.addGoal(9, new StudentCombatGoal(this, this));

        this.goalSelector.addGoal(10, new PanicGoal(this, 1.25));

        this.goalSelector.addGoal(11, new StudentFollowGoal(this, this, 1.1));
        this.goalSelector.addGoal(12, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override public BlockPos getSecurityPos() { return AliceEntity.this.getSecurityPos(); }
                    @Override public void setSecurityPos(BlockPos pos) { AliceEntity.this.setSecurityPos(pos); }
                }, 1.0));
        this.goalSelector.addGoal(13, new StudentEatGoal(this, this));
    }

    public void requestHyperShot() {
        if (this.level().isClientSide()) return;
        this.entityData.set(HYPER_TRIGGER, this.entityData.get(HYPER_TRIGGER) + 1);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            StudentForm now = getForm();
            if (lastForm != now) {
                lastForm = now;
                onFormChangedForAlice(now);
            }
        }

        if (this.level().isClientSide()) {
            int trig = this.entityData.get(HYPER_TRIGGER);
            if (trig != lastHyperTrigger) {
                lastHyperTrigger = trig;
                clientHyperTicks = HYPER_ANIM_TICKS;
            } else if (clientHyperTicks > 0) {
                clientHyperTicks--;
            }
        }
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (this.level().isClientSide() && clientHyperTicks > 0) {
            return HYPER_SHOT;
        }
        return null;
    }

    @Override
    protected RawAnimation getBrAnimationForAction(StudentBrAction a) {
        return switch (a) {
            case HYPER_CANNON_SET -> HYPER_CANNON_SET;
            case HYPER_CANNON -> HYPER_CANNON;
            case LEFT_MOVE -> LEFT_MOVE;
            case LEFT_MOVE_SHOT -> LEFT_MOVE_SHOT;
            case RIGHT_MOVE -> RIGHT_MOVE;
            case RIGHT_MOVE_SHOT -> RIGHT_MOVE_SHOT;
            default -> null;
        };
    }

    private void onFormChangedForAlice(StudentForm now) {
        if (now == StudentForm.BR) {
            this.setNoGravity(true);
            this.fallDistance = 0;
            this.setOnGround(false);
            this.getNavigation().stop();
        } else {
            this.setNoGravity(false);
            this.noFallTicks = Math.max(this.noFallTicks, 20);
        }
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }
}