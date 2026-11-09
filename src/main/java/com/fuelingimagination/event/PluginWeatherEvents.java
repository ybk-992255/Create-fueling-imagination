package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "天气" plugin: main-hand command block cycles the weather type on right click and runs the
 * matching {@code /weather} command on left click. The 30 minute cooldown lives on the plugin itself.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginWeatherEvents {
    private static final String KEY_TYPE = "fuelingimagination:weather_type";
    private static final String KEY_COOLDOWN = "fuelingimagination:weather_cd";
    private static final long COOLDOWN_TICKS = 20L * 60L * 30L;
    private static final String[] NAMES = {"clear", "rain", "thunder"};

    private PluginWeatherEvents() { }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack main = player.getMainHandItem();
        if (!CommandPlugins.isCommandBlock(main)
                || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_WEATHER.get())) return;
        int type = (CommandPlugins.getPluginInt(main, KEY_TYPE) + 1) % NAMES.length;
        CommandPlugins.setPluginInt(main, KEY_TYPE, type);
        player.displayClientMessage(Component.literal("天气: " + NAMES[type]), true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        execute(event.getEntity(), event.getItemStack());
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        execute(event.getEntity(), event.getEntity().getMainHandItem());
    }

    private static void execute(Player player, ItemStack main) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (!CommandPlugins.isCommandBlock(main)
                || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_WEATHER.get())) return;
        long now = serverLevel.getGameTime();
        if (now < CommandPlugins.getPluginLong(main, KEY_COOLDOWN)) {
            player.displayClientMessage(Component.literal("天气指令冷却中"), true);
            return;
        }
        int type = CommandPlugins.getPluginInt(main, KEY_TYPE);
        serverLevel.getServer().getCommands()
                .performPrefixedCommand(player.createCommandSourceStack(), "weather " + NAMES[type] + " 6000");
        CommandPlugins.setPluginLong(main, KEY_COOLDOWN, now + COOLDOWN_TICKS);
    }
}
