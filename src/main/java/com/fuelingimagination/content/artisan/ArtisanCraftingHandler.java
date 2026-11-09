package com.fuelingimagination.content.artisan;

import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.config.ArtisanConfig;
import com.fuelingimagination.recipe.ArtisanCraftingGrid;
import com.fuelingimagination.recipe.ArtisanRecipeLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.List;
import java.util.UUID;

public final class ArtisanCraftingHandler implements ArtisanModeHandler {
    public static final class Batch {
        private final ArtisanRecipeLookup.Match recipe;
        private final UUID owner;
        private int remainingCrafts;

        private Batch(ArtisanRecipeLookup.Match recipe, Player player) {
            this.recipe = recipe;
            owner = player.getUUID();
            remainingCrafts = ArtisanConfig.MAX_CRAFTS_PER_BATCH.get();
        }
    }

    @Override
    public boolean handleAction(PoweredArtisanTableBlockEntity table, Player player, ArtisanAction action, int detail) {
        if (action != ArtisanAction.CONFIRM || table.getLevel() == null) return false;
        if (table.isCrafting()) {
            table.setCraftingBatch(null);
            return true;
        }
        var grid = new ArtisanCraftingGrid(table.getInventory());
        var recipe = ArtisanRecipeLookup.find(grid, table.getLevel()).orElse(null);
        if (recipe == null) return fail(player, "no_recipe");
        table.setCraftingBatch(new Batch(recipe, player));
        return true;
    }

    @Override
    public void tickPowered(PoweredArtisanTableBlockEntity table) {
        Batch batch = table.getCraftingBatch();
        if (batch == null || table.getLevel() == null || table.getLevel().getServer() == null) return;
        ServerPlayer player = table.getLevel().getServer().getPlayerList().getPlayer(batch.owner);
        if (player == null || !player.isAlive() || player.level() != table.getLevel() || !batch.recipe.isCurrent(table.getLevel())) {
            table.setCraftingBatch(null);
            return;
        }
        // Bound tick work; a large batch never runs an unbounded loop in a packet handler.
        for (int i = 0; i < 4; i++) {
            if (!craftOnce(table, player, batch)) {
                table.setCraftingBatch(null);
                return;
            }
            if (--batch.remainingCrafts == 0) {
                table.setCraftingBatch(null);
                player.displayClientMessage(Component.translatable("gui.fuelingimagination.artisan.batch_limit"), true);
                return;
            }
        }
    }

    private boolean craftOnce(PoweredArtisanTableBlockEntity table, Player player, Batch batch) {
        SimpleContainer inventory = table.getInventory();
        ArtisanCraftingGrid input = new ArtisanCraftingGrid(inventory);
        // Keep the initially selected recipe even if leftovers could match a different recipe.
        if (!batch.recipe.matches(input, table.getLevel())) return false;
        ItemStack result = batch.recipe.assemble(input, table.getLevel());
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) return false;

        NonNullList<ItemStack> remainder;
        Player previousCraftingPlayer = ForgeHooks.getCraftingPlayer();
        ForgeHooks.setCraftingPlayer(player);
        try {
            remainder = batch.recipe.remaining(input);
        } finally {
            ForgeHooks.setCraftingPlayer(previousCraftingPlayer);
        }
        // Every occupied ingredient is consumed exactly once per craft.
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).isEmpty()) continue;
            inventory.removeItem(i, 1);
            ItemStack returned = remainder.get(i);
            if (returned.isEmpty()) continue;
            ItemStack current = inventory.getItem(i);
            if (current.isEmpty()) inventory.setItem(i, returned.copy());
            else if (ItemStack.isSameItemSameTags(current, returned)
                    && current.getCount() + returned.getCount() <= current.getMaxStackSize()) {
                current.grow(returned.getCount());
            } else spill(table, player, returned.copy());
        }
        result.onCraftedBy(table.getLevel(), player, result.getCount());
        ItemStack produced = result.copy();
        ItemStack output = inventory.getItem(PoweredArtisanTableBlockEntity.OUTPUT_SLOT);
        if (output.isEmpty()) {
            inventory.setItem(PoweredArtisanTableBlockEntity.OUTPUT_SLOT, result.copy());
            result.setCount(0);
        } else if (ItemStack.isSameItemSameTags(output, result)) {
            int moved = Math.min(result.getCount(), Math.max(0, output.getMaxStackSize() - output.getCount()));
            output.grow(moved);
            result.shrink(moved);
        }
        if (!result.isEmpty()) spill(table, player, result);
        inventory.setChanged();
        ForgeEventFactory.firePlayerCraftingEvent(player, produced, input);
        player.awardRecipes(List.of(batch.recipe.recipe()));
        return true;
    }

    private void spill(PoweredArtisanTableBlockEntity table, Player player, ItemStack stack) {
        var pos = table.getBlockPos();
        // Prefer the nearby operator's inventory; leave all remaining overflow at the table.
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64) {
            stack = ItemHandlerHelper.insertItemStacked(new PlayerMainInvWrapper(player.getInventory()), stack, false);
        }
        if (!stack.isEmpty()) Containers.dropItemStack(table.getLevel(), pos.getX() + 0.5, pos.getY() + 1,
                pos.getZ() + 0.5, stack);
    }

    public static boolean fail(Player player, String key) {
        player.displayClientMessage(Component.translatable("gui.fuelingimagination.artisan." + key), true);
        return false;
    }
}
