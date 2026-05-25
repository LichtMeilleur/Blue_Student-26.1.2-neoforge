package com.licht_meilleur.blue_student.bed;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class BedLinkEvents {

    private BedLinkEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();

        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!player.isCrouching()) {
            return;
        }

        if (player.getItemInHand(event.getHand()).is(Items.WHITE_BED)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = serverLevel.getBlockState(pos);

        if (!(state.getBlock() instanceof BedBlock)) {
            return;
        }

        StudentId linking = BedLinkManager.getLinking(player.getUUID());
        if (linking == null) {
            return;
        }

        Direction vanillaFacing = state.getValue(BedBlock.FACING);
        BedPart part = state.getValue(BedBlock.PART);

        BlockPos vanillaFootPos = (part == BedPart.FOOT)
                ? pos
                : pos.relative(vanillaFacing.getOpposite());




        BlockPos vanillaHeadPos = vanillaFootPos.relative(vanillaFacing);
        BlockPos onlyHeadPos = vanillaFootPos.relative(vanillaFacing);

        BlockPos oldFoot = StudentWorldState.get(serverLevel.getServer()).getBed(linking);
        if (oldFoot == null) {
            oldFoot = BedLinkManager.getBedPos(player.getUUID(), linking);
        }

        if (oldFoot != null) {
            BlockState old = serverLevel.getBlockState(oldFoot);
            if (old.getBlock() == BlueStudentMod.ONLY_BED_BLOCK.get() && old.hasProperty(OnlyBedBlock.FACING)) {
                Direction f = old.getValue(OnlyBedBlock.FACING);
                BlockPos oldHead = oldFoot.relative(f);

                int killFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
                serverLevel.setBlock(oldHead, Blocks.AIR.defaultBlockState(), killFlags);
                serverLevel.setBlock(oldFoot, Blocks.AIR.defaultBlockState(), killFlags);
            }
        }

        int killFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
        serverLevel.setBlock(vanillaHeadPos, Blocks.AIR.defaultBlockState(), killFlags);
        serverLevel.setBlock(vanillaFootPos, Blocks.AIR.defaultBlockState(), killFlags);

        BlockState footState = BlueStudentMod.ONLY_BED_BLOCK.get().defaultBlockState()
                .setValue(OnlyBedBlock.FACING, vanillaFacing)
                .setValue(OnlyBedBlock.PART, BedPart.FOOT)
                .setValue(OnlyBedBlock.STUDENT, linking);

        BlockState headState = footState.setValue(OnlyBedBlock.PART, BedPart.HEAD);

        serverLevel.setBlock(vanillaFootPos, footState, Block.UPDATE_ALL);
        serverLevel.setBlock(onlyHeadPos, headState, Block.UPDATE_ALL);

        BedLinkManager.setBedPosAndPersist(serverLevel, player.getUUID(), linking, vanillaFootPos);
        BedLinkManager.clearLinking(player.getUUID());

        player.sendSystemMessage(Component.literal("Linked bed -> " + linking.asString()));

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}