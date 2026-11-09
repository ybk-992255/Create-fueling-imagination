package com.fuelingimagination.recipe;

import com.fuelingimagination.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class ArtisanRecipeLookup {
    public static Optional<Match> find(ArtisanCraftingGrid grid, Level level) {
        // Explicit artisan JSON recipes keep priority over the automatically admitted recipes.
        var custom = level.getRecipeManager().getRecipeFor(ModRecipes.ARTISAN_CRAFTING.get(), grid, level);
        if (custom.isPresent()) return Optional.of(new Match(custom.get()));
        return Optional.empty();
    }

    public record Match(Recipe<?> recipe) {
        public boolean matches(ArtisanCraftingGrid grid, Level level) {
            if (recipe instanceof ArtisanCraftingRecipe custom) return custom.matches(grid, level);
            return ((CraftingRecipe) recipe).matches(grid, level);
        }

        public ItemStack assemble(ArtisanCraftingGrid grid, Level level) {
            if (recipe instanceof ArtisanCraftingRecipe custom) return custom.assemble(grid, level.registryAccess());
            return ((CraftingRecipe) recipe).assemble(grid, level.registryAccess());
        }

        public NonNullList<ItemStack> remaining(ArtisanCraftingGrid grid) {
            if (recipe instanceof ArtisanCraftingRecipe custom) return custom.getRemainingItems(grid);
            return ((CraftingRecipe) recipe).getRemainingItems(grid);
        }

        public boolean isCurrent(Level level) {
            return level.getRecipeManager().byKey(recipe.getId()).orElse(null) == recipe;
        }
    }

    private ArtisanRecipeLookup() { }
}
