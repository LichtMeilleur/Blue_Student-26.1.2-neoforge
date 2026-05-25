package com.licht_meilleur.blue_student.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.inventory.CraftChamberScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CraftChamberBlockEntity extends BlockEntity implements GeoBlockEntity, MenuProvider {

    private static final RawAnimation HOVER =
            RawAnimation.begin().thenLoop("animation.model.monolith_hover");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CraftChamberBlockEntity(BlockPos pos, BlockState state) {
        super(BlueStudentMod.CRAFT_CHAMBER_BE.get(), pos, state);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                "main",
                0,
                state -> {
                    state.setAnimation(HOVER);
                    return PlayState.CONTINUE;
                }
        ));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Craft Chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new CraftChamberScreenHandler(syncId, inv, this, this.worldPosition);
    }
}