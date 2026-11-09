package com.fuelingimagination.mixin;

import com.fuelingimagination.content.upgrade.UpgradeData;
import com.fuelingimagination.item.PoweredSwordItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Auto-smelts block drops for the blaze-burner accessory.
 *
 * <p>{@code getDrops} is the single funnel every block break passes through (the four-argument
 * overload forwards here with a null entity and empty tool), so remapping the returned list is
 * enough. The target is the SRG name of the six-argument overload.
 */
@Mixin(value = Block.class, remap = false)
public abstract class BlockSmeltMixin {
    @Inject(method = "m_49874_", at = @At("RETURN"), cancellable = true, remap = false)
    private static void fuelingimagination$smeltDrops(BlockState state, ServerLevel level, BlockPos pos,
                                                      BlockEntity blockEntity, Entity entity, ItemStack tool,
                                                      CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!(tool.getItem() instanceof PoweredSwordItem)) return;
        if (!UpgradeData.hasComponent(tool, "create:blaze_burner")) return;

        List<ItemStack> drops = cir.getReturnValue();
        List<ItemStack> smelted = new ArrayList<>(drops.size());
        for (ItemStack drop : drops) smelted.add(smelt(level, drop));
        cir.setReturnValue(smelted);
    }

    private static ItemStack smelt(ServerLevel level, ItemStack input) {
        if (input.isEmpty()) return input;
        Optional<SmeltingRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(input), level);
        if (recipe.isEmpty()) return input;
        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
        result.setCount(Math.max(1, result.getCount()) * input.getCount());
        return result;
    }
}
