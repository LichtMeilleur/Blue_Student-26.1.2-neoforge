package com.licht_meilleur.blue_student.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.bed.BedLinkManager;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import com.licht_meilleur.blue_student.inventory.StudentInventory;
import com.licht_meilleur.blue_student.inventory.StudentMenuData;
import com.licht_meilleur.blue_student.inventory.StudentScreenHandler;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.*;
import com.licht_meilleur.blue_student.weapon.WeaponSpec;
import com.licht_meilleur.blue_student.weapon.WeaponSpecs;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.IContainerFactory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.UUID;

public abstract class AbstractStudentEntity extends PathfinderMob implements IStudentEntity, GeoEntity {

    private static final int STUDENT_DATA_VERSION = 1;

    public static final String ANIM_IDLE   = "animation.model.idle";
    public static final String ANIM_RUN    = "animation.model.run";
    public static final String ANIM_SHOT   = "animation.model.shot";
    public static final String ANIM_RELOAD = "animation.model.reload";
    public static final String ANIM_SLEEP  = "animation.model.sleep";
    public static final String ANIM_JUMP   = "animation.model.jump";
    public static final String ANIM_DODGE  = "animation.model.dodge";
    public static final String ANIM_SWIM   = "animation.model.swim";
    public static final String ANIM_SIT    = "animation.model.sit";
    public static final String ANIM_FALL   = "animation.model.fall";
    public static final String ANIM_EXIT   = "animation.model.exit";
    public static final String ANIM_ACTION = "animation.model.action";

    private static final EntityDataAccessor<Integer> SHOT_TRIGGER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELOAD_TRIGGER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DODGE_TRIGGER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EAT_TRIGGER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Float> AIM_YAW =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_PITCH =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Integer> LIFE_STATE =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> FORM_ID =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BR_ACTION_ID =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BR_ACTION_HOLD =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_SHOT_KIND =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BR_ACTION_VER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SHOT_VER =
            SynchedEntityData.defineId(AbstractStudentEntity.class, EntityDataSerializers.INT);

    private int lastShotVerClient = -1;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE   = RawAnimation.begin().thenLoop(ANIM_IDLE);
    private static final RawAnimation RUN    = RawAnimation.begin().thenLoop(ANIM_RUN);
    private static final RawAnimation SHOT   = RawAnimation.begin().thenLoop(ANIM_SHOT);
    private static final RawAnimation RELOAD = RawAnimation.begin().thenPlay(ANIM_RELOAD);
    private static final RawAnimation SLEEP  = RawAnimation.begin().thenLoop(ANIM_SLEEP);
    private static final RawAnimation EXIT   = RawAnimation.begin().thenPlay(ANIM_EXIT);
    private static final RawAnimation DODGE  = RawAnimation.begin().thenPlay(ANIM_DODGE);
    private static final RawAnimation SWIM   = RawAnimation.begin().thenLoop(ANIM_SWIM);
    private static final RawAnimation SIT    = RawAnimation.begin().thenPlay(ANIM_SIT);
    private static final RawAnimation JUMP   = RawAnimation.begin().thenPlay(ANIM_JUMP);
    private static final RawAnimation FALL   = RawAnimation.begin().thenLoop(ANIM_FALL);
    private static final RawAnimation ACTION = RawAnimation.begin().thenPlay(ANIM_ACTION);


    private static final int DODGE_ANIM_TICKS = 10;
    private static final int JUMP_ANIM_TICKS  = 8;
    private static final int EAT_ANIM_TICKS   = 16;


    @Nullable
    protected RawAnimation getOverrideAnimationIfAny() {
        return null;
    }

    protected StudentLifeState lifeState = StudentLifeState.NORMAL;
    protected BlockPos securityPos;
    protected UUID ownerUuid;

    protected final StudentInventory studentInventory = new StudentInventory(9, this::onStudentInventoryChanged);

    protected int ammoInMag = 0;
    protected int reloadTicksLeft = 0;
    protected boolean ammoInitDone = false;

    private final EnumMap<IStudentEntity.FireChannel, ArrayDeque<UUID>> queuedFire =
            new EnumMap<>(IStudentEntity.FireChannel.class);
    private IStudentEntity.FireChannel lastConsumedChannel = IStudentEntity.FireChannel.MAIN;

    private boolean forcedSecurityBecauseOwnerOffline = false;
    private boolean appliedStats = false;
    private boolean guardBuffApplied = false;
    private boolean kisakiBuffApplied = false;
    private boolean evading = false;
    private boolean ghost = false;
    private boolean lookMoveDir = false;
    private boolean packedForDimTransfer = false;
    private boolean dimTransferQueued = false;
    private boolean owRespawnQueued = false;

    private int kisakiSupportTicks = 0;
    private int lifeTimer = 0;
    private int eatingSlot = -1;
    private int eatingServerTicks = 0;
    private int skillCooldownTicks = 0;
    private int skillActiveTicksLeft = 0;
    private int skillTrigger = 0;
    private int lookMoveDirPriority = 0;
    private int lookMoveDirTicks = 0;
    private int dimTransferCooldown = 0;
    private int owRespawnCooldown = 0;
    public int noFallTicks = 0;

    private int clientShotTicks = 0;
    private int lastShotTrigger = 0;
    private boolean shotJustStarted = false;

    private int clientReloadTicks = 0;
    private int lastReloadTrigger = 0;
    private boolean reloadJustStarted = false;

    private int clientDodgeTicks = 0;
    private int lastDodgeTrigger = 0;
    private boolean dodgeJustStarted = false;

    private int clientJumpTicks = 0;
    private boolean wasOnGroundClient = true;

    private int clientEatTicks = 0;
    private int lastEatTrigger = 0;
    private boolean eatJustStarted = false;

    private int clientShotHoldTicks = 0;

    private boolean bs_wasOnGround = true;

    private final LookRequest lookReq = new LookRequest();

    private BlockPos respawnBedFoot;
    private BlockPos respawnSafePos;

    private StudentBrAction lastBrActionClient = StudentBrAction.NONE;
    private int brHoldTicksClient = 0;
    private int lastBrActionVerClient = -1;

    private Vec3 clientMuzzleWorldPos;
    private Vec3 clientSubMuzzleWorldPos;
    private Vec3 clientLeftSubMuzzleWorldPos;
    private Vec3 clientRightSubMuzzleWorldPos;

    private int shotRestartGapTicks = 0;

    private StudentBrAction lastLoggedBrAction = null;
    private String lastLoggedBrAnim = null;

    protected AbstractStudentEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);

        //this.maxUpStep(1.0f);
        this.setPathfindingMalus(PathType.LAVA, 80.0f);
        //this.setPathfindingMalus(PathType.DAMAGE_FIRE, 40.0f);
        //this.setPathfindingMalus(PathType.DANGER_FIRE, 20.0f);
        for (IStudentEntity.FireChannel ch : IStudentEntity.FireChannel.values()) {
            queuedFire.put(ch, new ArrayDeque<>());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(SHOT_TRIGGER, 0);
        builder.define(RELOAD_TRIGGER, 0);
        builder.define(DODGE_TRIGGER, 0);
        builder.define(EAT_TRIGGER, 0);

        builder.define(AIM_YAW, 0f);
        builder.define(AIM_PITCH, 0f);

        builder.define(LIFE_STATE, StudentLifeState.NORMAL.ordinal());
        builder.define(getAiModeTrackedData(), getDefaultAiMode().id);

        builder.define(FORM_ID, 0);
        builder.define(BR_ACTION_ID, StudentBrAction.NONE.ordinal());
        builder.define(BR_ACTION_HOLD, 0);
        builder.define(LAST_SHOT_KIND, 0);
        builder.define(BR_ACTION_VER, 0);

        builder.define(SHOT_VER, 0);

    }

    @Override
    public abstract StudentId getStudentId();

    protected abstract EntityDataAccessor<Integer> getAiModeTrackedData();

    protected int getBrActionHoldTicks() {
        return 3;
    }

    protected EnumSet<StudentAiMode> getAllowedAiModes() {
        StudentAiMode[] allowed = getStudentId().getAllowedAis();
        EnumSet<StudentAiMode> set = EnumSet.noneOf(StudentAiMode.class);
        if (allowed != null) {
            for (StudentAiMode mode : allowed) {
                set.add(mode);
            }
        }
        if (set.isEmpty()) {
            set.add(StudentAiMode.FOLLOW);
        }
        return set;
    }

    protected StudentAiMode getDefaultAiMode() {
        StudentAiMode[] allowed = getStudentId().getAllowedAis();
        return (allowed != null && allowed.length > 0) ? allowed[0] : StudentAiMode.FOLLOW;
    }

    @Override
    public void setOwnerUuid(UUID uuid) {
        if (uuid == null) {
            return;
        }

        if (this.ownerUuid != null && !this.ownerUuid.equals(uuid)) {
            BlueStudentMod.LOGGER.error(
                    "[BlueStudent] BLOCK owner overwrite student={} entityId={} uuid={} oldOwner={} newOwner={}",
                    this.getStudentId().asString(),
                    this.getId(),
                    this.getUUID(),
                    this.ownerUuid,
                    uuid
            );
            new Exception("[BlueStudent] owner overwrite trace").printStackTrace();
            return;
        }

        this.ownerUuid = uuid;
        onStudentInventoryChanged();
    }

    @Override
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Player getOwnerPlayer() {
        if (ownerUuid == null || !(level() instanceof ServerLevel sw)) return null;
        return sw.getPlayerByUUID(ownerUuid);
    }
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putInt("DataVersion", STUDENT_DATA_VERSION);

        if (this.ownerUuid != null) {
            output.putString("Owner", this.ownerUuid.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        int dataVersion = input.getInt("DataVersion").orElse(0);

        this.ownerUuid = input.getString("Owner")
                .map(UUID::fromString)
                .orElse(null);
    }

    @Override
    public StudentAiMode getAiMode() {
        return StudentAiMode.fromId(this.entityData.get(getAiModeTrackedData()));
    }

    @Override
    public void setAiMode(StudentAiMode mode) {
        if (!getAllowedAiModes().contains(mode)) return;
        this.entityData.set(getAiModeTrackedData(), mode.id);
    }

    @Override
    public StudentInventory getStudentInventory() {
        return studentInventory;
    }

    protected void onStudentInventoryChanged() {
    }

    @Override
    public void requestShot(IStudentEntity.ShotKind kind, LivingEntity target) {
        requestShot(kind);
    }

    public void requestShot(IStudentEntity.ShotKind kind) {
        if (level().isClientSide()) return;

        entityData.set(SHOT_TRIGGER, entityData.get(SHOT_TRIGGER) + 1);
        entityData.set(SHOT_VER, entityData.get(SHOT_VER) + 1);
    }

    @Override
    public void requestReload() {
        if (level().isClientSide()) return;
        entityData.set(RELOAD_TRIGGER, entityData.get(RELOAD_TRIGGER) + 1);
    }

    public void requestDodge() {
        if (level().isClientSide()) return;
        entityData.set(DODGE_TRIGGER, entityData.get(DODGE_TRIGGER) + 1);
    }

    public void requestJump() {
        if (level().isClientSide()) return;
        entityData.set(DODGE_TRIGGER, entityData.get(DODGE_TRIGGER) + 1);
    }

    protected void requestEat() {
        if (level().isClientSide()) return;
        entityData.set(EAT_TRIGGER, entityData.get(EAT_TRIGGER) + 1);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }


        if (ownerUuid == null) {
            setOwnerUuid(player.getUUID());
        } else if (!ownerUuid.equals(player.getUUID())) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer sp) {
            openStudentCard(sp);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.CONSUME;
    }

    public void openStudentCard(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ownerUuid == null || !ownerUuid.equals(player.getUUID())) {
            return;
        }

        final var self = this;

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (syncId, inv, p) ->
                                new StudentScreenHandler(syncId, inv, self),
                        self.getDisplayName()
                ),
                buf -> StudentMenuData.STREAM_CODEC.encode(buf, new StudentMenuData(self.getId()))
        );
    }

    protected double getSleepForwardOffset() {
        return 0.7;
    }

    protected double getSleepSideOffset() {
        return 0.0;
    }

    protected double getSleepYOffset() {
        return 0.3;
    }

    protected Vec3 getSleepPos(ServerLevel sw, BlockPos bedFoot) {
        BlockState st = sw.getBlockState(bedFoot);
        if (!(st.getBlock() instanceof OnlyBedBlock) || !st.hasProperty(OnlyBedBlock.FACING)) {
            return new Vec3(bedFoot.getX() + 0.5, bedFoot.getY() + getSleepYOffset(), bedFoot.getZ() + 0.5);
        }

        Direction dir = st.getValue(OnlyBedBlock.FACING);
        Vec3 fwd = new Vec3(dir.getStepX(), 0, dir.getStepZ()).normalize();
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 base = new Vec3(bedFoot.getX() + 0.5, bedFoot.getY() + getSleepYOffset(), bedFoot.getZ() + 0.5);

        return base.add(fwd.scale(getSleepForwardOffset())).add(right.scale(getSleepSideOffset()));
    }

    public int getClientShotTicksForAnim() {
        if (!level().isClientSide()) return 0;
        return clientShotTicks + clientShotHoldTicks;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            tickClientOnly();
            return;
        }

        tickBrActionTimer();

        WeaponSpec spec = WeaponSpecs.forStudent(getStudentId());
        if (!ammoInitDone) {
            ammoInMag = spec.magSize;
            ammoInitDone = true;
        }

        if (!appliedStats) {
            appliedStats = true;
            applyStatsFromStudentId();
        }

        if (tickCount % 5 == 0) {
            tryPickupNearbyItems();
        }

        tickLifeStateServer();
        tickFormFromEquipment();

        if (tickCount % 20 == 0) {
            tickFormFromEquipment();
        }

        if (eatingServerTicks > 0) {
            eatingServerTicks--;
            if (eatingServerTicks <= 0) {
                eatingSlot = -1;
            }
        }

        tickSkillCommon();

        if (dimTransferCooldown > 0) dimTransferCooldown--;
        if (owRespawnCooldown > 0) owRespawnCooldown--;

        handleFollowDimTransfer();

        /*
        Player owner = getOwnerPlayer();
        if (owner instanceof ServerPlayer sp) {
            if (!isLifeLockedForGoal() && getAiMode() == StudentAiMode.FOLLOW) {
                if (sp.level() != level()) {
                    queueTeleportToOwnerDimension(sp);
                }
            }
        }
        */
        if (tickCount % 20 == 0 && level() instanceof ServerLevel sw) {
            StudentWorldState st = StudentWorldState.get(sw);

            CompoundTag backup = new CompoundTag();
            saveStudentDataToTagForTransfer(backup);
            st.setPacked(getStudentId(), backup);

            // 生きているのでpacked扱いにはしない
            st.setPackedFlag(getStudentId(), false);
        }

        if (tickCount % 40 == 0 && level() instanceof ServerLevel sw) {
            Player owner = getOwnerPlayer();
            if (owner != null) {
                double dist = distanceToSqr(owner);

                if (owner.level() != level() || dist > 256 * 256) {
                    StudentWorldState.get(sw).markMissing(getStudentId(), sw);
                }
            }
        }


        if (ownerUuid != null) {
            boolean ownerOnline = getOwnerPlayer() != null;
            if (!isLifeLocked()) {
                if (!ownerOnline) {
                    if (!forcedSecurityBecauseOwnerOffline) {
                        forcedSecurityBecauseOwnerOffline = true;
                        setAiMode(StudentAiMode.SECURITY);
                        if (securityPos == null) {
                            securityPos = blockPosition();
                        }
                    }
                } else if (forcedSecurityBecauseOwnerOffline) {
                    forcedSecurityBecauseOwnerOffline = false;
                    setAiMode(StudentAiMode.FOLLOW);
                }
            }
        }

        tickLookPolicies();

        if (tickCount % 5 == 0 && level() instanceof ServerLevel sw) {
            StudentWorldState state = StudentWorldState.get(sw);
            state.updatePos(getStudentId(), sw, blockPosition());
        }
    }



    private void tickClientOnly() {
        if (kisakiSupportTicks > 0) {
            kisakiSupportTicks--;
            if (kisakiSupportTicks == 0) {
                applyKisakiSupportBuff(false, 0, 0, 0);
            }
        }

        if (brHoldTicksClient > 0) {
            brHoldTicksClient--;
        }

        int shotTrig = entityData.get(SHOT_TRIGGER);
        int shotVer = entityData.get(SHOT_VER);

        if (shotTrig != lastShotTrigger || shotVer != lastShotVerClient) {
            lastShotTrigger = shotTrig;
            lastShotVerClient = shotVer;

            WeaponSpec spec = WeaponSpecs.forStudent(getStudentId());

            clientShotTicks = Math.max(2, spec.animShotHoldTicks);
            clientShotHoldTicks = 4;
            shotJustStarted = true;
        } else {
            if (clientShotTicks > 0) {
                clientShotTicks--;
                if (clientShotTicks == 0) {
                    clientShotHoldTicks = 4;
                }
            } else if (clientShotHoldTicks > 0) {
                clientShotHoldTicks--;
            }
        }

        int reloadTrig = entityData.get(RELOAD_TRIGGER);
        if (reloadTrig != lastReloadTrigger) {
            lastReloadTrigger = reloadTrig;
            WeaponSpec spec = WeaponSpecs.forStudent(getStudentId());
            clientReloadTicks = Math.max(1, spec.reloadTicks);
            reloadJustStarted = true;
        } else if (clientReloadTicks > 0) {
            clientReloadTicks--;
        }

        int dodgeTrig = entityData.get(DODGE_TRIGGER);
        if (dodgeTrig != lastDodgeTrigger) {
            lastDodgeTrigger = dodgeTrig;
            clientDodgeTicks = DODGE_ANIM_TICKS;
            dodgeJustStarted = true;
        } else if (clientDodgeTicks > 0) {
            clientDodgeTicks--;
        }

        int eatTrig = entityData.get(EAT_TRIGGER);
        if (eatTrig != lastEatTrigger) {
            lastEatTrigger = eatTrig;
            clientEatTicks = EAT_ANIM_TICKS;
            eatJustStarted = true;
        } else if (clientEatTicks > 0) {
            clientEatTicks--;
        }

        boolean onGroundNow = onGround();
        if (wasOnGroundClient && !onGroundNow) {
            if (getDeltaMovement().y > 0.02) {
                clientJumpTicks = JUMP_ANIM_TICKS;
            }
        }
        wasOnGroundClient = onGroundNow;
        if (clientJumpTicks > 0) clientJumpTicks--;
    }

    private void applyStatsFromStudentId() {
        StudentId id = getStudentId();

        AttributeInstance mh = getAttribute(Attributes.MAX_HEALTH);
        if (mh != null) {
            mh.setBaseValue(id.getBaseMaxHp());
        }

        AttributeInstance ar = getAttribute(Attributes.ARMOR);
        if (ar != null) {
            ar.setBaseValue(id.getBaseDefense());
        }

        if (!isLifeLocked()) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    protected void applyGuardBuff(boolean on, double addArmor, double addMaxHp, float healOnApply) {
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        AttributeInstance maxHp = getAttribute(Attributes.MAX_HEALTH);

        if (on) {
            if (guardBuffApplied) return;
            guardBuffApplied = true;

            if (armor != null) {
                armor.removeModifier(BlueStudentMod.id("guard_armor"));
                armor.addOrReplacePermanentModifier(new AttributeModifier(
                        BlueStudentMod.id("guard_armor"),
                        addArmor,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            if (maxHp != null) {
                maxHp.removeModifier(BlueStudentMod.id("guard_maxhp"));
                maxHp.addOrReplacePermanentModifier(new AttributeModifier(
                        BlueStudentMod.id("guard_maxhp"),
                        addMaxHp,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            float newMax = getMaxHealth();
            if (healOnApply > 0 && getHealth() < newMax) {
                setHealth(Math.min(newMax, getHealth() + healOnApply));
            }
        } else {
            if (!guardBuffApplied) return;
            guardBuffApplied = false;

            if (armor != null) armor.removeModifier(BlueStudentMod.id("guard_armor"));
            if (maxHp != null) maxHp.removeModifier(BlueStudentMod.id("guard_maxhp"));

            if (getHealth() > getMaxHealth()) {
                setHealth(getMaxHealth());
            }
        }
    }

    private boolean isLifeLocked() {
        return lifeState == StudentLifeState.EXITING
                || lifeState == StudentLifeState.RESPAWN_DELAY
                || lifeState == StudentLifeState.WARPING_TO_BED
                || lifeState == StudentLifeState.SLEEPING
                || lifeState == StudentLifeState.RECOVERING;
    }

    private void forceWakeUp(ServerLevel sw, @Nullable BlockPos fallbackPos, boolean tryTurnOffBedAnim) {
        if (tryTurnOffBedAnim && respawnBedFoot != null) {
            Entity blockEntity = null;
            var be = sw.getBlockEntity(respawnBedFoot);
            if (be instanceof OnlyBedBlockEntity obe) {
                obe.setSleepAnim(false);
            }
        }

        BlockPos out = (respawnSafePos != null) ? respawnSafePos
                : (fallbackPos != null ? fallbackPos : blockPosition());

        setPos(out.getX() + 0.5, out.getY(), out.getZ() + 0.5);
        setYRot(getYRot());
        setXRot(getXRot());
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();

        setGhost(false);
        setNoAi(false);
        setNoGravity(false);
        setInvulnerable(false);

        setLifeState(StudentLifeState.NORMAL);
        lifeTimer = 0;
        respawnBedFoot = null;
        respawnSafePos = null;
    }

    private void tickLifeStateServer() {
        if (!(level() instanceof ServerLevel sw)) return;

        if (isLifeLocked()) {
            setNoAi(true);
            setNoGravity(true);
            setGhost(true);
            setInvulnerable(false);
        }

        if (isLifeLocked() && respawnBedFoot != null) {
            ServerLevel ow = sw.getServer().overworld();

            // まだオーバーワールドでなければ、まず移動だけ行う
            if (ow != null && sw != ow) {
                StudentWorldState st = StudentWorldState.get(sw.getServer());

                CompoundTag packed = new CompoundTag();
                saveStudentDataToTagForTransfer(packed);
                st.setPacked(getStudentId(), packed);
                st.markRespawning(getStudentId(), sw, 100);

                discard();
                return;
            }

            // ここから先はオーバーワールドにいる前提でベッド確認
            if (ow != null) {
                boolean bedOk = isValidLinkedBed(ow, respawnBedFoot);
                if (!bedOk) {
                    StudentWorldState.get(sw.getServer()).clearBed(getStudentId());
                    forceWakeUp(sw, blockPosition(), true);
                    return;
                }
            }
        }

        switch (lifeState) {
            case NORMAL -> {
                setGhost(false);
                setNoAi(false);
                setNoGravity(false);
                setInvulnerable(false);
            }
            case EXITING -> {
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();

                if (lifeTimer > 0) {
                    lifeTimer--;
                    return;
                }

                setLifeState(StudentLifeState.RESPAWN_DELAY);
                lifeTimer = 10;
            }
            case RESPAWN_DELAY -> {
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();

                if (lifeTimer > 0) {
                    lifeTimer--;
                    return;
                }

                setLifeState(StudentLifeState.WARPING_TO_BED);
            }
            case WARPING_TO_BED -> {
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();

                Vec3 p = getSleepPos(sw, respawnBedFoot);
                float yaw = getBedYaw(sw, respawnBedFoot);
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                setPos(p.x, p.y, p.z);

                var be = sw.getBlockEntity(respawnBedFoot);
                if (be instanceof OnlyBedBlockEntity obe) {
                    obe.setSleepAnim(true);
                }

                setLifeState(StudentLifeState.SLEEPING);
            }
            case SLEEPING -> {
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();

                Vec3 p = getSleepPos(sw, respawnBedFoot);
                float yaw = getBedYaw(sw, respawnBedFoot);
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                setPos(p.x, p.y, p.z);

                setLifeState(StudentLifeState.RECOVERING);
            }
            case RECOVERING -> {
                setDeltaMovement(Vec3.ZERO);
                getNavigation().stop();

                Vec3 p = getSleepPos(sw, respawnBedFoot);
                float yaw = getBedYaw(sw, respawnBedFoot);
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
                setPos(p.x, p.y, p.z);

                if (tickCount % 30 == 0) {
                    heal(1f);
                }

                if (getHealth() >= getMaxHealth()) {
                    forceWakeUp(sw, blockPosition(), true);
                }
            }
            default -> setLifeState(StudentLifeState.NORMAL);
        }
    }

    protected void tryPickupNearbyItems() {
        AABB box = getBoundingBox().inflate(2.5);
        var items = level().getEntitiesOfClass(ItemEntity.class, box, it ->
                it.isAlive() && !it.getItem().isEmpty() && distanceToSqr(it) < 4.0
        );

        for (ItemEntity it : items) {
            ItemStack remain = it.getItem().copy();
            remain = insertIntoStudentInventory(remain);

            if (remain.isEmpty()) {
                it.discard();
            } else {
                it.setItem(remain);
            }
            break;
        }
    }

    protected ItemStack insertIntoStudentInventory(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        for (int i = 0; i < studentInventory.getContainerSize(); i++) {
            ItemStack cur = studentInventory.getItem(i);
            if (cur.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(cur, stack)) continue;

            int space = cur.getMaxStackSize() - cur.getCount();
            if (space <= 0) continue;

            int move = Math.min(space, stack.getCount());
            cur.grow(move);
            stack.shrink(move);
            if (stack.isEmpty()) {
                studentInventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        for (int i = 0; i < studentInventory.getContainerSize(); i++) {
            if (studentInventory.getItem(i).isEmpty()) {
                studentInventory.setItem(i, stack.copy());
                stack = ItemStack.EMPTY;
                studentInventory.setChanged();
                return ItemStack.EMPTY;
            }
        }

        studentInventory.setChanged();
        return stack;
    }

    private void setGhost(boolean value) {
        ghost = value;
        noPhysics = value;
    }

    @Override
    public boolean isAttackable() {
        return !isLifeLocked() && super.isAttackable();
    }

    @Override
    public boolean isPushable() {
        return !ghost && !isLifeLocked() && super.isPushable();
    }

    private void setLifeState(StudentLifeState state) {
        lifeState = state;
        if (!level().isClientSide()) {
            entityData.set(LIFE_STATE, state.ordinal());
        }
    }

    private StudentLifeState getLifeStateClientSafe() {
        if (level().isClientSide()) {
            int idx = entityData.get(LIFE_STATE);
            idx = Mth.clamp(idx, 0, StudentLifeState.values().length - 1);
            return StudentLifeState.values()[idx];
        }
        return lifeState;
    }

    protected float getBedYaw(ServerLevel world, BlockPos bedFoot) {
        BlockState st = world.getBlockState(bedFoot);
        if (!(st.getBlock() instanceof OnlyBedBlock) || !st.hasProperty(OnlyBedBlock.FACING)) {
            return getYRot();
        }
        Direction dir = st.getValue(OnlyBedBlock.FACING);
        return dir.getOpposite().toYRot();
    }

    private boolean isValidLinkedBed(ServerLevel sw, @Nullable BlockPos bedFoot) {
        if (sw == null || bedFoot == null) return false;

        BlockState foot = sw.getBlockState(bedFoot);
        if (!(foot.getBlock() instanceof OnlyBedBlock)) return false;
        if (!foot.hasProperty(OnlyBedBlock.PART) || !foot.hasProperty(OnlyBedBlock.STUDENT) || !foot.hasProperty(OnlyBedBlock.FACING)) return false;
        if (foot.getValue(OnlyBedBlock.PART) != BedPart.FOOT) return false;
        if (foot.getValue(OnlyBedBlock.STUDENT) != getStudentId()) return false;

        Direction facing = foot.getValue(OnlyBedBlock.FACING);
        BlockPos headPos = bedFoot.relative(facing);
        BlockState head = sw.getBlockState(headPos);

        if (!(head.getBlock() instanceof OnlyBedBlock)) return false;
        if (!head.hasProperty(OnlyBedBlock.PART) || !head.hasProperty(OnlyBedBlock.STUDENT) || !head.hasProperty(OnlyBedBlock.FACING)) return false;
        if (head.getValue(OnlyBedBlock.PART) != BedPart.HEAD) return false;
        if (head.getValue(OnlyBedBlock.STUDENT) != getStudentId()) return false;
        return head.getValue(OnlyBedBlock.FACING) == facing;
    }

    @Override
    public int getAmmoInMag() {
        return ammoInMag;
    }

    @Override
    public void consumeAmmo(int amount) {
        if (amount <= 0) return;
        ammoInMag = Math.max(0, ammoInMag - amount);
    }

    @Override
    public boolean isReloading() {
        return reloadTicksLeft > 0;
    }

    @Override
    public void startReload(WeaponSpec spec) {
        if (spec.reloadTicks <= 0 || isReloading() || spec.infiniteAmmo) return;
        reloadTicksLeft = spec.reloadTicks;
        requestReload();
    }

    @Override
    public void tickReload(WeaponSpec spec) {
        if (!isReloading()) return;

        reloadTicksLeft--;
        if (reloadTicksLeft <= 0) {
            reloadTicksLeft = 0;
            ammoInMag = spec.magSize;
        }
    }
    private RawAnimation resolveMainAnimation() {
        StudentLifeState ls = getLifeStateClientSafe();

        if (ls == StudentLifeState.EXITING) return EXIT;
        if (ls == StudentLifeState.WARPING_TO_BED) return IDLE;
        if (ls == StudentLifeState.SLEEPING || ls == StudentLifeState.RECOVERING) return SLEEP;

        RawAnimation ov = getOverrideAnimationIfAny();
        if (ov != null) return ov;

        if (clientEatTicks > 0) return ACTION;
        if (clientDodgeTicks > 0 && getForm() != StudentForm.BR) return DODGE;

        if (isPassenger()) return SIT;
        if (isInWater()) return SWIM;
        if (clientJumpTicks > 0) return JUMP;
        if (!onGround() && getDeltaMovement().y < -0.08) return FALL;

        if (getForm() == StudentForm.BR) {
            if (!onGround() && getDeltaMovement().y < -0.08) {
                //logBrAnim(StudentBrAction.NONE, "FALL");
                return FALL;
            }

            StudentBrAction action = getBrActionForAnimationClient();
            if (action != StudentBrAction.NONE) {
                RawAnimation brAnim = getBrAnimationForAction(action);
                if (brAnim != null) {
                   // logBrAnim(action, getBrAnimationName(action));
                    return brAnim;
                } else {
                   // logBrAnim(action, "NULL_BR_ANIM");
                }
            }

            if (isActuallyMovingForAnim()) {
                //logBrAnim(StudentBrAction.NONE, "RUN");
                return RUN;
            }

            //logBrAnim(StudentBrAction.NONE, "IDLE");
            return IDLE;
        }

        if (clientShotTicks > 0 || clientShotHoldTicks > 0) {
            if (getForm() != StudentForm.BR) {
                if (shotJustStarted) {
                    shotJustStarted = false;
                    return RawAnimation.begin().thenPlay(ANIM_SHOT);
                }
                return SHOT;
            }
        }

        if (clientReloadTicks > 0) return RELOAD;

        return isActuallyMoving() ? RUN : IDLE;
    }

    /*
    private void logBrAnim(StudentBrAction action, String animName) {
        if (action == lastLoggedBrAction && java.util.Objects.equals(animName, lastLoggedBrAnim)) {
            return;
        }

        lastLoggedBrAction = action;
        lastLoggedBrAnim = animName;

        System.out.println("[BR-ANIM] action=" + action + " anim=" + animName);
    }


     */
    private String getBrAnimationName(StudentBrAction a) {
        return switch (a) {
            case MAIN_SHOT -> "MAIN_SHOT";
            case GUARD_SHOT -> "GUARD_SHOT";
            case DODGE_SHOT -> "DODGE_SHOT";
            case GUARD_TACKLE -> "GUARD_TACKLE";
            case GUARD_BASH -> "GUARD_BASH";
            case RIGHT_SIDE_SUB_SHOT -> "RIGHT_SIDE_SUB_SHOT";
            case LEFT_SIDE_SUB_SHOT -> "LEFT_SIDE_SUB_SHOT";
            case SUB_SHOT -> "SUB_SHOT";
            case SUB_RELOAD_SHOT -> "SUB_RELOAD_SHOT";
            case LEFT_MOVE -> "LEFT_MOVE";
            case RIGHT_MOVE -> "RIGHT_MOVE";
            case IDLE -> "IDLE";
            case NONE -> "NONE";
            default -> a.name();
        };
    }

    private boolean isActuallyMovingForAnim() {
        Vec3 v = getDeltaMovement();
        double horizontal = v.x * v.x + v.z * v.z;

        if (horizontal > 0.0004) {
            return true;
        }

        if (getNavigation() != null && !getNavigation().isDone()) {
            return true;
        }

        return false;
    }




    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(
                        "main",
                        0,
                        state -> {
                            state.setAnimation(resolveMainAnimation());
                            return PlayState.CONTINUE;
                        }
                )
        );
    }





    private boolean isActuallyMoving() {
        Vec3 v = getDeltaMovement();
        double horizontal = v.x * v.x + v.z * v.z;
        return horizontal > 1.0e-5 || !getNavigation().isDone();
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public Vec3 getMuzzlePosApprox() {
        Vec3 eye = getEyePosition();
        Vec3 forward = getViewVector(1.0f).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = up.cross(forward).normalize();

        Vec3 off = getStudentId().getMuzzleOffset();

        return eye
                .add(right.scale(off.x))
                .add(up.scale(off.y))
                .add(forward.scale(off.z));
    }

    public Vec3 getMuzzlePosFor(WeaponSpec spec) {
        if (level().isClientSide()) {
            if (spec.muzzleLocator == WeaponSpec.MuzzleLocator.SUB_MUZZLE) {
                if (clientSubMuzzleWorldPos != null) return clientSubMuzzleWorldPos;
            } else if (spec.muzzleLocator == WeaponSpec.MuzzleLocator.LEFT_SUB_MUZZLE) {
                if (clientLeftSubMuzzleWorldPos != null) return clientLeftSubMuzzleWorldPos;
            } else if (spec.muzzleLocator == WeaponSpec.MuzzleLocator.RIGHT_SUB_MUZZLE) {
                if (clientRightSubMuzzleWorldPos != null) return clientRightSubMuzzleWorldPos;
            } else {
                if (clientMuzzleWorldPos != null) return clientMuzzleWorldPos;
            }
        }

        return switch (spec.muzzleLocator) {
            case SUB_MUZZLE, LEFT_SUB_MUZZLE, RIGHT_SUB_MUZZLE -> getMuzzlePosApproxSub();
            default -> getMuzzlePosApproxMain();
        };
    }

    public Vec3 getMuzzlePosApproxMain() {
        return getMuzzlePosApprox();
    }

    public Vec3 getMuzzlePosApproxSub() {
        return getMuzzlePosApprox();
    }

    public void setClientMuzzleWorldPos(Vec3 pos) {
        clientMuzzleWorldPos = pos;
    }

    public Vec3 getClientMuzzleWorldPosOrApprox() {
        return clientMuzzleWorldPos != null ? clientMuzzleWorldPos : getMuzzlePosApprox();
    }

    public void setClientSubMuzzleWorldPos(Vec3 pos) {
        clientSubMuzzleWorldPos = pos;
    }

    public Vec3 getClientSubMuzzleWorldPosOrApprox() {
        return clientSubMuzzleWorldPos != null ? clientSubMuzzleWorldPos : getClientMuzzleWorldPosOrApprox();
    }

    public void setClientLeftSubMuzzleWorldPos(Vec3 pos) {
        clientLeftSubMuzzleWorldPos = pos;
    }

    public void setClientRightSubMuzzleWorldPos(Vec3 pos) {
        clientRightSubMuzzleWorldPos = pos;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide() && isLifeLocked()) {
            if (level() instanceof ServerLevel sw) {
                getNavigation().stop();
                setDeltaMovement(Vec3.ZERO);

                if ((lifeState == StudentLifeState.WARPING_TO_BED
                        || lifeState == StudentLifeState.SLEEPING
                        || lifeState == StudentLifeState.RECOVERING)
                        && respawnBedFoot != null) {
                    Vec3 p = getSleepPos(sw, respawnBedFoot);
                    float yaw = getBedYaw(sw, respawnBedFoot);
                    setYRot(yaw);
                    yBodyRot = yaw;
                    yHeadRot = yaw;
                    setPos(p.x, p.y, p.z);
                }
            }
            super.aiStep();
            return;
        }

        super.aiStep();

        if (!level().isClientSide()) {
            boolean onGroundNow = onGround();

            if (bs_wasOnGround && !onGroundNow) {
                noFallTicks = Math.max(noFallTicks, 20);
            }
            bs_wasOnGround = onGroundNow;

            if (noFallTicks > 0) noFallTicks--;
            if (!onGroundNow && noFallTicks > 0) {
                noFallTicks = Math.max(noFallTicks, 5);
            }
        }

        if (!level().isClientSide() && onGround()) {
            preventWalkingOffCliff();
        }

        if (level().isClientSide()) return;

        if (isInWater() && !isPassenger()) {
            Vec3 look = getViewVector(1.0f);
            Vec3 forward = new Vec3(look.x, 0, look.z);
            if (forward.lengthSqr() > 1.0e-6) {
                forward = forward.normalize().scale(0.03);
                addDeltaMovement(new Vec3(forward.x, 0.0, forward.z));
            }
        }
    }

    private void preventWalkingOffCliff() {
        if (!(level() instanceof ServerLevel sw)) return;

        Vec3 motion = getDeltaMovement();
        double hx = motion.x;
        double hz = motion.z;

        if (hx * hx + hz * hz < 0.003) return;

        Vec3 next = position().add(hx * 4.0, 0, hz * 4.0);
        BlockPos nextPos = BlockPos.containing(next.x, getY() - 0.1, next.z);

        int safeDrop = 3;
        boolean groundFound = false;

        for (int i = 0; i <= safeDrop; i++) {
            BlockPos check = nextPos.below(i);
            if (!sw.getBlockState(check).getCollisionShape(sw, check).isEmpty()) {
                groundFound = true;
                break;
            }
        }

        if (!groundFound) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
    }

    public void setAimAngles(float yaw, float pitch) {
        if (!level().isClientSide()) {
            entityData.set(AIM_YAW, yaw);
            entityData.set(AIM_PITCH, pitch);
        }
    }

    public float getAimYaw() {
        return entityData.get(AIM_YAW);
    }

    public float getAimPitch() {
        return entityData.get(AIM_PITCH);
    }

    private void saveStudentDataToTag(CompoundTag tag) {
        tag.putInt("DataVersion", STUDENT_DATA_VERSION);

        tag.putInt("StudentForm", entityData.get(FORM_ID));
        tag.putInt("DimTransferCooldown", dimTransferCooldown);
        tag.putBoolean("DimTransferQueued", dimTransferQueued);
        tag.putBoolean("ForcedSecurityOffline", forcedSecurityBecauseOwnerOffline);
        tag.putInt("LifeState", lifeState.ordinal());
        tag.putInt("LifeTimer", lifeTimer);



        if (respawnBedFoot != null) tag.putLong("RespawnBedFoot", respawnBedFoot.asLong());
        if (respawnSafePos != null) tag.putLong("RespawnSafePos", respawnSafePos.asLong());

        if (ownerUuid != null) {
            tag.putString("Owner", ownerUuid.toString());
        }

        tag.putInt("AiMode", getAiMode().id);

        if (securityPos != null) {
            tag.putInt("SecX", securityPos.getX());
            tag.putInt("SecY", securityPos.getY());
            tag.putInt("SecZ", securityPos.getZ());
        }

        CompoundTag invTag = new CompoundTag();
        studentInventory.writeNbt(invTag);
        tag.put("StudentInv", invTag);

        tag.putString("StudentId", getStudentId().asString());
        tag.putInt("Ammo", ammoInMag);
        tag.putInt("ReloadLeft", reloadTicksLeft);
    }

    private void loadStudentDataFromTag(CompoundTag tag) {
        int dataVersion = tag.getInt("DataVersion").orElse(0);

        if (tag.contains("StudentForm")) {
            entityData.set(FORM_ID, tag.getInt("StudentForm").orElse(0));
        }



        dimTransferCooldown = tag.getInt("DimTransferCooldown").orElse(0);
        dimTransferQueued = tag.getBoolean("DimTransferQueued").orElse(false);
        forcedSecurityBecauseOwnerOffline = tag.getBoolean("ForcedSecurityOffline").orElse(false);

        if (tag.contains("Owner")) {
            String s = tag.getString("Owner").orElse("");
            ownerUuid = s.isEmpty() ? null : UUID.fromString(s);
        } else {
            ownerUuid = null;
        }

        int modeId = tag.contains("AiMode")
                ? tag.getInt("AiMode").orElse(getDefaultAiMode().id)
                : getDefaultAiMode().id;
        entityData.set(getAiModeTrackedData(), modeId);

        if (tag.contains("SecX")) {
            securityPos = new BlockPos(
                    tag.getInt("SecX").orElse(0),
                    tag.getInt("SecY").orElse(0),
                    tag.getInt("SecZ").orElse(0)
            );
        } else {
            securityPos = null;
        }

        if (tag.contains("StudentInv")) {
            studentInventory.readNbt(
                    tag.getCompound("StudentInv").orElse(new CompoundTag())
            );
        }

        ammoInMag = tag.contains("Ammo")
                ? tag.getInt("Ammo").orElse(ammoInMag)
                : ammoInMag;

        reloadTicksLeft = tag.contains("ReloadLeft")
                ? tag.getInt("ReloadLeft").orElse(0)
                : 0;

        appliedStats = false;

        setInvulnerable(false);
        setNoGravity(false);
        noPhysics = false;
        setNoAi(false);
        ghost = false;

        if (tag.contains("LifeState")) {
            int idx = Mth.clamp(
                    tag.getInt("LifeState").orElse(0),
                    0,
                    StudentLifeState.values().length - 1
            );
            lifeState = StudentLifeState.values()[idx];
            entityData.set(LIFE_STATE, lifeState.ordinal());
        }

        lifeTimer = tag.getInt("LifeTimer").orElse(0);

        respawnBedFoot = tag.contains("RespawnBedFoot")
                ? BlockPos.of(tag.getLong("RespawnBedFoot").orElse(0L))
                : null;

        respawnSafePos = tag.contains("RespawnSafePos")
                ? BlockPos.of(tag.getLong("RespawnSafePos").orElse(0L))
                : null;

        eatingSlot = -1;
        eatingServerTicks = 0;

        guardBuffApplied = false;
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        AttributeInstance maxHp = getAttribute(Attributes.MAX_HEALTH);

        if (armor != null) {
            armor.removeModifier(BlueStudentMod.id("guard_armor"));
        }
        if (maxHp != null) {
            maxHp.removeModifier(BlueStudentMod.id("guard_maxhp"));
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason == RemovalReason.CHANGED_DIMENSION
                || reason == RemovalReason.UNLOADED_TO_CHUNK
                || reason == RemovalReason.UNLOADED_WITH_PLAYER) {
            super.remove(reason);
            return;
        }

        if (!level().isClientSide() && level() instanceof ServerLevel sw) {
            StudentWorldState st = StudentWorldState.get(sw);

            // 次元移動・死亡復活待ち・packed保存中のdiscardでは、親データを消さない
            if (packedForDimTransfer
                    || st.isPacked(getStudentId())
                    || st.getPresence(getStudentId()) == StudentPresenceState.PACKED
                    || st.getPresence(getStudentId()) == StudentPresenceState.RESPAWNING) {
                super.remove(reason);
                return;
            }
        }

        super.remove(reason);

        if (level().isClientSide()) return;

        if (level() instanceof ServerLevel sw) {
            StudentWorldState st = StudentWorldState.get(sw);
            UUID cur = st.getStudentUuid(getStudentId());
            if (cur != null && cur.equals(getUUID())) {
                st.clearStudent(getStudentId());
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    public void requestExit() {
    }

    public int getEatingSlotForRender() {
        return eatingSlot;
    }

    public ItemStack getEatingStackForRender() {
        if (eatingSlot < 0 || eatingSlot >= studentInventory.getContainerSize()) return ItemStack.EMPTY;
        return studentInventory.getItem(eatingSlot);
    }

    @Override
    public BlockPos getSecurityPos() {
        return securityPos;
    }

    @Override
    public void setSecurityPos(BlockPos pos) {
        securityPos = pos;
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (noFallTicks > 0 && source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }

        if (isLifeLocked()) return false;

        if (amount <= 1.0f) {
            int prevHurt = hurtTime;
            boolean result = super.hurtServer(level, source, amount);
            hurtTime = Math.min(hurtTime, prevHurt);
            return result;
        }

        float after = getHealth() - amount;
        if (after <= 0.5f) {
            startBedRespawn(level);
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource damageSource) {
        if (level().isClientSide()) {
            super.die(damageSource);
            return;
        }

        if (!(level() instanceof ServerLevel sw)) {
            super.die(damageSource);
            return;
        }

        if (isLifeLocked()) {
            return;
        }

        startBedRespawn(sw);
    }

    private void startBedRespawn(ServerLevel sw) {
        MinecraftServer server = sw.getServer();
        ServerLevel overworld = server.overworld();
        StudentWorldState st = StudentWorldState.get(server);

        CompoundTag packed = new CompoundTag();
        saveStudentDataToTagForTransfer(packed);
        st.setPacked(getStudentId(), packed);
        st.markRespawning(getStudentId(), sw, 100);

        BlockPos bed = st.getBed(getStudentId());



        if (bed == null && ownerUuid != null) {
            bed = BedLinkManager.getBedPos(ownerUuid, getStudentId());
            if (bed != null) st.setBed(getStudentId(), bed);
        }

        if (bed == null) {
            bed = findNearestBedFoot(overworld, getStudentId(), blockPosition(), 96);
            if (bed != null) st.setBed(getStudentId(), bed);
        }

        respawnBedFoot = null;
        respawnSafePos = null;

        if (bed == null) {
            st.clearStudent(getStudentId());
            discard();
            return;
        }

        if (!isValidLinkedBed(overworld, bed)) {
            st.clearBed(getStudentId());
            st.clearStudent(getStudentId());
            discard();
            return;
        }

        respawnBedFoot = bed;
        respawnSafePos = findSafeRespawnPosNearBed(overworld, bed);

        setHealth(1f);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);

        setNoAi(true);
        setNoGravity(true);
        setGhost(true);
        setInvulnerable(false);

        requestExit();
        setLifeState(StudentLifeState.EXITING);
        lifeTimer = 60;

        if (level() != overworld) {
            //queueTeleportToOverworldForRespawn(overworld);
        }
    }

    protected @Nullable BlockPos findNearestBedFoot(ServerLevel sw, StudentId sid, BlockPos origin, int r) {
        BlockPos best = null;
        double bestD2 = 1.0E18;

        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState st = sw.getBlockState(m);
                    if (!(st.getBlock() instanceof OnlyBedBlock)) continue;
                    if (st.getValue(OnlyBedBlock.PART) != BedPart.FOOT) continue;
                    if (st.getValue(OnlyBedBlock.STUDENT) != sid) continue;

                    double d2 = m.distSqr(origin);
                    if (d2 < bestD2) {
                        bestD2 = d2;
                        best = m.immutable();
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private BlockPos findSafeRespawnPosNearBed(ServerLevel world, BlockPos bedFootPos) {
        BlockPos base = bedFootPos.above();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();

        for (int r = 0; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(base.getX() + dx, base.getY(), base.getZ() + dz);
                    if (isSafeStandPos(world, m)) {
                        return m.immutable();
                    }
                }
            }
        }
        return base;
    }

    private boolean isSafeStandPos(ServerLevel world, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = world.getBlockState(below);

        if (belowState.isAir()) return false;
        if (belowState.getCollisionShape(world, below).isEmpty()) return false;
        if (!world.getFluidState(below).isEmpty()) return false;

        if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) return false;
        if (!world.getBlockState(pos.above()).getCollisionShape(world, pos.above()).isEmpty()) return false;

        return true;
    }

    public boolean isLifeLockedForGoal() {
        return isLifeLocked();
    }

    public void requestEatFromGoal() {
        requestEat();
    }

    public void startEatingVisualFromGoal(int slot, int ticks) {
        eatingSlot = slot;
        eatingServerTicks = ticks;
    }

    private void tickSkillCommon() {
        if (skillCooldownTicks > 0) skillCooldownTicks--;

        if (skillActiveTicksLeft > 0) {
            skillActiveTicksLeft--;
            if (skillActiveTicksLeft == 0) {
                skillCooldownTicks = 0;
            }
        }
    }

    public boolean isSkillActive() {
        return skillActiveTicksLeft > 0;
    }

    public boolean canStartSkill() {
        return skillCooldownTicks <= 0 && skillActiveTicksLeft <= 0;
    }

    public void startSkillNow() {
        if (!canStartSkill()) return;
        skillActiveTicksLeft = 40;
        skillTrigger++;
    }

    public void faceTargetForShot(LivingEntity target, float maxYawStep, float maxPitchStep) {
        if (target == null) return;

        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        Vec3 d = to.subtract(from);

        double dx = d.x;
        double dy = d.y;
        double dz = d.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horiz)));

        float newYaw = approachAngle(getYRot(), targetYaw, maxYawStep);
        float newPitch = approachAngle(getXRot(), targetPitch, maxPitchStep);

        setYRot(newYaw);
        setXRot(newPitch);
        yHeadRot = newYaw;
        yBodyRot = newYaw;
    }

    private float approachAngle(float cur, float target, float maxStep) {
        float delta = Mth.wrapDegrees(target - cur);
        if (delta > maxStep) delta = maxStep;
        if (delta < -maxStep) delta = -maxStep;
        return cur + delta;
    }

    public boolean isBadFoodItemForGoal(ItemStack st) {
        return isBadFoodItem(st);
    }

    private boolean isBadFoodItem(ItemStack st) {
        if (st == null || st.isEmpty()) return true;

        if (st.is(Items.ROTTEN_FLESH)) return true;
        if (st.is(Items.POISONOUS_POTATO)) return true;
        if (st.is(Items.SPIDER_EYE)) return true;
        if (st.is(Items.PUFFERFISH)) return true;
        if (st.is(Items.CHORUS_FRUIT)) return true;
        if (st.is(Items.SUSPICIOUS_STEW)) return true;

        return false;
    }

    @Override
    public boolean isEvading() {
        return evading;
    }

    @Override
    public void setEvading(boolean v) {
        evading = v;
    }

    public LookRequest getLookRequest() {
        return lookReq;
    }

    private boolean canOverrideLook(int prio) {
        return !(lookReq.holdTicks > 0 && prio < lookReq.priority);
    }

    @Override
    public void requestLookTarget(LivingEntity t, int prio, int hold) {
        if (t == null) return;
        if (!canOverrideLook(prio)) return;
        lookReq.type = LookIntentType.TARGET;
        lookReq.target = t;
        lookReq.dir = null;
        lookReq.priority = prio;
        lookReq.holdTicks = hold;
    }

    @Override
    public void requestLookAwayFrom(LivingEntity t, int prio, int hold) {
        if (t == null) return;
        if (!canOverrideLook(prio)) return;
        lookReq.type = LookIntentType.AWAY_FROM;
        lookReq.target = t;
        lookReq.dir = null;
        lookReq.priority = prio;
        lookReq.holdTicks = hold;
    }

    @Override
    public void requestLookWorldDir(Vec3 d, int prio, int hold) {
        if (d == null || d.lengthSqr() < 1.0e-6) return;
        if (!canOverrideLook(prio)) return;
        lookReq.type = LookIntentType.WORLD_DIR;
        lookReq.target = null;
        lookReq.dir = d;
        lookReq.priority = prio;
        lookReq.holdTicks = hold;
    }

    @Override
    public void requestLookMoveDir(int priority, int ticks) {
        if (ticks <= 0) return;

        if (!lookMoveDir || priority >= lookMoveDirPriority) {
            lookMoveDir = true;
            lookMoveDirPriority = priority;
            lookMoveDirTicks = ticks;
        }
    }

    @Override
    public void requestLookPos(Vec3 pos, int priority, int holdTicks) {
        if (pos == null) return;
        lookReq.type = LookIntentType.POS;
        lookReq.pos = pos;
        lookReq.priority = priority;
        lookReq.holdTicks = holdTicks;
    }

    @Override
    public LookRequest consumeLookRequest() {
        LookRequest copy = new LookRequest();
        copy.type = lookReq.type;
        copy.target = lookReq.target;
        copy.dir = lookReq.dir;
        copy.pos = lookReq.pos;
        copy.priority = lookReq.priority;
        copy.holdTicks = lookReq.holdTicks;
        lookReq.clear();
        return copy;
    }




    public void setKisakiSupportTicks(int ticks) {
        kisakiSupportTicks = Math.max(kisakiSupportTicks, ticks);
    }

    public void applyKisakiSupportBuff(boolean on, double addArmor, double addMaxHp, float healOnApply) {
        AttributeInstance armor = getAttribute(Attributes.ARMOR);
        AttributeInstance maxHp = getAttribute(Attributes.MAX_HEALTH);

        if (on) {
            if (kisakiBuffApplied) return;
            kisakiBuffApplied = true;

            if (armor != null && addArmor != 0) {
                armor.removeModifier(BlueStudentMod.id("kisaki_support_armor"));
                armor.addOrReplacePermanentModifier(new AttributeModifier(
                        BlueStudentMod.id("kisaki_support_armor"),
                        addArmor,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            if (maxHp != null && addMaxHp != 0) {
                maxHp.removeModifier(BlueStudentMod.id("kisaki_support_maxhp"));
                maxHp.addOrReplacePermanentModifier(new AttributeModifier(
                        BlueStudentMod.id("kisaki_support_maxhp"),
                        addMaxHp,
                        AttributeModifier.Operation.ADD_VALUE
                ));
            }

            float newMax = getMaxHealth();
            if (healOnApply > 0 && getHealth() < newMax) {
                setHealth(Math.min(newMax, getHealth() + healOnApply));
            }
        } else {
            if (!kisakiBuffApplied) return;
            kisakiBuffApplied = false;

            if (armor != null) {
                armor.removeModifier(BlueStudentMod.id("kisaki_support_armor"));
            }
            if (maxHp != null) {
                maxHp.removeModifier(BlueStudentMod.id("kisaki_support_maxhp"));
            }

            if (getHealth() > getMaxHealth()) {
                setHealth(getMaxHealth());
            }
        }
    }

    public boolean hasKisakiSupportBuff() {
        return kisakiBuffApplied;
    }

    public int getShotTrigger() {
        return entityData.get(SHOT_TRIGGER);
    }

    public void bumpShotTrigger() {
        if (level().isClientSide()) return;
        entityData.set(SHOT_TRIGGER, entityData.get(SHOT_TRIGGER) + 1);
    }

    private void queueTeleportToOwnerDimension(ServerPlayer ownerHint) {
        if (level().isClientSide()) return;
        if (dimTransferQueued || dimTransferCooldown > 0 || ownerUuid == null) return;

        dimTransferQueued = true;
        dimTransferCooldown = 40;

        MinecraftServer server = ownerHint.level().getServer();
        server.execute(() -> {
            dimTransferQueued = false;

            if (isRemoved() || !isAlive()) return;
            if (!(level() instanceof ServerLevel src)) return;

            ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
            if (owner == null || !owner.isAlive()) return;

            ServerLevel dest = owner.level();
            if (dest == src) return;

            stopRiding();
            ejectPassengers();
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            fallDistance = 0;

            BlockPos safe = com.licht_meilleur.blue_student.util.DimensionTransferHelper.findSafeNear(dest, owner.blockPosition());
            AbstractStudentEntity moved = teleportTo(dest, safe, owner.getYRot());
            if (moved == null) return;

            moved.setDeltaMovement(Vec3.ZERO);
            moved.getNavigation().stop();
            moved.dimTransferCooldown = Math.max(moved.dimTransferCooldown, 40);
            moved.fallDistance = 0;
            moved.noFallTicks = Math.max(moved.noFallTicks, 20);
        });
    }

    private void queueTeleportToOverworldForRespawn(ServerLevel overworld) {
        if (level().isClientSide()) return;
        if (owRespawnQueued || owRespawnCooldown > 0) return;

        owRespawnQueued = true;
        owRespawnCooldown = 20;

        MinecraftServer server = overworld.getServer();
        server.execute(() -> {
            owRespawnQueued = false;

            if (isRemoved() || !isAlive()) return;
            if (!(level() instanceof ServerLevel src)) return;

            ServerLevel ow = server.overworld();
            if (ow == null || src == ow) return;

            stopRiding();
            ejectPassengers();
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            fallDistance = 0;

            BlockPos p = respawnSafePos != null
                    ? respawnSafePos
                    : (respawnBedFoot != null ? respawnBedFoot.above() : blockPosition());

            AbstractStudentEntity moved = teleportTo(ow, p, getYRot());
            if (moved != null) {
                moved.owRespawnCooldown = Math.max(moved.owRespawnCooldown, 20);
                moved.setDeltaMovement(Vec3.ZERO);
                moved.getNavigation().stop();
                moved.fallDistance = 0;
                moved.noFallTicks = Math.max(moved.noFallTicks, 20);
            }
        });
    }



    private @Nullable AbstractStudentEntity teleportTo(ServerLevel dest, BlockPos safe, float yaw) {
        return com.licht_meilleur.blue_student.util.DimensionTransferHelper.transferStudent(
                this,
                dest,
                safe,
                yaw
        );
    }

    public boolean teleportToWorldForCallback(ServerLevel dest, BlockPos spawn, float yaw) {
        if (level().isClientSide() || isLifeLockedForGoal()) return false;

        AbstractStudentEntity moved = teleportTo(dest, spawn, yaw);
        if (moved == null) return false;

        moved.setDeltaMovement(Vec3.ZERO);
        moved.getNavigation().stop();
        moved.fallDistance = 0;
        moved.noFallTicks = Math.max(moved.noFallTicks, 20);
        return true;
    }

    public boolean isLifeLockedPublic() {
        return isLifeLocked();
    }



    private void handleFollowDimTransfer() {
        if (!(level() instanceof ServerLevel sw)) return;
        if (ownerUuid == null || isLifeLocked() || getAiMode() != StudentAiMode.FOLLOW) return;

        if (dimTransferCooldown > 0) {
            dimTransferCooldown--;
            return;
        }
        if (dimTransferQueued) return;

        MinecraftServer server = sw.getServer();
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUuid);
        if (owner == null) return;

        ServerLevel dest = owner.level();
        if (dest == sw) return;

        dimTransferQueued = true;
        dimTransferCooldown = 40;

        server.execute(() -> {
            dimTransferQueued = false;

            if (isRemoved() || !isAlive()) {
                StudentWorldState.get(server).markMissing(getStudentId(), sw);
                return;
            }

            if (!(level() instanceof ServerLevel currentLevel)) return;

            ServerPlayer currentOwner = server.getPlayerList().getPlayer(ownerUuid);
            if (currentOwner == null || !currentOwner.isAlive()) return;

            ServerLevel currentDest = currentOwner.level();
            if (currentDest == currentLevel) return;

            StudentWorldState state = StudentWorldState.get(server);

            CompoundTag packed = new CompoundTag();
            saveStudentDataToTagForTransfer(packed);
            state.setPacked(getStudentId(), packed);
            state.markPacked(getStudentId(), currentLevel);

            stopRiding();
            ejectPassengers();
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            fallDistance = 0;

            BlockPos spawn = com.licht_meilleur.blue_student.util.DimensionTransferHelper
                    .findSafeNear(currentDest, currentOwner.blockPosition());

            com.licht_meilleur.blue_student.util.DimensionTransferHelper
                    .packStudent(this, currentLevel);

            AbstractStudentEntity spawned =
                    com.licht_meilleur.blue_student.util.DimensionTransferHelper
                            .spawnPackedStudent(currentDest, getStudentId(), spawn, currentOwner.getYRot());

            if (spawned == null) {
                state.markMissing(getStudentId(), currentLevel);
                return;
            }

            spawned.setDeltaMovement(Vec3.ZERO);
            spawned.getNavigation().stop();
            spawned.fallDistance = 0;
            spawned.noFallTicks = Math.max(spawned.noFallTicks, 20);
            spawned.dimTransferCooldown = Math.max(spawned.dimTransferCooldown, 40);

            state.setStudent(getStudentId(), spawned.getUUID(), ownerUuid, currentDest, spawn);
            state.clearPacked(getStudentId());
        });
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    private void tickFormFromEquipment() {
        if (!(level() instanceof ServerLevel sw)) return;

        ItemStack equip = studentInventory.getBrEquipStack();
        StudentWorldState state = StudentWorldState.get(sw);

        StudentForm stateForm = state.getForm(getStudentId());
        StudentForm desired;

        if (!equip.isEmpty()) {
            desired = StudentEquipments.isBrEquipped(getStudentId(), equip)
                    ? StudentForm.BR
                    : StudentForm.NORMAL;
        } else {
            desired = stateForm;
        }



        if (getForm() != desired) {



            setForm(desired);
            applyFormStatsAndAi(desired);
            state.setForm(getStudentId(), desired);
        }
    }

    private void applyFormStatsAndAi(StudentForm f) {
        AttributeInstance tough = getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (tough == null) return;

        tough.removeModifier(BlueStudentMod.id("br_toughness"));

        if (f == StudentForm.BR) {
            tough.addOrReplacePermanentModifier(new AttributeModifier(
                    BlueStudentMod.id("br_toughness"),
                    4.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    public StudentForm getForm() {
        return entityData.get(FORM_ID) == 1 ? StudentForm.BR : StudentForm.NORMAL;
    }

    public void setForm(StudentForm f) {
        entityData.set(FORM_ID, f == StudentForm.BR ? 1 : 0);
    }

    @Override
    public void queueFire(LivingEntity target, IStudentEntity.FireChannel ch) {
        if (level().isClientSide() || target == null) return;
        if (ch == null) ch = IStudentEntity.FireChannel.MAIN;
        queuedFire.get(ch).addLast(target.getUUID());
    }

    @Override
    public boolean hasQueuedFire(IStudentEntity.FireChannel ch) {
        if (level().isClientSide()) return false;
        if (ch == null) ch = IStudentEntity.FireChannel.MAIN;
        return !queuedFire.get(ch).isEmpty();
    }

    @Override
    public LivingEntity consumeQueuedFireTarget(IStudentEntity.FireChannel ch) {
        if (level().isClientSide() || !(level() instanceof ServerLevel sw)) return null;
        if (ch == null) ch = IStudentEntity.FireChannel.MAIN;

        UUID id = queuedFire.get(ch).pollFirst();
        if (id == null) return null;

        lastConsumedChannel = ch;
        Entity e = sw.getEntity(id);
        return e instanceof LivingEntity le ? le : null;
    }

    @Override
    public IStudentEntity.FireChannel getLastConsumedFireChannel() {
        return lastConsumedChannel;
    }

    @Override
    public void requestBrAction(StudentBrAction action, int holdTicks) {
        if (level().isClientSide()) return;

        int newId = action.ordinal();
        int curId = entityData.get(BR_ACTION_ID);
        int curHold = entityData.get(BR_ACTION_HOLD);

        if (curId != newId) {
            entityData.set(BR_ACTION_ID, newId);
        }

        int nextHold = Math.max(0, holdTicks);
        entityData.set(BR_ACTION_HOLD, Math.max(curHold, nextHold));

        if (curId != newId || nextHold > curHold) {
            int v = entityData.get(BR_ACTION_VER);
            entityData.set(BR_ACTION_VER, v + 1);
        }
    }

    public int getBrActionVersion() {
        return entityData.get(BR_ACTION_VER);
    }

    private void tickBrActionTimer() {
        if (level().isClientSide()) return;

        int hold = entityData.get(BR_ACTION_HOLD);
        if (hold > 0) {
            entityData.set(BR_ACTION_HOLD, hold - 1);
        }
    }

    public StudentBrAction getBrAction() {
        int id = entityData.get(BR_ACTION_ID);
        StudentBrAction[] vals = StudentBrAction.values();
        if (id < 0 || id >= vals.length) return StudentBrAction.NONE;
        return vals[id];
    }

    @Nullable
    protected RawAnimation getBrAnimationForAction(StudentBrAction a) {
        return null;
    }

    public boolean shouldLockBodyYawToMoveDir() {
        return lookMoveDir && lookMoveDirTicks > 0;
    }

    private void tickLookPolicies() {
        if (lookMoveDirTicks > 0) lookMoveDirTicks--;
        if (lookMoveDirTicks <= 0) lookMoveDir = false;

        if (lookMoveDir) {
            Vec3 v = getDeltaMovement();
            double vx = v.x;
            double vz = v.z;

            if (vx * vx + vz * vz > 1.0e-4) {
                float yaw = (float) (Math.toDegrees(Math.atan2(vz, vx)) - 90.0);
                setYRot(yaw);
                yBodyRot = yaw;
                yHeadRot = yaw;
            }
        }
    }

    public void addAmmoInMag(int add, int magSize) {
        ammoInMag = Math.min(magSize, ammoInMag + add);
    }

    public boolean shouldStopNavigationForShot(boolean fireIsSub) {
        if (getForm() == StudentForm.BR) {
            return false;
        }
        return true;
    }

    @Override
    public void requestLookDir(Vec3 dir, int yawSpeed, int pitchSpeed) {
        if (dir == null || dir.lengthSqr() < 1.0e-6) return;
        Vec3 p = position().add(dir.normalize().scale(100.0));
        requestLookPos(p, yawSpeed, pitchSpeed);
    }

    public StudentBrAction getBrActionServer() {
        if (level().isClientSide()) return StudentBrAction.NONE;
        int id = entityData.get(BR_ACTION_ID);
        StudentBrAction[] vals = StudentBrAction.values();
        if (id < 0 || id >= vals.length) return StudentBrAction.NONE;
        return vals[id];
    }

    public boolean isBrActionActiveServer() {
        if (getForm() != StudentForm.BR) return false;
        StudentBrAction a = getBrActionServer();
        if (a == null || a == StudentBrAction.NONE || a == StudentBrAction.IDLE) return false;
        return entityData.get(BR_ACTION_HOLD) > 0;
    }

    public boolean shouldIgnoreHitReactNow() {
        if (isLifeLocked()) return true;
        if (isEvading()) return true;
        return getForm() == StudentForm.BR && isBrActionActiveServer();
    }

    protected StudentBrAction getBrActionForAnimationClient() {
        int hold = entityData.get(BR_ACTION_HOLD);
        if (hold <= 0) {
            if (brHoldTicksClient > 0) return lastBrActionClient;
            return StudentBrAction.NONE;
        }

        StudentBrAction a = getBrAction();
        if (a == null) return StudentBrAction.NONE;

        lastBrActionClient = a;
        brHoldTicksClient = getBrActionHoldTicks();
        return a;
    }
//次元移動
    public boolean isPackedForDimTransfer() {
        return packedForDimTransfer;
    }

    public void setPackedForDimTransfer(boolean value) {
        this.packedForDimTransfer = value;
    }

    public void saveStudentDataToTagForTransfer(CompoundTag tag) {
        saveStudentDataToTag(tag);
    }

    public void loadStudentDataFromTagForTransfer(CompoundTag tag) {
        loadStudentDataFromTag(tag);
    }

    // ===== Render / model helper =====

    public StudentForm getRenderForm() {
        return this.getForm();
    }

    public float getRenderAimYawDeg() {
        return this.getAimYaw();
    }

    public float getRenderAimPitchDeg() {
        return this.getAimPitch();
    }

    public float getHeadYawDegForRender() {
        return clampDeg(this.getRenderAimYawDeg(), -60.0f, 60.0f);
    }

    public float getHeadPitchDegForRender() {
        return clampDeg(this.getRenderAimPitchDeg(), -40.0f, 30.0f);
    }

    public float getArmYawDegForRender() {
        return clampDeg(this.getRenderAimYawDeg(), -35.0f, 35.0f);
    }

    public float getArmPitchDegForRender() {
        return clampDeg(this.getRenderAimPitchDeg(), -45.0f, 45.0f);
    }

    public float getHeadYawRadForRender() {
        return degToRad(this.getHeadYawDegForRender());
    }

    public float getHeadPitchRadForRender() {
        return degToRad(this.getHeadPitchDegForRender());
    }

    public float getArmYawRadForRender() {
        return degToRad(this.getArmYawDegForRender());
    }

    public float getArmPitchRadForRender() {
        return degToRad(this.getArmPitchDegForRender());
    }

    public boolean isBrFormForRender() {
        return this.getRenderForm() == StudentForm.BR;
    }

    protected static float degToRad(float deg) {
        return deg * ((float) Math.PI / 180.0F);
    }

    protected static float clampDeg(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

}