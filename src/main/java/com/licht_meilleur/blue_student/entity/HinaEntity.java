package com.licht_meilleur.blue_student.entity;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.ai.StudentAimGoal;
import com.licht_meilleur.blue_student.ai.StudentCombatGoal;
import com.licht_meilleur.blue_student.ai.StudentEatGoal;
import com.licht_meilleur.blue_student.ai.StudentFollowGoal;
import com.licht_meilleur.blue_student.ai.StudentReturnToOwnerGoal;
import com.licht_meilleur.blue_student.ai.StudentRideWithOwnerGoal;
import com.licht_meilleur.blue_student.ai.StudentSecurityGoal;
import com.licht_meilleur.blue_student.ai.StudentStuckEscapeGoal;
import com.licht_meilleur.blue_student.ai.only.HinaAirCombatGoal;
import com.licht_meilleur.blue_student.ai.only.HinaFlyGoal;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.student.StudentAiMode;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class HinaEntity extends AbstractStudentEntity {

    private static final EntityDataAccessor<Integer> AI_MODE =
            SynchedEntityData.defineId(HinaEntity.class, EntityDataSerializers.INT);

    public static final String ANIM_FLY = "animation.model.fly";
    public static final String ANIM_FLY_SHOT = "animation.model.fly_shot";

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop(ANIM_FLY);
    private static final RawAnimation FLY_SHOT = RawAnimation.begin().thenLoop(ANIM_FLY_SHOT);

    private static final int FLY_DURATION_TICKS = 20 * 20;
    private static final int FLY_COOLDOWN_TICKS = 20 * 12;
    private static final double HOVER_MIN_Y_VEL = 0.03;
    private static final double LAND_DESCEND_SPEED = 0.08;
    private static final int LANDING_MAX_TICKS = 20 * 6;

    private int flyActiveTicks = 0;
    private int flyCooldownTicks = 0;
    private int landingTicksLeft = 0;

    private static final EntityDataAccessor<Boolean> FLYING_T =
            SynchedEntityData.defineId(HinaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANDING_T =
            SynchedEntityData.defineId(HinaEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLY_SHOOT_T =
            SynchedEntityData.defineId(HinaEntity.class, EntityDataSerializers.BOOLEAN);

    private int flyShotPulseTicks = 0;
    private int noFallTicks = 0;
    private static final int NO_FALL_GRACE_TICKS = 20 * 10;

    private static final UUID FLY_SPEED_UUID =
            UUID.fromString("f0b8c8a4-2c1f-4c9a-8a3a-6d7c2f3c1b11");

    public HinaEntity(EntityType<? extends AbstractStudentEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    @Override
    public StudentId getStudentId() {
        return StudentId.HINA;
    }

    @Override
    protected EntityDataAccessor<Integer> getAiModeTrackedData() {
        return AI_MODE;
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING_T);
    }

    public boolean isFlyLanding() {
        return this.entityData.get(LANDING_T);
    }

    public boolean isFlyShooting() {
        return this.entityData.get(FLY_SHOOT_T);
    }

    public void setFlyShooting(boolean v) {
        this.entityData.set(FLY_SHOOT_T, v);
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
            BedLinkManager.setLinking(player.getUUID(), StudentId.HINA);
            player.sendSystemMessage(Component.translatable("msg.blue_student.link_mode", "hina"));
            return InteractionResult.CONSUME;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new StudentRideWithOwnerGoal(this, this));
        this.goalSelector.addGoal(1, new FloatGoal(this));

        this.goalSelector.addGoal(2, new StudentStuckEscapeGoal(this, this));
        this.goalSelector.addGoal(3, new PanicGoal(this, 1.25));

        this.goalSelector.addGoal(4, new HinaAirCombatGoal(this, this));
        this.goalSelector.addGoal(5, new StudentCombatGoal(this, this));
        this.goalSelector.addGoal(6, new StudentAimGoal(this, this));
        this.goalSelector.addGoal(7, new HinaFlyGoal(this, this));

        this.goalSelector.addGoal(9, new StudentReturnToOwnerGoal(this, this, 1.35, 28.0, 2.5, 48.0, 20));
        this.goalSelector.addGoal(10, new StudentFollowGoal(this, this, 1.1));

        this.goalSelector.addGoal(11, new StudentSecurityGoal(this, this,
                new StudentSecurityGoal.ISecurityPosProvider() {
                    @Override
                    public BlockPos getSecurityPos() {
                        return HinaEntity.this.getSecurityPos();
                    }

                    @Override
                    public void setSecurityPos(BlockPos pos) {
                        HinaEntity.this.setSecurityPos(pos);
                    }
                },
                1.0));

        this.goalSelector.addGoal(12, new StudentEatGoal(this, this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractStudentEntity.createAttributes()
                .add(Attributes.FLYING_SPEED, 1.8);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            if (flyShotPulseTicks > 0) {
                flyShotPulseTicks--;
                if (flyShotPulseTicks == 0) {
                    this.entityData.set(FLY_SHOOT_T, false);
                }
            }

            if (noFallTicks > 0) {
                noFallTicks--;
            }
        }

        if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
            tickFlySkill(serverLevel);
        }
    }

    private void tickFlySkill(ServerLevel serverLevel) {
        if (this.isLifeLockedForGoal()) {
            stopFlyImmediately();
            flyActiveTicks = 0;
            flyCooldownTicks = 0;
            landingTicksLeft = 0;
            return;
        }

        if (flyCooldownTicks > 0) {
            flyCooldownTicks--;
        }

        if (flyActiveTicks > 0) {
            flyActiveTicks--;

            if (!isFlying()) {
                startFlyInternal();
            }

            keepFlyingPhysics();

            if (flyActiveTicks == 0) {
                startLandingInternal();
            }
            return;
        }

        if (isFlyLanding()) {
            tickLandingInternal();
            return;
        }

        if (flyCooldownTicks <= 0) {
            boolean danger =
                    hasNearbyEnemy(serverLevel)
                            || hasIncomingProjectile(serverLevel)
                            || this.hurtTime > 0;

            if (danger) {
                startFlySkill();
            }
        }
    }

    private void startFlySkill() {
        flyActiveTicks = FLY_DURATION_TICKS;
        startFlyInternal();
    }

    private void startFlyInternal() {
        this.entityData.set(FLYING_T, true);
        this.entityData.set(LANDING_T, false);

        this.setNoGravity(true);
        this.fallDistance = 0.0f;

        this.setPos(this.getX(), this.getY() + 0.12, this.getZ());
        this.setYRot(this.getYRot());
        this.setXRot(this.getXRot());

        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x, Math.max(v.y, 0.42), v.z);

        this.getNavigation().stop();
        applyFlySpeed(true);
    }

    private void keepFlyingPhysics() {
        this.fallDistance = 0.0f;

        this.setPos(this.getX(), this.getY() + 0.10, this.getZ());
        this.setYRot(this.getYRot());
        this.setXRot(this.getXRot());

        Vec3 v = this.getDeltaMovement();
        if (v.y < HOVER_MIN_Y_VEL) {
            this.setDeltaMovement(v.x, HOVER_MIN_Y_VEL, v.z);
        }
    }

    private void startLandingInternal() {
        this.entityData.set(FLYING_T, true);
        this.entityData.set(LANDING_T, true);

        landingTicksLeft = LANDING_MAX_TICKS;
        this.setNoGravity(true);
        this.fallDistance = 0.0f;
    }

    private void tickLandingInternal() {
        this.fallDistance = 0.0f;

        if (landingTicksLeft > 0) {
            landingTicksLeft--;
        } else {
            stopFlyAfterLanded();
            return;
        }

        if (this.onGround()) {
            stopFlyAfterLanded();
            return;
        }

        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x, -LAND_DESCEND_SPEED, v.z);
    }

    private void stopFlyAfterLanded() {
        this.entityData.set(FLYING_T, false);
        this.entityData.set(LANDING_T, false);
        this.entityData.set(FLY_SHOOT_T, false);

        this.setNoGravity(false);
        this.fallDistance = 0.0f;

        flyCooldownTicks = FLY_COOLDOWN_TICKS;
        landingTicksLeft = 0;
        applyFlySpeed(false);
        noFallTicks = NO_FALL_GRACE_TICKS;
    }

    private void stopFlyImmediately() {
        this.entityData.set(FLYING_T, false);
        this.entityData.set(LANDING_T, false);
        this.entityData.set(FLY_SHOOT_T, false);

        this.setNoGravity(false);
        this.fallDistance = 0.0f;
        landingTicksLeft = 0;
        applyFlySpeed(false);
        noFallTicks = NO_FALL_GRACE_TICKS;
    }

    @Override
    protected RawAnimation getOverrideAnimationIfAny() {
        if (isFlying()) {
            if (this.getClientShotTicksForAnim() > 0) {
                return FLY_SHOT;
            }
            return FLY;
        }
        return null;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource source) {
        boolean blocked = this.isFlying() || this.isFlyLanding() || noFallTicks > 0;
        if (blocked) {
            return false;
        }
        return super.causeFallDamage(fallDistance, damageMultiplier, source);
    }

    private boolean hasNearbyEnemy(ServerLevel serverLevel) {
        var box = this.getBoundingBox().inflate(8.0);
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
            var v = p.getDeltaMovement();
            if (v.lengthSqr() < 0.01) {
                continue;
            }

            var toMe = myPos.subtract(p.position());
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING_T, false);
        builder.define(LANDING_T, false);
        builder.define(FLY_SHOOT_T, false);
    }

    private void applyFlySpeed(boolean on) {
        var ms = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (ms == null) return;

        ms.removeModifier(BlueStudentMod.id("hina_fly_speed"));

        if (on) {
            ms.addOrReplacePermanentModifier(new AttributeModifier(
                    BlueStudentMod.id("hina_fly_speed"),
                    0.6,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    public void beginFlyShotPulse(int ticks) {
        if (!isFlying()) {
            return;
        }

        this.entityData.set(FLY_SHOOT_T, true);
        this.flyShotPulseTicks = Math.max(this.flyShotPulseTicks, ticks);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    public void aiStep() {
        boolean air = this.isFlying() || this.isFlyLanding();

        if (air) {
            this.setNoGravity(true);
            this.fallDistance = 0.0f;
        }

        super.aiStep();

        if (!this.level().isClientSide()) {
            if (air) {
                this.setNoGravity(true);
                this.fallDistance = 0.0f;

                Vec3 v = this.getDeltaMovement();

                double maxH = 0.9;
                double maxYUp = 0.45;
                double maxYDown = -0.60;

                double hxz = Math.sqrt(v.x * v.x + v.z * v.z);
                if (hxz > maxH) {
                    double s = maxH / hxz;
                    v = new Vec3(v.x * s, v.y, v.z * s);
                }

                if (v.y > maxYUp) {
                    v = new Vec3(v.x, maxYUp, v.z);
                }
                if (v.y < maxYDown) {
                    v = new Vec3(v.x, maxYDown, v.z);
                }

                this.setDeltaMovement(v);
                this.fallDistance = 0.0f;
            } else {
                this.setNoGravity(false);
            }

            Vec3 v = this.getDeltaMovement();
            double sp = v.length();
            if (sp > 2.0) {
                System.out.println("[HinaFlySpike] sp=" + sp + " v=" + v +
                        " flyingSpeed=" + getAttributeValue(Attributes.FLYING_SPEED) +
                        " moveSpeed=" + getAttributeValue(Attributes.MOVEMENT_SPEED));
            }
        }
    }
}