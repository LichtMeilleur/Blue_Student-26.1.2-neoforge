package com.licht_meilleur.blue_student.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.BlueStudentMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TabletBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation ROT =
            RawAnimation.begin().thenLoop("animation.model.tablet_rotation");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public TabletBlockEntity(BlockPos pos, BlockState state) {
        super(BlueStudentMod.TABLET_BE.get(), pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "main",
                0,
                state -> {
                    state.setAnimation(ROT);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}