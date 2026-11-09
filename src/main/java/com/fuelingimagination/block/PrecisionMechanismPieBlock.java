package com.fuelingimagination.block;

import com.fuelingimagination.block.entity.PrecisionMechanismPieBlockEntity;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class PrecisionMechanismPieBlock extends PlacedFoodBlock implements EntityBlock {
    public PrecisionMechanismPieBlock(Properties properties) {
        super(properties, Block.box(1, 0, 3, 15, 6, 13));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrecisionMechanismPieBlockEntity(pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(blockEntity instanceof PrecisionMechanismPieBlockEntity pie
                ? pie.getItemStack() : new ItemStack(ModItems.PRECISION_MECHANISM_PIE.get()));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                     net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                     net.minecraft.world.entity.player.Player player) {
        return level.getBlockEntity(pos) instanceof PrecisionMechanismPieBlockEntity pie
                ? pie.getItemStack() : new ItemStack(ModItems.PRECISION_MECHANISM_PIE.get());
    }
}
