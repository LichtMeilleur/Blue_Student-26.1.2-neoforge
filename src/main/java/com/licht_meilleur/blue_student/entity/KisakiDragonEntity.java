package com.licht_meilleur.blue_student.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class KisakiDragonEntity extends Entity implements GeoEntity {

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(KisakiDragonEntity.class, EntityDataSerializers.INT);

    private static final int STATE_FLY = 0;
    private static final int STATE_COIL = 1;

    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation BUFF = RawAnimation.begin().thenLoop("buff");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Nullable
    private UUID ownerKisakiUuid;
    @Nullable
    private UUID targetUuid;

    private int coilTicks = 0;
    private double coilAngle = 0.0;

    public KisakiDragonEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }



    public KisakiDragonEntity setOwnerAndTarget(@Nullable UUID ownerKisaki, @Nullable UUID target) {
        this.ownerKisakiUuid = ownerKisaki;
        this.targetUuid = target;
        return this;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, STATE_FLY);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide()) {
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity target = (targetUuid != null) ? serverLevel.getEntity(targetUuid) : null;
        if (target == null || !target.isAlive()) {
            this.discard();
            return;
        }

        int st = this.entityData.get(STATE);
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.6, 0);

        if (st == STATE_FLY) {
            Vec3 to = center.subtract(this.position());
            double d = to.length();

            lookAt(target);

            if (d < 1.2) {
                this.entityData.set(STATE, STATE_COIL);
                coilTicks = 40;
                coilAngle = 0.0;
                return;
            }

            Vec3 step = to.normalize().scale(0.65);
            Vec3 next = this.position().add(step);

            this.setPos(next.x, next.y, next.z);
            this.setDeltaMovement(Vec3.ZERO);


        } else {
            coilTicks--;

            Vec3 p = center.add(0.8, 0.0, 0.0);

            this.setPos(p.x, p.y, p.z);
            lookAt(target);

            this.setDeltaMovement(Vec3.ZERO);


            if (coilTicks <= 0) {
                this.discard();
            }
        }
    }

    @Override
    public void move(MoverType moverType, Vec3 movement) {
        // 位置は tick 内で setPos しているため無効化
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {

    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "main",
                0,
                state -> {
                    int st = this.entityData.get(STATE);
                    state.setAnimation(st == STATE_COIL ? BUFF : RUN);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private void lookAt(Entity target) {
        Vec3 to = target.position()
                .add(0, target.getBbHeight() * 0.6, 0)
                .subtract(this.position());

        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));

        this.setYRot(yaw);

        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }
}