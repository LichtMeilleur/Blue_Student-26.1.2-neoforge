package com.licht_meilleur.blue_student.block;

import com.licht_meilleur.blue_student.block.entity.OnlyBedBlockEntity;
import com.licht_meilleur.blue_student.registry.StudentBedRegistry;
import com.licht_meilleur.blue_student.state.StudentWorldState;
import com.licht_meilleur.blue_student.student.StudentId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class OnlyBedBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<BedPart> PART = BlockStateProperties.BED_PART;
    public static final EnumProperty<StudentId> STUDENT =
            EnumProperty.create("student", StudentId.class, StudentId.values());

    private static final VoxelShape BED_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 9.0, 16.0);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;


    public OnlyBedBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(PART, BedPart.FOOT)
                        .setValue(STUDENT, StudentId.SHIROKO)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, STUDENT, HALF);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BedPart.FOOT ? new OnlyBedBlockEntity(pos, state) : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (level instanceof Level realLevel) {
            if (!realLevel.dimension().equals(Level.OVERWORLD)) {
                return false;
            }
        }

        BedPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);

        if (part == BedPart.FOOT) {
            BlockPos headPos = pos.relative(facing);
            return level.getBlockState(headPos).canBeReplaced();
        } else {
            BlockPos footPos = pos.relative(facing.getOpposite());
            BlockState foot = level.getBlockState(footPos);
            return foot.is(this) && foot.getValue(PART) == BedPart.FOOT;
        }
    }

    @Override
    protected BlockState updateShape(BlockState state,
                                     LevelReader level,
                                     ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos,
                                     Direction dir,
                                     BlockPos neighborPos,
                                     BlockState neighborState,
                                     RandomSource random) {
        BlockPos other = findOtherHalfPos(level, pos, state);
        if (other == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Nullable
    public BlockPos findOtherHalfPos(LevelReader level, BlockPos pos, BlockState state) {
        if (state == null || !state.is(this)) {
            return null;
        }
        if (!state.hasProperty(PART) || !state.hasProperty(FACING)) {
            return null;
        }

        BedPart part = state.getValue(PART);
        Direction facing = state.getValue(FACING);

        return part == BedPart.FOOT
                ? pos.relative(facing)
                : pos.relative(facing.getOpposite());
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos,
                              BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);

        if (!level.isClientSide() && state.getValue(PART) == BedPart.FOOT) {
            popResource(level, pos, new ItemStack(Items.WHITE_BED));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {

        BlockPos other = findOtherHalfPos(level, pos, state);
        if (other == null) {
            return;
        }

        // 同じブロックなら何もしない
        if (oldState.is(state.getBlock())) {
            return;
        }

        if (!level.isClientSide()) {
            BlockPos otherPos = findOtherHalfPos(level, pos, oldState);
            if (otherPos != null) {
                level.destroyBlock(otherPos, false);
            }
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BED_SHAPE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return BED_SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide()) return;
        if (state.getValue(PART) != BedPart.FOOT) return;

        StudentId sid = state.getValue(STUDENT);
        BlockPos old = StudentBedRegistry.get(sid);

        if (old != null && !old.equals(pos)) {
            BlockState oldState = level.getBlockState(old);
            if (oldState.getBlock() instanceof OnlyBedBlock) {
                level.destroyBlock(old, true);
            }
        }

        StudentBedRegistry.set(sid, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            BlockPos otherPos = findOtherHalfPos(level, pos, state);
            if (otherPos != null) {
                BlockState otherState = level.getBlockState(otherPos);
                if (otherState.is(this)) {
                    level.destroyBlock(otherPos, false);
                }
            }

            StudentWorldState stateData = StudentWorldState.get(sl);

            for (StudentId sid : StudentId.values()) {
                BlockPos bed = stateData.getBed(sid);
                if (bed != null && (bed.equals(pos) || (otherPos != null && bed.equals(otherPos)) || bed.closerThan(pos, 1.5))) {
                    stateData.clearBed(sid);
                }
            }

            StudentId sid = state.getValue(STUDENT);
            BlockPos reg = StudentBedRegistry.get(sid);
            if (reg != null && (reg.equals(pos) || (otherPos != null && reg.equals(otherPos)) || reg.closerThan(pos, 1.5))) {
                StudentBedRegistry.remove(sid);
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }




}