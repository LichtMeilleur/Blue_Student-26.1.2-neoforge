package com.licht_meilleur.blue_student.entity;

import com.licht_meilleur.blue_student.ai.StudentAimGoal;
import com.licht_meilleur.blue_student.ai.StudentCombatGoal;
import com.licht_meilleur.blue_student.ai.StudentEatGoal;
import com.licht_meilleur.blue_student.ai.StudentEvadeGoal;
import com.licht_meilleur.blue_student.ai.StudentFollowGoal;
import com.licht_meilleur.blue_student.ai.StudentRideWithOwnerGoal;
import com.licht_meilleur.blue_student.ai.StudentSecurityGoal;
import com.licht_meilleur.blue_student.ai.StudentStuckEscapeGoal;
import com.licht_meilleur.blue_student.ai.br_ai.HoshinoBrCombatGoal;
import com.licht_meilleur.blue_student.ai.br_ai.HoshinoBrMoveGoal;
import com.licht_meilleur.blue_student.ai.only.HoshinoGuardGoal;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.student.StudentAiMode;
import com.licht_meilleur.blue_student.student.StudentBrAction;
import com.licht_meilleur.blue_student.student.StudentForm;
import com.licht_meilleur.blue_student.student.StudentId;
import com.geckolib.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HoshinoEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(HoshinoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TD_GUARDING =
            SynchedEntityData.defineId(HoshinoEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TD_GUARD_SHOOTING =
            SynchedEntityData.defineId(HoshinoEntity.class, EntityDataSerializers.BOOLEAN);

    public static final String ANIM_SHOT = "animation.model.shot";
    public static final String ANIM_GUARD_IDLE = "animation.model.guard_idle";
    public static final String ANIM_GUARD_WALK = "animation.model.guard_walk";
    public static final String ANIM_GUARD_SHOT = "animation.model.guard_shot";

    public static final String ANIM_BR_IDLE = "animation.model.idle";
    public static final String ANIM_BR_RUN = "animation.model.run";

    private static final RawAnimation BR_IDLE = RawAnimation.begin().thenLoop(ANIM_BR_IDLE);
    private static final RawAnimation BR_RUN = RawAnimation.begin().thenLoop(ANIM_BR_RUN);

    public static final String ANIM_DODGE_SHOT = "animation.model.dodge_shot";
    public static final String ANIM_GUARD_TACKLE = "animation.model.guard_tackle";
    public static final String ANIM_GUARD_BASH = "animation.model.guard_bash";
    public static final String ANIM_SUB_RELOAD_SHOT = "animation.model.sub_reload_shot3";
    public static final String ANIM_SUB_SHOT = "animation.model.sub_shot3";
    public static final String ANIM_RIGHT_SIDE_SUB_SHOT = "animation.model.right_side_sub_shot";
    public static final String ANIM_LEFT_SIDE_SUB_SHOT = "animation.model.left_side_sub_shot";

    private static final RawAnimation MAIN_SHOT = RawAnimation.begin().thenPlay(ANIM_SHOT);
    private static final RawAnimation GUARD_IDLE = RawAnimation.begin().thenLoop(ANIM_GUARD_IDLE);
    private static final RawAnimation GUARD_WALK = RawAnimation.begin().thenLoop(ANIM_GUARD_WALK);
    private static final RawAnimation GUARD_SHOT = RawAnimation.begin().thenPlay(ANIM_GUARD_SHOT);

    private static final RawAnimation DODGE_SHOT = RawAnimation.begin().thenPlay(ANIM_DODGE_SHOT);
    private static final RawAnimation GUARD_TACKLE = RawAnimation.begin().thenPlay(ANIM_GUARD_TACKLE);
    private static final RawAnimation GUARD_BASH = RawAnimation.begin().thenPlay(ANIM_GUARD_BASH);
    private static final RawAnimation SUB_RELOAD_SHOT = RawAnimation.begin().thenPlay(ANIM_SUB_RELOAD_SHOT);
    private static final RawAnimation SUB_SHOT = RawAnimation.begin().thenPlay(ANIM_SUB_SHOT);
    private static final RawAnimation RIGHT_SIDE_SUB_SHOT = RawAnimation.begin().thenPlay(ANIM_RIGHT_SIDE_SUB_SHOT);
    private static final RawAnimation LEFT_SIDE_SUB_SHOT = RawAnimation.begin().thenPlay(ANIM_LEFT_SIDE_SUB_SHOT);

    private static final int GUARD_DURATION_TICKS = 60;
    private static final int GUARD_COOLDOWN_TICKS = 100;
    private static final int TAUNT_INTERVAL_TICKS = 10;
    private static final double TAUNT_RADIUS = 12.0;

    private int guardActiveTicks = 0;
    private int guardCooldownTicks = 0;

    private double baseMoveSpeedCached = -1;

    public HoshinoEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected RawAnimation getBrAnimationForAction(StudentBrAction a) {
        return switch (a) {
            case MAIN_SHOT -> MAIN_SHOT;
            case DODGE_SHOT -> DODGE_SHOT;
            case GUARD_TACKLE -> GUARD_TACKLE;
            case GUARD_BASH -> GUARD_BASH;
            case RIGHT_SIDE_SUB_SHOT -> RIGHT_SIDE_SUB_SHOT;
            case LEFT_SIDE_SUB_SHOT -> LEFT_SIDE_SUB_SHOT;
            case SUB_SHOT -> SUB_SHOT;
            case SUB_RELOAD_SHOT -> SUB_RELOAD_SHOT;
            case IDLE -> BR_IDLE;
            default -> null;
        };
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.HOSHINO;
    }



    @Override
    protected EntityDataAccessor<Integer> getAiModeTrackedData() {
        return AI_MODE;
    }

    public boolean isGuarding() {
        return this.entityData.get(TD_GUARDING);
    }

    public boolean isGuardShooting() {
        return this.entityData.get(TD_GUARD_SHOOTING);
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.HOSHINO);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "hoshino"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(3, new HoshinoBrCombatGoal(this, this));
        this.goalSelector.addGoal(4, new HoshinoGuardGoal(this, this));
        this.goalSelector.addGoal(5, new StudentEvadeGoal(this, this));
        this.goalSelector.addGoal(7, new HoshinoBrMoveGoal(this, this));
        this.goalSelector.addGoal(8, new StudentCombatGoal(this, this));
        this.goalSelector.addGoal(9, new StudentStuckEscapeGoal(this, this));

        this.goalSelector.addGoal(10, new StudentFollowGoal(this, this, 1.1));
        this.goalSelector.addGoal(11, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override
                    public BlockPos getSecurityPos() {
                        return HoshinoEntity.this.getSecurityPos();
                    }

                    @Override
                    public void setSecurityPos(BlockPos pos) {
                        HoshinoEntity.this.setSecurityPos(pos);
                    }
                },
                1.0));
        this.goalSelector.addGoal(12, new StudentEatGoal(this, this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            tickGuardSkill(serverLevel);
        }
    }

    private void tickGuardSkill(ServerLevel serverLevel) {
        if (getForm() == StudentForm.BR) {
            if (isGuarding()) {
                setGuardingInternal(false);
            }
            guardActiveTicks = 0;
            guardCooldownTicks = 0;
            return;
        }

        if (this.isLifeLockedForGoal()) {
            if (isGuarding()) {
                setGuardingInternal(false);
            }
            guardActiveTicks = 0;
            guardCooldownTicks = 0;
            return;
        }

        if (guardCooldownTicks > 0) {
            guardCooldownTicks--;
        }

        if (guardActiveTicks > 0) {
            guardActiveTicks--;

            if (this.tickCount % TAUNT_INTERVAL_TICKS == 0) {
                applyTaunt(serverLevel);
            }

            if (!isGuarding()) {
                setGuardingInternal(true);
            }

            if (guardActiveTicks == 0) {
                setGuardingInternal(false);
                guardCooldownTicks = GUARD_COOLDOWN_TICKS;
            }
            return;
        }

        if (guardCooldownTicks <= 0) {
            boolean danger = hasNearbyEnemy(serverLevel) || hasIncomingProjectile(serverLevel);
            if (danger) {
                startGuardSkill(serverLevel);
            }
        }
    }

    private void startGuardSkill(ServerLevel serverLevel) {
        guardActiveTicks = GUARD_DURATION_TICKS;
        setGuardingInternal(true);
        applyTaunt(serverLevel);
    }

    private boolean hasNearbyEnemy(ServerLevel serverLevel) {
        var box = this.getBoundingBox().inflate(2.0);
        return !serverLevel.getEntitiesOfClass(
                Monster.class,
                box,
                e -> e.isAlive()
        ).isEmpty();
    }

    private boolean hasIncomingProjectile(ServerLevel serverLevel) {
        var box = this.getBoundingBox().inflate(8.0);
        var myPos = this.getEyePosition();

        var list = serverLevel.getEntitiesOfClass(
                Projectile.class,
                box,
                p -> p.isAlive()
        );

        for (var p : list) {
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.01) {
                continue;
            }

            Vec3 toMe = myPos.subtract(p.position());
            if (toMe.lengthSqr() < 0.01) {
                continue;
            }

            double dot = v.normalize().dot(toMe.normalize());
            if (dot > 0.85) {
                return true;
            }
        }
        return false;
    }

    private void applyTaunt(ServerLevel serverLevel) {
        var box = this.getBoundingBox().inflate(TAUNT_RADIUS);

        var mobs = serverLevel.getEntitiesOfClass(
                Monster.class,
                box,
                e -> e.isAlive()
        );

        for (var m : mobs) {
            m.setTarget(this);
        }
    }

    private void setGuardingInternal(boolean on) {
        if (!this.level().isClientSide()) {
            this.entityData.set(TD_GUARDING, on);
        }

        if (on) {
            applyGuardBuff(true, 8.0, 6.0, 6.0f);
            setMovementSpeedMultiplier(0.70);
        } else {
            applyGuardBuff(false, 0, 0, 0);
            setMovementSpeedMultiplier(1.00);
            if (!this.level().isClientSide()) {
                this.entityData.set(TD_GUARD_SHOOTING, false);
            }
        }
    }

    private void setMovementSpeedMultiplier(double mul) {
        var inst = getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst == null) {
            return;
        }

        if (baseMoveSpeedCached < 0) {
            baseMoveSpeedCached = inst.getBaseValue();
            if (baseMoveSpeedCached <= 0) {
                baseMoveSpeedCached = 0.35;
            }
        }

        inst.setBaseValue(baseMoveSpeedCached * mul);
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (getForm() == StudentForm.BR) {
            return null;
        }

        if (!isGuarding()) {
            return null;
        }

        if (isGuardShooting()) {
            return GUARD_SHOT;
        }

        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 0.002;
        return moving ? GUARD_WALK : GUARD_IDLE;
    }

    public void setGuardShooting(boolean v) {
        if (!this.level().isClientSide()) {
            this.entityData.set(TD_GUARD_SHOOTING, v);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TD_GUARDING, false);
        builder.define(TD_GUARD_SHOOTING, false);
    }
}