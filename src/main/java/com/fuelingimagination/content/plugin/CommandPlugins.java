package com.fuelingimagination.content.plugin;

import com.fuelingimagination.content.upgrade.UpgradeData;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Small helpers for "command block + installed plugin" checks. */
public final class CommandPlugins {
    private CommandPlugins() { }

    public static boolean isCommandBlock(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.COMMAND_BLOCK_Q.get());
    }

    public static ItemStack plugin(ItemStack commandBlock) {
        return UpgradeData.readSocket(commandBlock);
    }

    public static boolean has(ItemStack commandBlock, Item pluginItem) {
        ItemStack installed = plugin(commandBlock);
        return !installed.isEmpty() && installed.is(pluginItem);
    }

    /** Main-hand command block carrying this plugin, or an empty stack. */
    public static ItemStack mainHand(Player player, Item pluginItem) {
        ItemStack main = player.getMainHandItem();
        return isCommandBlock(main) && has(main, pluginItem) ? main : ItemStack.EMPTY;
    }

    /** Any command block in the main inventory / hotbar carrying this plugin. */
    public static ItemStack anyInInventory(Player player, Item pluginItem) {
        for (ItemStack stack : player.getInventory().items) {
            if (isCommandBlock(stack) && has(stack, pluginItem)) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Writes a long onto the installed plugin's own tag and stores it back. */
    public static void setPluginLong(ItemStack commandBlock, String key, long value) {
        ItemStack installed = plugin(commandBlock.copy());
        if (installed.isEmpty()) return;
        installed.getOrCreateTag().putLong(key, value);
        UpgradeData.applySocket(commandBlock, installed);
    }

    public static long getPluginLong(ItemStack commandBlock, String key) {
        ItemStack installed = plugin(commandBlock);
        return installed.isEmpty() ? 0L : installed.getOrCreateTag().getLong(key);
    }

    public static void setPluginInt(ItemStack commandBlock, String key, int value) {
        ItemStack installed = plugin(commandBlock.copy());
        if (installed.isEmpty()) return;
        installed.getOrCreateTag().putInt(key, value);
        UpgradeData.applySocket(commandBlock, installed);
    }

    public static int getPluginInt(ItemStack commandBlock, String key) {
        ItemStack installed = plugin(commandBlock);
        return installed.isEmpty() ? 0 : installed.getOrCreateTag().getInt(key);
    }

    public static void setPluginDouble(ItemStack commandBlock, String key, double value) {
        ItemStack installed = plugin(commandBlock.copy());
        if (installed.isEmpty()) return;
        installed.getOrCreateTag().putDouble(key, value);
        UpgradeData.applySocket(commandBlock, installed);
    }

    public static double getPluginDouble(ItemStack commandBlock, String key) {
        ItemStack installed = plugin(commandBlock);
        return installed.isEmpty() ? 0.0D : installed.getOrCreateTag().getDouble(key);
    }

    public static boolean hasPluginKey(ItemStack commandBlock, String key) {
        ItemStack installed = plugin(commandBlock);
        return !installed.isEmpty() && installed.getOrCreateTag().contains(key);
    }

    public static void setPluginString(ItemStack commandBlock, String key, String value) {
        ItemStack installed = plugin(commandBlock.copy());
        if (installed.isEmpty()) return;
        installed.getOrCreateTag().putString(key, value);
        UpgradeData.applySocket(commandBlock, installed);
    }

    public static String getPluginString(ItemStack commandBlock, String key) {
        ItemStack installed = plugin(commandBlock);
        return installed.isEmpty() ? "" : installed.getOrCreateTag().getString(key);
    }
}
