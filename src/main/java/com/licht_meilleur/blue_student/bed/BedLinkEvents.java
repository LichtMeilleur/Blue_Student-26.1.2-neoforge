package com.licht_meilleur.blue_student.bed;

import com.licht_meilleur.blue_student.BlueStudentMod;
import com.licht_meilleur.blue_student.block.OnlyBedBlock;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.StudentId;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.item.Items;

public class BedLinkEvents {

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (!player.isCrouching()) {
                return InteractionResult.PASS;
            }

            // 設置中は無視
            if (player.getItemInHand(hand).is(Items.WHITE_BED)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hit.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!(state.getBlock() instanceof BedBlock)) {
                return InteractionResult.PASS;
            }

            StudentId linking = BedLinkManager.getLinking(player.getUUID());
            if (linking == null) {
                return InteractionResult.PASS;
            }

            Direction vanillaFacing = state.getValue(BedBlock.FACING);
            BedPart part = state.getValue(BedBlock.PART);

            // バニラの FOOT 基準に揃える
            BlockPos vanillaFootPos = (part == BedPart.FOOT)
                    ? pos
                    : pos.relative(vanillaFacing.getOpposite());
            BlockPos vanillaHeadPos = vanillaFootPos.relative(vanillaFacing);

            // OnlyBed の向き
            Direction onlyFacing = vanillaFacing;

            // OnlyBed の HEAD は onlyFacing 基準
            BlockPos onlyHeadPos = vanillaFootPos.relative(onlyFacing);

            ServerLevel serverLevel = (ServerLevel) world;

            // 既存 OnlyBed 除去（ドロップなし）
            BlockPos oldFoot = StudentWorldState.get(serverLevel.getServer()).getBed(linking);
            if (oldFoot == null) {
                oldFoot = BedLinkManager.getBedPos(player.getUUID(), linking); // 互換フォールバック
            }

            if (oldFoot != null) {
                BlockState old = world.getBlockState(oldFoot);
                if (old.getBlock() == BlueStudentMod.ONLY_BED_BLOCK && old.hasProperty(OnlyBedBlock.FACING)) {
                    Direction f = old.getValue(OnlyBedBlock.FACING);
                    BlockPos oldHead = oldFoot.relative(f);
                    int killFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
                    world.setBlock(oldHead, Blocks.AIR.defaultBlockState(), killFlags);
                    world.setBlock(oldFoot, Blocks.AIR.defaultBlockState(), killFlags);
                }
            }

            // バニラベッドを両方ドロップなしで削除（HEAD → FOOT）
            int killFlags = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
            world.setBlock(vanillaHeadPos, Blocks.AIR.defaultBlockState(), killFlags);
            world.setBlock(vanillaFootPos, Blocks.AIR.defaultBlockState(), killFlags);

            // OnlyBed 設置
            BlockState footState = ((Block) BlueStudentMod.ONLY_BED_BLOCK).defaultBlockState();
            footState = footState.setValue(OnlyBedBlock.FACING, onlyFacing);
            footState = footState.setValue(OnlyBedBlock.PART, BedPart.FOOT);
            footState = footState.setValue(OnlyBedBlock.STUDENT, linking);


            BlockState headState = footState.setValue(OnlyBedBlock.PART, BedPart.HEAD);

            world.setBlock(vanillaFootPos, footState, Block.UPDATE_ALL);
            world.setBlock(onlyHeadPos, headState, Block.UPDATE_ALL);

            // 永続化込みで保存
            BedLinkManager.setBedPosAndPersist(serverLevel, player.getUUID(), linking, vanillaFootPos);

            BedLinkManager.clearLinking(player.getUUID());
            player.sendSystemMessage(Component.literal("Linked bed -> " + linking.asString()));

            return InteractionResult.SUCCESS;
        });
    }
}