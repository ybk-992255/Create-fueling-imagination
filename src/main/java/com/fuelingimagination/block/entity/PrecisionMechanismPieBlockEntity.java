package com.fuelingimagination.block.entity;

import com.fuelingimagination.registry.ModBlockEntities;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PrecisionMechanismPieBlockEntity extends BlockEntity {
    private CompoundTag itemTag = new CompoundTag();

    public PrecisionMechanismPieBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRECISION_MECHANISM_PIE.get(), pos, state);
    }

    public void setItemTag(CompoundTag tag) {
        itemTag = tag.copy();
        itemTag.remove("remaining_servings");
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public ItemStack getItemStack() {
        ItemStack stack = new ItemStack(ModItems.PRECISION_MECHANISM_PIE.get());
        if (!itemTag.isEmpty()) {
            stack.setTag(itemTag.copy());
        }
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("PieItemTag", itemTag.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemTag = tag.getCompound("PieItemTag").copy();
        itemTag.remove("remaining_servings");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
