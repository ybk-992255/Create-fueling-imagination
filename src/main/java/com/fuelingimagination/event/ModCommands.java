package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Debug helpers plus the anchor / effect plugin commands.
 *
 * <p>The arguments arrive as one greedy string and are split here so Brigadier never has to parse
 * several typed arguments in a row (which was rejecting values like {@code minecraft:speed}).
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class ModCommands {
    public static final String KEY_X = "fuelingimagination:anchor_x";
    public static final String KEY_Y = "fuelingimagination:anchor_y";
    public static final String KEY_Z = "fuelingimagination:anchor_z";
    public static final String KEY_TP_AT = "fuelingimagination:anchor_tp_at";
    public static final String KEY_TP_X = "fuelingimagination:anchor_tp_x";
    public static final String KEY_TP_Y = "fuelingimagination:anchor_tp_y";
    public static final String KEY_TP_Z = "fuelingimagination:anchor_tp_z";
    public static final String KEY_CD = "fuelingimagination:anchor_cd";
    public static final long WINDUP_TICKS = 60L;
    public static final long COOLDOWN_TICKS = 260L;

    private ModCommands() { }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("cgf")
                        .then(Commands.literal("spawn")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerPlayer player = source.getPlayerOrException();
                                    ServerLevel level = source.getLevel();
                                    boolean spawned = ScavengerSpawner.trySpawn(level, player);
                                    source.sendSuccess(() -> Component.translatable(spawned
                                            ? "command.fuelingimagination.spawn.ok"
                                            : "command.fuelingimagination.spawn.failed"), false);
                                    return spawned ? 1 : 0;
                                }))
                        .then(Commands.literal("anchor")
                                .then(Commands.literal("save")
                                        .then(Commands.argument("xyz", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ItemStack main = player.getMainHandItem();
                                                    if (!CommandPlugins.isCommandBlock(main)
                                                            || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_ANCHOR.get())) return 0;
                                                    String[] parts = split(StringArgumentType.getString(context, "xyz"));
                                                    if (parts.length < 3) return 0;
                                                    try {
                                                        CommandPlugins.setPluginDouble(main, KEY_X, Double.parseDouble(parts[0]));
                                                        CommandPlugins.setPluginDouble(main, KEY_Y, Double.parseDouble(parts[1]));
                                                        CommandPlugins.setPluginDouble(main, KEY_Z, Double.parseDouble(parts[2]));
                                                    } catch (NumberFormatException ignored) {
                                                        return 0;
                                                    }
                                                    return 1;
                                                })))
                                .then(Commands.literal("tp")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ItemStack main = player.getMainHandItem();
                                            if (!CommandPlugins.isCommandBlock(main)
                                                    || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_ANCHOR.get())) return 0;
                                            long now = player.level().getGameTime();
                                            if (now < CommandPlugins.getPluginLong(main, KEY_CD)) return 0;
                                            if (!CommandPlugins.hasPluginKey(main, KEY_X)) return 0;
                                            // Capture the destination now so swapping the held item
                                            // mid-windup cannot cancel the teleport, and start the
                                            // cooldown immediately.
                                            var data = player.getPersistentData();
                                            data.putDouble(KEY_TP_X, CommandPlugins.getPluginDouble(main, KEY_X));
                                            data.putDouble(KEY_TP_Y, CommandPlugins.getPluginDouble(main, KEY_Y));
                                            data.putDouble(KEY_TP_Z, CommandPlugins.getPluginDouble(main, KEY_Z));
                                            data.putLong(KEY_TP_AT, now + WINDUP_TICKS);
                                            CommandPlugins.setPluginLong(main, KEY_CD, now + COOLDOWN_TICKS);
                                            return 1;
                                        })))
                        .then(Commands.literal("effect")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("pair", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ItemStack main = player.getMainHandItem();
                                                    if (!CommandPlugins.isCommandBlock(main)
                                                            || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_EFFECT.get())) return 0;
                                                    String[] parts = split(StringArgumentType.getString(context, "pair"));
                                                    CommandPlugins.setPluginString(main, PluginEffectEvents.KEY_A,
                                                            parts.length > 0 ? parts[0] : "");
                                                    CommandPlugins.setPluginString(main, PluginEffectEvents.KEY_B,
                                                            parts.length > 1 ? parts[1] : "");
                                                    return 1;
                                                }))))
                        .then(Commands.literal("title")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    ItemStack main = player.getMainHandItem();
                                                    if (!CommandPlugins.isCommandBlock(main)
                                                            || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_TITLE.get())) return 0;
                                                    String[] parts = splitLimit(StringArgumentType.getString(context, "args"), 4);
                                                    if (parts.length < 4) return 0;
                                                    CommandPlugins.setPluginInt(main, PluginTitleEvents.KEY_MAIN, "1".equals(parts[0]) ? 1 : 0);
                                                    CommandPlugins.setPluginInt(main, PluginTitleEvents.KEY_SUB, "1".equals(parts[1]) ? 1 : 0);
                                                    CommandPlugins.setPluginInt(main, PluginTitleEvents.KEY_SELF, "1".equals(parts[2]) ? 1 : 0);
                                                    CommandPlugins.setPluginString(main, PluginTitleEvents.KEY_TEXT, parts[3]);
                                                    return 1;
                                                })))
                                .then(Commands.literal("clear")
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ItemStack main = player.getMainHandItem();
                                            if (!CommandPlugins.isCommandBlock(main)
                                                    || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_TITLE.get())) return 0;
                                            CommandPlugins.setPluginString(main, PluginTitleEvents.KEY_TEXT, "");
                                            return 1;
                                        })))
                        .then(Commands.literal("redstone")
                                .then(Commands.literal("bindkey")
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .executes(context -> {
                                                    PluginRedstoneEvents.bindKey(
                                                            context.getSource().getPlayerOrException(),
                                                            StringArgumentType.getString(context, "key"));
                                                    return 1;
                                                })))
                                .then(Commands.literal("clearkey")
                                        .then(Commands.argument("key", StringArgumentType.word())
                                                .executes(context -> {
                                                    PluginRedstoneEvents.clearKey(
                                                            context.getSource().getPlayerOrException(),
                                                            StringArgumentType.getString(context, "key"));
                                                    return 1;
                                                })))
                                    .then(Commands.literal("press")
                                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    String[] parts = split(StringArgumentType.getString(context, "args"));
                                                    if (parts.length < 2) return 0;
                                                    PluginRedstoneEvents.press(
                                                            context.getSource().getPlayerOrException(),
                                                            parts[0], "1".equals(parts[1]));
                                                    return 1;
                                                })))));
    }

    private static String[] split(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    /** Splits into at most {@code limit} parts, so the remainder keeps its spaces. */
    private static String[] splitLimit(String value, int limit) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+", limit);
    }
}
