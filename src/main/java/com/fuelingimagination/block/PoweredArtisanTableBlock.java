package com.fuelingimagination.block;

import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

public final class PoweredArtisanTableBlock extends KineticBlock implements IBE<PoweredArtisanTableBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final int CENTER = 1;

    public PoweredArtisanTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(PART, CENTER).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, ACTIVE);
    }

    public static BlockPos partPosition(BlockPos center, Direction facing, int part) {
        return center.relative(facing.getClockWise(), part - CENTER);
    }

    public static BlockPos centerPosition(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getClockWise(), CENTER - state.getValue(PART));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        for (int part = 0; part < 3; part++) {
            BlockPos target = partPosition(center, facing, part);
            if (!level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).canBeReplaced(BlockPlaceContext.at(context, target, context.getClickedFace()))
                    || !level.isUnobstructed(state.setValue(PART, part), target,
                            context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer()))
                    || (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(target, context.getClickedFace(), context.getItemInHand()))) {
                return null;
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            for (int part : new int[]{0, 2}) {
                level.setBlock(partPosition(pos, state.getValue(FACING), part), state.setValue(PART, part), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !replacement.is(this) && !moving
                && level.getBlockEntity(centerPosition(pos, state)) instanceof PoweredArtisanTableBlockEntity table) {
            // A side cell can be broken first. Empty the center once before removing any of the three cells.
            table.dropContents();
        }
        super.onRemove(state, level, pos, replacement, moving);
        if (!level.isClientSide && !replacement.is(this) && !moving) {
            BlockPos center = centerPosition(pos, state);
            for (int part = 0; part < 3; part++) {
                BlockPos target = partPosition(center, state.getValue(FACING), part);
                if (target.equals(pos)) continue;
                BlockState other = level.getBlockState(target);
                if (other.is(this) && other.getValue(FACING) == state.getValue(FACING) && other.getValue(PART) == part) {
                    // Only the originally broken/wrenched part supplies loot.
                    level.removeBlock(target, false);
                }
            }
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos center = centerPosition(pos, state);
        Direction facing = state.getValue(FACING);
        // Do not interpret an unloaded neighboring chunk as a removed table part.
        for (int part = 0; part < 3; part++) {
            if (!level.hasChunkAt(partPosition(center, facing, part))) return;
        }
        for (int part = 0; part < 3; part++) {
            BlockState other = level.getBlockState(partPosition(center, facing, part));
            if (!other.is(this) || other.getValue(FACING) != facing || other.getValue(PART) != part) {
                level.destroyBlock(pos, true);
                return;
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos center = centerPosition(pos, state);
        if (!level.isClientSide && level.getBlockEntity(center) instanceof PoweredArtisanTableBlockEntity table) {
            if (table.isSpeedRequirementFulfilled()) {
                NetworkHooks.openScreen((ServerPlayer) player, table, center);
            } else {
                player.displayClientMessage(Component.translatable("gui.fuelingimagination.artisan.insufficient_power"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) { return Direction.Axis.Y; }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(PART) == CENTER && face == Direction.DOWN;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        // Rotation of a single cell would split the table. Sneak-wrench uses Create's whole-item recovery.
        return InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == CENTER ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public Class<PoweredArtisanTableBlockEntity> getBlockEntityClass() { return PoweredArtisanTableBlockEntity.class; }

    @Override
    public BlockEntityType<? extends PoweredArtisanTableBlockEntity> getBlockEntityType() {
        return ModBlockEntities.POWERED_ARTISAN_TABLE.get();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == CENTER ? getBlockEntityType().create(pos, state) : null;
    }
}
