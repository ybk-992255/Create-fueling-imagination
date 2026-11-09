package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * "触及" plugin: carrying a command block with the touch plugin multiplies block and entity reach
 * by ten (+900% base).
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginTouchEvents {
    private static final UUID BLOCK_REACH_ID = UUID.fromString("6a0f3e2a-1d4b-4c8e-9f01-1a2b3c4d5e01");
    private static final UUID ENTITY_REACH_ID = UUID.fromString("6a0f3e2a-1d4b-4c8e-9f01-1a2b3c4d5e02");

    private PluginTouchEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        boolean active = !CommandPlugins.anyInInventory(player, ModItems.COMMAND_PLUGIN_TOUCH.get()).isEmpty();
        apply(player.getAttribute(ForgeMod.BLOCK_REACH.get()), BLOCK_REACH_ID, active);
        apply(player.getAttribute(ForgeMod.ENTITY_REACH.get()), ENTITY_REACH_ID, active);
    }

    private static void apply(AttributeInstance instance, UUID id, boolean active) {
        if (instance == null) return;
        if (active) {
            if (instance.getModifier(id) == null) {
                instance.addTransientModifier(new AttributeModifier(id, "fuelingimagination:touch", 9.0D,
                        AttributeModifier.Operation.MULTIPLY_BASE));
            }
        } else {
            instance.removeModifier(id);
        }
    }
}
