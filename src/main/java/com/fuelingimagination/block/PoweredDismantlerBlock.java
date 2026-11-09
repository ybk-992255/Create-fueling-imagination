package com.fuelingimagination.block;

import com.fuelingimagination.block.entity.PoweredDismantlerBlockEntity;
import com.fuelingimagination.content.dismantler.DismantlingRecipes;
import com.fuelingimagination.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class PoweredDismantlerBlock extends HorizontalKineticBlock
        implements IBE<PoweredDismantlerBlockEntity> {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public PoweredDismantlerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HORIZONTAL_FACING, Direction.NORTH)
                .setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PoweredDismantlerBlockEntity dismantler)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty()) {
            if (!DismantlingRecipes.canProcess(held)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("gui.fuelingimagination.dismantler.invalid")
                                    .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (level.isClientSide) return InteractionResult.SUCCESS;

            int inserted = dismantler.insert(held);
            if (inserted <= 0) {
                player.displayClientMessage(
                        Component.translatable("gui.fuelingimagination.dismantler.full")
                                .withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
            held.shrink(inserted);
            player.setItemInHand(hand, held);
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack extracted = dismantler.extractAll();
        if (extracted.isEmpty()) return InteractionResult.PASS;

        int count = extracted.getCount();
        Component name = extracted.getHoverName();
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                new PlayerMainInvWrapper(player.getInventory()), extracted, false);
        if (!remainder.isEmpty()) player.drop(remainder, false);
        player.displayClientMessage(
                Component.translatable("gui.fuelingimagination.dismantler.removed", count, name)
                        .withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!level.isClientSide && !state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof PoweredDismantlerBlockEntity dismantler) {
            dismantler.dropContents();
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public Class<PoweredDismantlerBlockEntity> getBlockEntityClass() {
        return PoweredDismantlerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PoweredDismantlerBlockEntity> getBlockEntityType() {
        return ModBlockEntities.POWERED_DISMANTLER.get();
    }
}
