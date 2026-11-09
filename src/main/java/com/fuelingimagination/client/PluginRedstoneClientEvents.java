package com.fuelingimagination.client;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.WirelessTypewriter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Sends key presses of the hand-held wireless typewriter to the server via the hidden cgf command. */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
public final class PluginRedstoneClientEvents {
    public static final List<String> ROWS = List.of(
            "1234567890", "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM");

    private PluginRedstoneClientEvents() { }

    /** Printable name for a letter or digit key, null for anything else. */
    public static String keyName(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + key - GLFW.GLFW_KEY_A));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + key - GLFW.GLFW_KEY_0));
        }
        return null;
    }

    @SubscribeEvent
    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        openMap(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        openMap(event);
    }

    private static void openMap(net.minecraftforge.event.entity.player.PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isShiftKeyDown()) return;
        ItemStack stack = com.fuelingimagination.event.PluginRedstoneEvents.held(minecraft.player);
        if (stack.isEmpty()) return;
        minecraft.setScreen(new KeyboardScreen(stack));
        event.setCanceled(true);
    }

    /** While in control mode the bound keys must only drive signals, not the player. */
    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options == null) return;
        ItemStack stack = com.fuelingimagination.event.PluginRedstoneEvents.held(minecraft.player);
        if (stack.isEmpty() || !WirelessTypewriter.control(stack)) return;
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        minecraft.options.keySprint.setDown(false);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) return;
        ItemStack stack = com.fuelingimagination.event.PluginRedstoneEvents.held(minecraft.player);
        if (stack.isEmpty()) return;
        String name = keyName(event.getKey());
        if (name == null) return;
        boolean bound = WirelessTypewriter.keyFrequency(stack, name) != null;
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (WirelessTypewriter.pending(stack) != null) {
                minecraft.player.connection.sendCommand("cgf redstone bindkey " + name);
            } else if (WirelessTypewriter.control(stack) && bound) {
                minecraft.player.connection.sendCommand("cgf redstone press " + name + " 1");
            }
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (WirelessTypewriter.control(stack) && bound) {
                minecraft.player.connection.sendCommand("cgf redstone press " + name + " 0");
            }
        }
    }
}