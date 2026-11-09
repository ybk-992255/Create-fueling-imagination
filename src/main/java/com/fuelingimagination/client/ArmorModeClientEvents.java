package com.fuelingimagination.client;

import com.fuelingimagination.content.armor.ArmorMode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import net.minecraftforge.client.event.ScreenEvent.Opening;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "fuelingimagination",
   value = {Dist.CLIENT}
)
public final class ArmorModeClientEvents {
   /** Ticks the sneak key has to be held before the suit lets go. Three seconds. */
   private static final int SNEAK_EXIT_TICKS = 60;
   private static final ResourceLocation CHAT_PANEL = new ResourceLocation("minecraft", "chat_panel");
   private static final ResourceLocation BOSS_EVENT = new ResourceLocation("minecraft", "boss_event");
   private static final ResourceLocation DEBUG_TEXT = new ResourceLocation("minecraft", "debug_text");

   private static int sneakTicks;
   private static boolean exitRequested;

   private ArmorModeClientEvents() {
   }

   private static boolean active() {
      return Minecraft.getInstance().player != null && ArmorMode.isActive(Minecraft.getInstance().player);
   }

   private static void drain(KeyMapping key) {
      key.setDown(false);

      while (key.consumeClick()) {
      }
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent event) {
      if (event.phase == Phase.START && active()) {
         Minecraft mc = Minecraft.getInstance();

         for (KeyMapping key : mc.options.keyHotbarSlots) {
            drain(key);
         }

         drain(mc.options.keyInventory);
         drain(mc.options.keySwapOffhand);
         drain(mc.options.keyDrop);
         drain(mc.options.keyPickItem);
         drain(mc.options.keyAttack);
         mc.player.getInventory().selected = ArmorMode.lockedSlot(mc.player);

         // Holding sneak for three seconds is the way out.
         if (mc.options.keyShift.isDown()) {
            sneakTicks++;
            if (sneakTicks >= SNEAK_EXIT_TICKS && !exitRequested) {
               exitRequested = true;
               com.fuelingimagination.content.armor.ArmorModeNetwork.requestExit();
            }
         } else {
            sneakTicks = 0;
            exitRequested = false;
         }
      } else if (event.phase == Phase.START) {
         sneakTicks = 0;
         exitRequested = false;
      }
   }

   /** Hides the hotbar, health, hunger and experience while the suit is worn. */
   @SubscribeEvent
   public static void hideHud(RenderGuiOverlayEvent.Pre event) {
      if (!active()) {
         return;
      }
      ResourceLocation id = event.getOverlay().id();
      if (CHAT_PANEL.equals(id) || BOSS_EVENT.equals(id) || DEBUG_TEXT.equals(id)) {
         return;
      }
      event.setCanceled(true);
   }

   @SubscribeEvent
   public static void openScreen(Opening event) {
      if (active() && event.getNewScreen() instanceof AbstractContainerScreen) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void scroll(MouseScrollingEvent event) {
      if (active() && Minecraft.getInstance().screen == null) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void interact(InteractionKeyMappingTriggered event) {
      if (active() && (event.isAttack() || event.isPickBlock())) {
         event.setCanceled(true);
         event.setSwingHand(false);
      }
   }

   @SubscribeEvent
   public static void hand(RenderHandEvent event) {
      if (active()) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void logout(LoggingOut event) {
      ArmorMode.clearClientState();
      sneakTicks = 0;
      exitRequested = false;
   }
}
