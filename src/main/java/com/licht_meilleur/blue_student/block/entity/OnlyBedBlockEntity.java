package com.licht_meilleur.blue_student.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class OnlyBedBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation NORMAL =
            RawAnimation.begin().thenLoop("animation.model.normal");
    private static final RawAnimation SLEEP =
            RawAnimation.begin().thenLoop("animation.model.sleep");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean sleepAnim = false;

    public OnlyBedBlockEntity(BlockPos pos, BlockState state) {
        super(BlueStudentMod.ONLY_BED_BE.get(), pos, state);
    }

    public boolean isSleepAnim() {
        return this.sleepAnim;
    }

    public void setSleepAnim(boolean value) {
        if (this.sleepAnim == value) {
            return;
        }

        this.sleepAnim = value;
        this.setChanged();
        this.sync();
    }

    public StudentId getStudentId() {
        BlockState state = this.getBlockState();
        if (state.hasProperty(OnlyBedBlock.STUDENT)) {
            return state.getValue(OnlyBedBlock.STUDENT);
        }
        return StudentId.SHIROKO;
    }

    public String getBedTexturePath() {
        return switch (getStudentId()) {
            case SHIROKO -> "textures/entity/shiroko_bed.png";
            case HOSHINO -> "textures/entity/hoshino_bed.png";
            case HINA    -> "textures/entity/hina_bed.png";
            case ALICE   -> "textures/entity/alice_bed.png";
            case KISAKI  -> "textures/entity/kisaki_bed.png";
            case MARIE   -> "textures/entity/marie_bed.png";
            case HIKARI  -> "textures/entity/hikari_bed.png";
            case NOZOMI  -> "textures/entity/nozomi_bed.png";
        };
    }

    private void sync() {
        if (this.level == null) {
            return;
        }

        BlockState state = this.getBlockState();
        this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "main",
                0,
                state -> {
                    state.setAnimation(this.sleepAnim ? SLEEP : NORMAL);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("SleepAnim", this.sleepAnim);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.sleepAnim = input.getBooleanOr("SleepAnim", false);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}