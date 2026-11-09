package com.fuelingimagination.recipe;

import com.fuelingimagination.registry.ModItems;
import com.fuelingimagination.registry.ModRecipes;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/** Vanilla-shaped JSON syntax with an independent recipe type, invisible to crafting tables. */
public final class ArtisanCraftingRecipe implements Recipe<Container> {
    private final ShapedRecipe pattern;

    public ArtisanCraftingRecipe(ShapedRecipe pattern) { this.pattern = pattern; }

    @Override
    public boolean matches(Container input, Level level) {
        if (input.getContainerSize() < 9) return false;
        for (int offsetY = 0; offsetY <= 3 - pattern.getHeight(); offsetY++) {
            for (int offsetX = 0; offsetX <= 3 - pattern.getWidth(); offsetX++) {
                if (matchesAt(input, offsetX, offsetY, false) || matchesAt(input, offsetX, offsetY, true)) return true;
            }
        }
        return false;
    }

    private boolean matchesAt(Container input, int offsetX, int offsetY, boolean mirror) {
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int px = x - offsetX;
                int py = y - offsetY;
                Ingredient ingredient = Ingredient.EMPTY;
                if (px >= 0 && px < pattern.getWidth() && py >= 0 && py < pattern.getHeight()) {
                    int index = (mirror ? pattern.getWidth() - px - 1 : px) + py * pattern.getWidth();
                    ingredient = pattern.getIngredients().get(index);
                }
                if (!ingredient.test(input.getItem(x + y * 3))) return false;
            }
        }
        return true;
    }

    public int recipeWidth() { return pattern.getWidth(); }
    public int recipeHeight() { return pattern.getHeight(); }

    @Override public ItemStack assemble(Container input, RegistryAccess registries) { return getResultItem(registries).copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return pattern.canCraftInDimensions(width, height); }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return pattern.getResultItem(registries); }
    @Override public NonNullList<Ingredient> getIngredients() { return pattern.getIngredients(); }
    @Override public ResourceLocation getId() { return pattern.getId(); }
    @Override public String getGroup() { return pattern.getGroup(); }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack getToastSymbol() { return new ItemStack(ModItems.POWERED_ARTISAN_TABLE.get()); }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ARTISAN_CRAFTING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.ARTISAN_CRAFTING.get(); }

    public static final class Serializer implements RecipeSerializer<ArtisanCraftingRecipe> {
        @Override
        public ArtisanCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new ArtisanCraftingRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public ArtisanCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ArtisanCraftingRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ArtisanCraftingRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe.pattern);
        }
    }
}
