package com.fuelingimagination.mixin;

import com.simibubi.create.content.equipment.armor.BacktankBlock;
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Create's backtank {@code getDrops} only re-attaches the loot table's tag, which drops our custom
 * upgrade NBT when a placed tank is broken. Re-attach the block entity's vanilla tag (which holds
 * {@code fuelingimagination:upgrades}) so capacity doubling and other upgrades survive placement.
 */
@Mixin(value = BacktankBlock.class, remap = false)
public abstract class BacktankBlockMixin {
    @Inject(method = "getDrops", at = @At("RETURN"), remap = false)
    private void fuelingimagination$preserveVanillaTag(BlockState state, LootParams.Builder builder,
                                                       CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(be instanceof BacktankBlockEntity bbe)) return;
        CompoundTag vanilla = bbe.getVanillaTag();
        if (vanilla == null) return;
        CompoundTag tag = vanilla.copy();
        tag.putInt("Air", bbe.getAirLevel());
        for (ItemStack stack : cir.getReturnValue()) {
            if (stack.getItem() instanceof BacktankItem) {
                stack.setTag(tag.copy());
            }
        }
    }
}
