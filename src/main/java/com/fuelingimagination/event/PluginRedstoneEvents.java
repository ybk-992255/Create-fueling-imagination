package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.content.plugin.WirelessTypewriter;
import com.fuelingimagination.registry.ModItems;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * "Redstone" plugin as a hand-held wireless typewriter.
 *
 * <p>Right-click a wireless redstone terminal to capture its frequency, then press a key to bind it.
 * Right-click air (or a non-terminal block) toggles control mode, in which bound keys charge their
 * frequency while held. Shift-right-click opens the key map.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginRedstoneEvents {
    private PluginRedstoneEvents() { }

    public static boolean isTypewriter(ItemStack stack) {
        return CommandPlugins.isCommandBlock(stack)
                && CommandPlugins.has(stack, ModItems.COMMAND_PLUGIN_REDSTONE.get());
    }

    public static ItemStack held(Player player) {
        if (isTypewriter(player.getMainHandItem())) return player.getMainHandItem();
        if (isTypewriter(player.getOffhandItem())) return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack stack = held(player);
        if (stack.isEmpty() || player.isShiftKeyDown()) return;
        BlockEntity be = player.level().getBlockEntity(event.getPos());
        if (be instanceof RedstoneLinkBlockEntity link) {
            LinkBehaviour behaviour = link.getBehaviour(LinkBehaviour.TYPE);
            if (behaviour != null) {
                Couple<Frequency> key = behaviour.getNetworkKey();
                WirelessTypewriter.setPending(stack,
                        WirelessTypewriter.idOf(key.getFirst().getStack()),
                        WirelessTypewriter.idOf(key.getSecond().getStack()));
                player.displayClientMessage(Component.literal("已捕获频率，按一个键绑定"), true);
                event.setCanceled(true);
                return;
            }
        }
        WirelessTypewriter.setControl(stack, !WirelessTypewriter.control(stack));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack stack = held(player);
        if (stack.isEmpty() || player.isShiftKeyDown()) return;
        WirelessTypewriter.setControl(stack, !WirelessTypewriter.control(stack));
        event.setCanceled(true);
    }

    /** Binds the key the player pressed to the captured frequency. */
    public static void bindKey(ServerPlayer player, String key) {
        ItemStack stack = held(player);
        if (stack.isEmpty()) return;
        String[] pending = WirelessTypewriter.pending(stack);
        if (pending == null) return;
        WirelessTypewriter.bindKey(stack, key, pending[0], pending[1]);
        player.displayClientMessage(Component.literal("已绑定 " + key), true);
    }

    /** Removes a key binding. */
    public static void clearKey(ServerPlayer player, String key) {
        ItemStack stack = held(player);
        if (stack.isEmpty()) return;
        WirelessTypewriter.clearKey(stack, key);
    }

        /** Charges (or releases) the frequency bound to the key. */
    public static void press(ServerPlayer player, String key, boolean down) {
        ItemStack stack = held(player);
        if (stack.isEmpty() || !WirelessTypewriter.control(stack)) return;
        String[] freq = WirelessTypewriter.keyFrequency(stack, key);
        if (freq == null) return;
        Couple<Frequency> couple = Couple.create(
                Frequency.of(itemById(freq[0])), Frequency.of(itemById(freq[1])));
        LinkedControllerServerHandler.receivePressed(player.level(), player.blockPosition(),
                player.getUUID(), List.of(couple), down);
    }

    private static ItemStack itemById(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}