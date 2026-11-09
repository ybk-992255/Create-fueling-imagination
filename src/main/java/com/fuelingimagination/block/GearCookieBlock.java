package com.fuelingimagination.block;

import com.fuelingimagination.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GearCookieBlock extends PlacedFoodBlock {
    public static final int MAX_COOKIES = 6;
    public static final IntegerProperty COOKIES = IntegerProperty.create("cookies", 1, MAX_COOKIES);
    private static final VoxelShape[] SHAPES = {
            Block.box(1, 0, 1, 15, 2, 15), Block.box(1, 0, 1, 15, 2, 15),
            Block.box(1, 0, 1, 15, 4, 15), Block.box(1, 0, 1, 15, 4, 15),
            Block.box(1, 0, 1, 15, 5, 15), Block.box(1, 0, 1, 15, 5, 15)
    };

    public GearCookieBlock(Properties properties) {
        super(properties, Block.box(1, 0, 1, 15, 5, 15));
        registerDefaultState(stateDefinition.any().setValue(COOKIES, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COOKIES);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(COOKIES) - 1];
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        int count = state.getValue(COOKIES);
        if (held.is(ModItems.GEAR_COOKIE.get())) {
            // Always consume this interaction, including a full plate, so it cannot place or eat another cookie.
            if (!level.isClientSide && count < MAX_COOKIES
                    && level.setBlock(pos, state.setValue(COOKIES, count + 1), 3)) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                player.awardStat(Stats.ITEM_USED.get(ModItems.GEAR_COOKIE.get()));
                level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player));
            }
        } else if (!level.isClientSide) {
            boolean changed = count == 1 ? level.removeBlock(pos, false)
                    : level.setBlock(pos, state.setValue(COOKIES, count - 1), 3);
            if (changed) {
                ItemStack extracted = new ItemStack(ModItems.GEAR_COOKIE.get());
                if (!player.addItem(extracted)) {
                    player.drop(extracted, false);
                }
                level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
                level.gameEvent(count == 1 ? GameEvent.BLOCK_DESTROY : GameEvent.BLOCK_CHANGE,
                        pos, GameEvent.Context.of(player));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
