package com.fuelingimagination.content.artisan;

import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.content.damaged.DamagedBlocks;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Repair mode: takes a damaged block item and restores it.
 *
 * <p>Disassembly lays the block's crafting ingredients out as parts. One or two of them are
 * broken (replaced by a damaged component) and must be swapped for the real ingredient by
 * the player; the rest are merely dirty and get wiped. Reassembly needs both conditions met.
 *
 * <p>Repair costs nothing beyond the ingredients the player supplies for the broken parts.
 */
public final class DamagedBlockRepairHandler implements ArtisanModeHandler {
    /** Never more than this many parts break, regardless of recipe size. */
    private static final int MAX_BROKEN_PARTS = 2;

    /** Only damaged block items may enter repair mode. */
    public static boolean supports(ItemStack stack) {
        return DamagedBlocks.isDamaged(stack) || stack.is(ModItems.DAMAGED_COMMAND_BLOCK.get());
    }

    @Override
    public boolean handleAction(PoweredArtisanTableBlockEntity table, Player player, ArtisanAction action, int detail) {
        if (table.getLevel() == null || table.getLevel().isClientSide) return false;
        ArtisanProcessStage stage = table.getProcessStage();
        if (action == ArtisanAction.CANCEL && stage != ArtisanProcessStage.IDLE) {
            // Cancelling scraps the block: clean parts are handed over, dirty ones are lost.
            table.scrap(player);
            return true;
        }
        ItemStack original = table.getInventory().getItem(PoweredArtisanTableBlockEntity.MAIN_SLOT);
        if (!supports(original)) {
            return ArtisanCraftingHandler.fail(player,
                    DamagedBlocks.canBeDamaged(original) ? "repair_block_intact" : "repair_block_only");
        }
        if (action == ArtisanAction.CRANK) {
            action = stage == ArtisanProcessStage.IDLE ? ArtisanAction.DISASSEMBLE : ArtisanAction.ASSEMBLE;
        }
        if (action == ArtisanAction.DISASSEMBLE && stage == ArtisanProcessStage.IDLE) {
            disassemble(table, original);
            return true;
        }
        if (action == ArtisanAction.CLEAN && (stage == ArtisanProcessStage.REPAIR_DISASSEMBLED
                || stage == ArtisanProcessStage.REPAIR_CLEANING)) {
            return table.cleanRepairPart(detail);
        }
        if (action == ArtisanAction.ASSEMBLE) {
            if (stage != ArtisanProcessStage.REPAIR_READY || !table.canAssemble()) {
                return ArtisanCraftingHandler.fail(player,
                        table.getDirtyParts() != 0 ? "repair_clean_first" : "repair_parts_missing");
            }
            // Clearing the flag is the whole repair: the stack is the same item, just usable again.
            DamagedBlocks.repair(original);
            table.getInventory().setChanged();
            table.resetProcess();
            return true;
        }
        return false;
    }

    private void disassemble(PoweredArtisanTableBlockEntity table, ItemStack original) {
        Level level = table.getLevel();
        // The damaged command block has no recipe: it always breaks down into its frame and core.
        if (original.is(ModItems.DAMAGED_COMMAND_BLOCK.get())) {
            NonNullList<ItemStack> parts = NonNullList.withSize(9, ItemStack.EMPTY);
            parts.set(3, new ItemStack(ModItems.DAMAGED_COMMAND_BLOCK_FRAME.get()));
            parts.set(5, new ItemStack(ModItems.COMMAND_CORE.get()));
            NonNullList<ItemStack> expected = NonNullList.withSize(9, ItemStack.EMPTY);
            for (int i = 0; i < 9; i++) if (!parts.get(i).isEmpty()) expected.set(i, parts.get(i).copy());
            table.beginRepair(parts, 0, expected, new Ingredient[9], "");
            return;
        }
        CraftingRecipe recipe = findRecipe(original, level);
        NonNullList<ItemStack> parts = findParts(original, recipe, level);
        Ingredient[] ingredients = gridIngredients(recipe);
        NonNullList<ItemStack> expected = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) {
            if (!parts.get(i).isEmpty()) expected.set(i, parts.get(i).copy());
        }

        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < 9; i++) if (!parts.get(i).isEmpty()) occupied.add(i);

        int brokenMask = chooseBrokenParts(occupied, parts, level.random);
        for (int i = 0; i < 9; i++) {
            if ((brokenMask & 1 << i) != 0) parts.set(i, new ItemStack(ModItems.DAMAGED_COMPONENT.get()));
        }

        // Broken parts cannot be wiped; only the intact ones can be dirty.
        List<Integer> cleanable = new ArrayList<>();
        for (int index : occupied) if ((brokenMask & 1 << index) == 0) cleanable.add(index);
        int dirtyMask = 0;
        if (!cleanable.isEmpty()) {
            int dirtCount = 1 + level.random.nextInt(Math.min(3, cleanable.size()));
            for (int i = 0; i < dirtCount; i++) {
                dirtyMask |= 1 << cleanable.remove(level.random.nextInt(cleanable.size()));
            }
        }
        table.beginRepair(parts, dirtyMask, expected, ingredients,
                recipe == null ? "" : recipe.getId().toString());
    }

    /** The recipe a repair is based on, or null for blocks that cannot be crafted. */
    public static CraftingRecipe findRecipe(ItemStack original, Level level) {
        return level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(candidate -> candidate.getResultItem(level.registryAccess()).is(original.getItem())
                        && !candidate.getIngredients().isEmpty())
                .sorted(Comparator.comparing(candidate -> candidate.getId().toString()))
                .findFirst().orElse(null);
    }

    /**
     * Lays the recipe's ingredients onto the 3x3 grid, keeping the real {@link Ingredient}s.
     *
     * <p>Validation must use these rather than a representative stack: Create's recipes accept
     * tags such as {@code forge:storage_blocks/iron}, and comparing against the first item of
     * such a tag rejects every other valid choice.
     */
    public static Ingredient[] gridIngredients(CraftingRecipe recipe) {
        Ingredient[] grid = new Ingredient[9];
        if (recipe == null) return grid;
        if (recipe instanceof ShapedRecipe shaped && shaped.getWidth() <= 3 && shaped.getHeight() <= 3) {
            int dx = (3 - shaped.getWidth()) / 2, dy = (3 - shaped.getHeight()) / 2;
            for (int y = 0; y < shaped.getHeight(); y++) {
                for (int x = 0; x < shaped.getWidth(); x++) {
                    grid[(y + dy) * 3 + x + dx] = shaped.getIngredients().get(y * shaped.getWidth() + x);
                }
            }
        } else if (recipe.getIngredients().size() <= 9) {
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                grid[i] = recipe.getIngredients().get(i);
            }
        }
        return grid;
    }

    /**
     * Picks which parts break: at least one, at most {@value #MAX_BROKEN_PARTS}.
     *
     * <p>A part made of a Create precision mechanism is always broken first, because that is
     * the part a repair is meant to cost; the rest of the budget is rolled at random.
     */
    private int chooseBrokenParts(List<Integer> occupied, NonNullList<ItemStack> parts, RandomSource random) {
        if (occupied.isEmpty()) return 0;
        int target = Math.min(occupied.size(), 1 + random.nextInt(MAX_BROKEN_PARTS));

        List<Integer> mechanism = new ArrayList<>();
        for (int index : occupied) {
            if (parts.get(index).is(com.simibubi.create.AllItems.PRECISION_MECHANISM.get())) mechanism.add(index);
        }

        List<Integer> candidates = new ArrayList<>(occupied);
        int mask = 0;
        if (!mechanism.isEmpty()) {
            int index = mechanism.get(random.nextInt(mechanism.size()));
            mask |= 1 << index;
            candidates.remove(Integer.valueOf(index));
        }
        while (Integer.bitCount(mask) < target && !candidates.isEmpty()) {
            mask |= 1 << candidates.remove(random.nextInt(candidates.size()));
        }
        return mask;
    }

    /** Lays the block's crafting ingredients out on the 3x3 display grid, if it has a recipe. */
    private NonNullList<ItemStack> findParts(ItemStack original, CraftingRecipe recipe, Level level) {
        NonNullList<ItemStack> parts = NonNullList.withSize(9, ItemStack.EMPTY);
        if (recipe instanceof ShapedRecipe shaped && shaped.getWidth() <= 3 && shaped.getHeight() <= 3) {
            int dx = (3 - shaped.getWidth()) / 2, dy = (3 - shaped.getHeight()) / 2;
            for (int y = 0; y < shaped.getHeight(); y++) {
                for (int x = 0; x < shaped.getWidth(); x++) {
                    parts.set((y + dy) * 3 + x + dx,
                            representative(shaped.getIngredients().get(y * shaped.getWidth() + x)));
                }
            }
        } else if (recipe != null && recipe.getIngredients().size() <= 9) {
            for (int i = 0; i < recipe.getIngredients().size(); i++) {
                parts.set(i, representative(recipe.getIngredients().get(i)));
            }
        }
        // No recipe (creative blocks, stonecut variants): one whole unit to clean.
        if (parts.stream().allMatch(ItemStack::isEmpty)) parts.set(4, new ItemStack(original.getItem()));
        return parts;
    }

    private ItemStack representative(Ingredient ingredient) {
        ItemStack[] choices = ingredient.getItems();
        return choices.length == 0 ? ItemStack.EMPTY : choices[0].copyWithCount(1);
    }
}
