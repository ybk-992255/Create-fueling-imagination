package com.fuelingimagination.content.dismantler;

import com.fuelingimagination.content.artisan.DamagedBlockRepairHandler;
import com.fuelingimagination.content.damaged.DamagedBlocks;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rules for the powered dismantler.
 *
 * <p>The dismantler is not a repair station: it deliberately ignores the damaged flag
 * and reduces any functional block in {@link DamagedBlocks#DAMAGEABLE_BLOCKS} to parts.
 * A block's vanilla crafting recipe supplies the number of parts. Half of those parts
 * come back as their real ingredient; the other half are destroyed and replaced by
 * damaged components. A one-part block therefore always yields a single damaged
 * component, as specified by the design.
 */
public final class DismantlingRecipes {
    private DismantlingRecipes() { }

    /** Inputs accepted by the dismantler: functional blocks, but never the special crate pool. */
    public static boolean canProcess(ItemStack stack) {
        return !stack.isEmpty()
                && stack.is(DamagedBlocks.DAMAGEABLE_BLOCKS)
                && !stack.is(DamagedBlocks.SCRAP_CRATE_SPECIAL);
    }

    /**
     * Rolls the outputs for one consumed input.
     *
     * <p>For a recipe with N occupied ingredient slots, {@code N / 2} randomly chosen
     * slots yield one representative ingredient each and every remaining slot yields a
     * damaged component. N=1 (including blocks with no crafting recipe) produces only a
     * damaged component, so a machine can never be used to duplicate a one-part block.
     */
    public static List<ItemStack> roll(ItemStack input, Level level) {
        List<ItemStack> parts = recipeParts(input, level);
        if (parts.size() <= 1) {
            return List.of(new ItemStack(ModItems.DAMAGED_COMPONENT.get()));
        }

        int materialCount = parts.size() / 2;
        shuffle(parts, level.random);
        List<ItemStack> outputs = new ArrayList<>(parts.size());
        for (int i = 0; i < materialCount; i++) {
            outputs.add(parts.get(i).copyWithCount(1));
        }
        for (int i = materialCount; i < parts.size(); i++) {
            outputs.add(new ItemStack(ModItems.DAMAGED_COMPONENT.get()));
        }
        return outputs;
    }

    /** All non-empty ingredient slots of the block's crafting recipe, in grid order. */
    private static List<ItemStack> recipeParts(ItemStack input, Level level) {
        List<ItemStack> parts = new ArrayList<>();
        CraftingRecipe recipe = DamagedBlockRepairHandler.findRecipe(input, level);
        Ingredient[] grid = DamagedBlockRepairHandler.gridIngredients(recipe);
        for (Ingredient ingredient : grid) {
            if (ingredient == null) continue;
            ItemStack representative = representative(ingredient);
            if (!representative.isEmpty()) parts.add(representative);
        }
        return parts;
    }

    private static ItemStack representative(Ingredient ingredient) {
        for (ItemStack choice : ingredient.getItems()) {
            if (!choice.isEmpty()) return choice.copyWithCount(1);
        }
        return ItemStack.EMPTY;
    }

    private static void shuffle(List<ItemStack> parts, RandomSource random) {
        for (int i = parts.size(); i > 1; i--) {
            Collections.swap(parts, i - 1, random.nextInt(i));
        }
    }
}
