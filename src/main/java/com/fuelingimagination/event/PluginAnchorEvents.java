package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.client.AnchorScreen;
import com.fuelingimagination.client.AnchorWindup;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "锚定" plugin: shift-right-click opens the coordinate screen, right-click starts the 3 second
 * windup and the server completes the teleport once it elapses.
 *
 * <p>Right-clicking air and right-clicking a block are two different events, so both are handled
 * through the same method here.
 */
public final class PluginAnchorEvents {
    private PluginAnchorEvents() { }

    @Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() { }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            AnchorWindup.clientTick();
            if (!AnchorWindup.isActive()) return;
            int tick = AnchorWindup.ticksSinceStart();
            if (tick == 0 || tick == 15 || tick == 30 || tick == 45) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null) {
                    minecraft.player.playSound(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 1.0F);
                }
            }
        }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (!event.getLevel().isClientSide) return;
            ItemStack main = mainHand();
            if (!isAnchor(main)) return;
            handleUse(event, main);
        }

        @SubscribeEvent
        public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
            if (!event.getLevel().isClientSide) return;
            ItemStack main = mainHand();
            if (!isAnchor(main)) return;
            handleUse(event, main);
        }

        private static ItemStack mainHand() {
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
        }

        private static boolean isAnchor(ItemStack main) {
            return CommandPlugins.isCommandBlock(main)
                    && CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_ANCHOR.get());
        }

        private static void handleUse(PlayerInteractEvent event, ItemStack main) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (minecraft.player.isShiftKeyDown()) {
                minecraft.setScreen(new AnchorScreen(main));
                event.setCanceled(true);
                return;
            }
            if (!CommandPlugins.hasPluginKey(main, ModCommands.KEY_X)) return;
            long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
            if (now < CommandPlugins.getPluginLong(main, ModCommands.KEY_CD)) {
                minecraft.player.displayClientMessage(Component.literal("锚定冷却中"), true);
                event.setCanceled(true);
                return;
            }
            AnchorWindup.start(CommandPlugins.getPluginDouble(main, ModCommands.KEY_X),
                    CommandPlugins.getPluginDouble(main, ModCommands.KEY_Y),
                    CommandPlugins.getPluginDouble(main, ModCommands.KEY_Z));
            minecraft.player.connection.sendCommand("cgf anchor tp");
            event.setCanceled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
    public static final class Server {
        /** Plays the portal-ignite sound a few ticks after arriving. */
        private static final String KEY_SOUND_AT = "fuelingimagination:anchor_sound_at";

        private Server() { }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (player.level().isClientSide()) return;
            var data = player.getPersistentData();
            long now = player.level().getGameTime();

            long soundAt = data.getLong(KEY_SOUND_AT);
            if (soundAt != 0L && now >= soundAt) {
                data.remove(KEY_SOUND_AT);
                player.level().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            long at = data.getLong(ModCommands.KEY_TP_AT);
            if (at == 0L || now < at) return;
            data.remove(ModCommands.KEY_TP_AT);
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            double x = data.getDouble(ModCommands.KEY_TP_X);
            double y = data.getDouble(ModCommands.KEY_TP_Y);
            double z = data.getDouble(ModCommands.KEY_TP_Z);
            serverPlayer.teleportTo(x, y, z);
            data.putLong(KEY_SOUND_AT, now + 5L);
        }

        /** Right-clicking a mob sends it straight to the saved spot (no windup, but still a cooldown). */
        @SubscribeEvent
        public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            ItemStack main = player.getMainHandItem();
            if (!CommandPlugins.isCommandBlock(main)
                    || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_ANCHOR.get())) return;
            long now = player.level().getGameTime();
            if (now < CommandPlugins.getPluginLong(main, ModCommands.KEY_CD)) return;
            if (!CommandPlugins.hasPluginKey(main, ModCommands.KEY_X)) return;
            if (!(event.getTarget() instanceof net.minecraft.world.entity.LivingEntity living)) return;
            living.teleportTo(CommandPlugins.getPluginDouble(main, ModCommands.KEY_X),
                    CommandPlugins.getPluginDouble(main, ModCommands.KEY_Y),
                    CommandPlugins.getPluginDouble(main, ModCommands.KEY_Z));
            CommandPlugins.setPluginLong(main, ModCommands.KEY_CD, now + ModCommands.COOLDOWN_TICKS);
            event.setCanceled(true);
        }
    }
}
