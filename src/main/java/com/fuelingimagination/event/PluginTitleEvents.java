package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.client.TitleScreen;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "标题" plugin: right click opens the editor, shift-right-click projects the saved text to other
 * players within 32 blocks (never the sender) and starts a 15 second cooldown.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginTitleEvents {
    public static final String KEY_TEXT = "fuelingimagination:title_text";
    public static final String KEY_MAIN = "fuelingimagination:title_main";
    public static final String KEY_SUB = "fuelingimagination:title_sub";
    public static final String KEY_SELF = "fuelingimagination:title_self";
    public static final String KEY_CD = "fuelingimagination:title_cd";
    public static final long COOLDOWN_TICKS = 300L;
    private static final double RADIUS = 32.0D;

    private PluginTitleEvents() { }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!player.isShiftKeyDown()) return;
        ItemStack main = player.getMainHandItem();
        if (!CommandPlugins.isCommandBlock(main)
                || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_TITLE.get())) return;
        long now = player.level().getGameTime();
        if (now < CommandPlugins.getPluginLong(main, KEY_CD)) return;
        String text = CommandPlugins.getPluginString(main, KEY_TEXT);
        if (text.isEmpty()) return;
        boolean showMain = CommandPlugins.getPluginInt(main, KEY_MAIN) != 0;
        boolean showSub = CommandPlugins.getPluginInt(main, KEY_SUB) != 0;
        if (!showMain && !showSub) return;

        Component component = Component.literal(text);
        boolean showSelf = CommandPlugins.getPluginInt(main, KEY_SELF) != 0;
        double radiusSq = RADIUS * RADIUS;
        for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
            if (target == player) {
                if (!showSelf) continue;
            } else if (target.level() != player.level() || target.distanceToSqr(player) > radiusSq) {
                continue;
            }
            target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            if (showMain) target.connection.send(new ClientboundSetTitleTextPacket(component));
            if (showSub) target.connection.send(new ClientboundSetSubtitleTextPacket(component));
        }
        CommandPlugins.setPluginLong(main, KEY_CD, now + COOLDOWN_TICKS);
        event.setCanceled(true);
    }

    @Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() { }

        @SubscribeEvent
        public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
            if (!event.getLevel().isClientSide) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            if (minecraft.player.isShiftKeyDown()) return;
            ItemStack main = minecraft.player.getMainHandItem();
            if (!CommandPlugins.isCommandBlock(main)
                    || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_TITLE.get())) return;
            minecraft.setScreen(new TitleScreen(main));
            event.setCanceled(true);
        }
    }
}
