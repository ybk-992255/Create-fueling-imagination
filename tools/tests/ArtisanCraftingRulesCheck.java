import com.creategourmetfood.content.artisan.MaterialRetention;
import com.creategourmetfood.recipe.ArtisanRecipePolicy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/** Focused checks for this change; runs without starting Minecraft or adding test dependencies. */
public final class ArtisanCraftingRulesCheck {
    public static void main(String[] args) {
        boolean[] nineRedstone = new boolean[9];
        Arrays.fill(nineRedstone, true);
        double[] rolls = {0.01, 0.15, 0.09, 0.8, 0.1, 0.999, 0.02, 0.5, 0.7};
        AtomicInteger cursor = new AtomicInteger();
        boolean[] consumed = MaterialRetention.consumedSlots(nineRedstone, 0.1, () -> rolls[cursor.getAndIncrement()]);
        check(Arrays.equals(consumed, new boolean[]{false, true, false, true, true, true, false, true, true}),
                "Nine occupied slots must get independent retention outcomes, including the 10% boundary");
        check(cursor.get() == 9, "Nine ingredients require nine independent rolls");

        cursor.set(0);
        boolean[] sparse = MaterialRetention.consumedSlots(new boolean[]{false, true, false, true}, 0.1,
                () -> rolls[cursor.getAndIncrement()]);
        check(Arrays.equals(sparse, new boolean[]{false, false, false, true}) && cursor.get() == 2,
                "Empty slots must neither consume nor use a random roll");
        check(Arrays.equals(MaterialRetention.consumedSlots(nineRedstone, 0.0, () -> 0.0), nineRedstone),
                "Zero retention must consume every ingredient");
        check(Arrays.equals(MaterialRetention.consumedSlots(nineRedstone, 1.0, () -> 0.999999), new boolean[9]),
                "Full retention must preserve every ingredient");

        // Exercise batch depletion with independent savings: stop when one required slot runs out.
        int[] counts = {2, 2};
        double[][] batchRolls = {{0.01, 0.9}, {0.9, 0.9}};
        int crafts = 0;
        while (counts[0] > 0 && counts[1] > 0) {
            final double[] row = batchRolls[crafts];
            cursor.set(0);
            boolean[] plan = MaterialRetention.consumedSlots(new boolean[]{true, true}, 0.1,
                    () -> row[cursor.getAndIncrement()]);
            for (int i = 0; i < 2; i++) if (plan[i]) counts[i]--;
            crafts++;
        }
        check(crafts == 2 && counts[0] == 1 && counts[1] == 0, "Saved materials remain when a recipe becomes impossible");

        check(ArtisanRecipePolicy.allows("create", "any_future_component", false), "Include Create outputs dynamically");
        check(!ArtisanRecipePolicy.allows("createaddition", "electric_motor", false), "Use the exact Create namespace");
        check(ArtisanRecipePolicy.allows("minecraft", "redstone_block", false), "Include redstone blocks regardless of category");
        check(ArtisanRecipePolicy.allows("minecraft", "powered_rail", false), "Include powered rails");
        check(ArtisanRecipePolicy.allows("minecraft", "bamboo_button", false), "Include wood variants of redstone controls");
        check(ArtisanRecipePolicy.allows("minecraft", "category_component", true), "Include the redstone crafting category");
        check(!ArtisanRecipePolicy.allows("minecraft", "diamond_sword", false), "Do not admit unrelated vanilla recipes");
        check(!ArtisanRecipePolicy.allows("other_mod", "redstone_block", true), "Do not admit unrelated namespaces");
        System.out.println("PASS: material retention, depletion boundaries and Create/redstone eligibility");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
