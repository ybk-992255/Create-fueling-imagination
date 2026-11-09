package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.recipe.ArtisanCraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, FuelingImagination.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FuelingImagination.MOD_ID);
    public static final RegistryObject<RecipeType<ArtisanCraftingRecipe>> ARTISAN_CRAFTING =
            TYPES.register("artisan_crafting", () -> new RecipeType<ArtisanCraftingRecipe>() {
                @Override public String toString() { return FuelingImagination.MOD_ID + ":artisan_crafting"; }
            });
    public static final RegistryObject<RecipeSerializer<ArtisanCraftingRecipe>> ARTISAN_CRAFTING_SERIALIZER =
            SERIALIZERS.register("artisan_crafting", ArtisanCraftingRecipe.Serializer::new);

    private ModRecipes() { }
}
