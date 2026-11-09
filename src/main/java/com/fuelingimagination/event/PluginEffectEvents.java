package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.client.EffectSelectScreen;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "效果" plugin: the player picks two vanilla potion effects by right clicking. While the command
 * block is carried the pair is applied for 5 seconds every 10 seconds, always at level 1.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginEffectEvents {
    public static final String KEY_A = "fuelingimagination:effect_a";
    public static final String KEY_B = "fuelingimagination:effect_b";
    private static final int PERIOD = 200;
    private static final int DURATION = 100;

    private PluginEffectEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        if (player.level().getGameTime() % PERIOD != 0) return;
        for (ItemStack stack : player.getInventory().items) {
            if (!CommandPlugins.isCommandBlock(stack)
                    || !CommandPlugins.has(stack, ModItems.COMMAND_PLUGIN_EFFECT.get())) continue;
            for (String key : new String[]{KEY_A, KEY_B}) {
                String id = CommandPlugins.getPluginString(stack, key);
                if (id.isEmpty()) continue;
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(id));
                if (effect != null) player.addEffect(new MobEffectInstance(effect, DURATION, 0));
            }
            return;
        }
    }

    @Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private Client() { }

        @SubscribeEvent
        public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
            if (!event.getLevel().isClientSide) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;
            ItemStack main = minecraft.player.getMainHandItem();
            if (!CommandPlugins.isCommandBlock(main)
                    || !CommandPlugins.has(main, ModItems.COMMAND_PLUGIN_EFFECT.get())) return;
            minecraft.setScreen(new EffectSelectScreen());
            event.setCanceled(true);
        }
    }
}
