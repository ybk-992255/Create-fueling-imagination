package com.fuelingimagination.event;

import com.fuelingimagination.content.armor.ArmorMode;
import com.fuelingimagination.content.armor.ArmorModeNetwork;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(
   modid = "fuelingimagination"
)
public final class ArmorModeEvents {
   private ArmorModeEvents() {
   }

   @SubscribeEvent
   public static void tick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         ArmorMode.tick(player);
      }
   }

   @SubscribeEvent
   public static void logout(PlayerLoggedOutEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ArmorMode.stop(player);
      }
   }

   @SubscribeEvent
   public static void login(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ArmorMode.restoreCooldown(player);
      }
   }

   @SubscribeEvent
   public static void respawn(PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ArmorMode.restoreCooldown(player);
      }
   }

   @SubscribeEvent
   public static void changeDimension(PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ArmorMode.stop(player);
      }
   }

   @SubscribeEvent
   public static void death(LivingDeathEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ArmorMode.stop(player);
      }
   }

   @SubscribeEvent
   public static void clonePlayer(Clone event) {
      event.getEntity()
         .getPersistentData()
         .putLong("fuelingimagination:armor_cooldown_until", event.getOriginal().getPersistentData().getLong("fuelingimagination:armor_cooldown_until"));
   }

   @SubscribeEvent
   public static void tracking(StartTracking event) {
      if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer target) {
         ArmorModeNetwork.send(watcher, target, ArmorMode.isActive(target), ArmorMode.lockedSlot(target));
      }
   }

   @SubscribeEvent
   public static void stop(ServerStoppedEvent event) {
      ArmorMode.clearServerState();
   }

   @SubscribeEvent
   public static void stopping(ServerStoppingEvent event) {
      for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
         ArmorMode.stop(player);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void blockInteraction(RightClickBlock event) {
      boolean controller = event.getHand() == InteractionHand.MAIN_HAND && event.getItemStack().is((Item)ModItems.ARMOR_CONTROLLER.get());
      if (ArmorMode.isActive(event.getEntity()) || controller) {
         event.setUseBlock(Result.DENY);
         event.setUseItem(Result.DENY);
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (controller) {
            ((Item)ModItems.ARMOR_CONTROLLER.get()).use(event.getLevel(), event.getEntity(), event.getHand());
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void itemInteraction(RightClickItem event) {
      if (ArmorMode.isActive(event.getEntity())
         && (event.getHand() != InteractionHand.MAIN_HAND || !event.getItemStack().is((Item)ModItems.ARMOR_CONTROLLER.get()))) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void entityInteraction(EntityInteract event) {
      if (ArmorMode.isActive(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void entitySpecificInteraction(EntityInteractSpecific event) {
      if (ArmorMode.isActive(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void attack(AttackEntityEvent event) {
      if (ArmorMode.isActive(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public static void leftClick(LeftClickBlock event) {
      if (ArmorMode.isActive(event.getEntity())) {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void containerOpened(Open event) {
      if (event.getEntity() instanceof ServerPlayer player && ArmorMode.isActive(player)) {
         player.closeContainer();
      }
   }
}
