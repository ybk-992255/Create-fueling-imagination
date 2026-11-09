package com.fuelingimagination.content.armor;

import com.fuelingimagination.registry.ModItems;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class ArmorMode {
   public static final int DURATION_TICKS = 600;
   public static final int COOLDOWN_TICKS = 1800;
   public static final String COOLDOWN_UNTIL = "fuelingimagination:armor_cooldown_until";
   private static final Map<UUID, ArmorMode.Session> ACTIVE = new HashMap<>();
   private static final Map<UUID, Integer> CLIENT_ACTIVE = new HashMap<>();

   private ArmorMode() {
   }

   public static boolean isActive(LivingEntity entity) {
      return entity.level().isClientSide ? CLIENT_ACTIVE.containsKey(entity.getUUID()) : ACTIVE.containsKey(entity.getUUID());
   }

   public static int lockedSlot(Player player) {
      if (player.level().isClientSide) {
         return CLIENT_ACTIVE.getOrDefault(player.getUUID(), player.getInventory().selected);
      } else {
         ArmorMode.Session session = ACTIVE.get(player.getUUID());
         return session == null ? player.getInventory().selected : session.slot;
      }
   }

   private static long now(ServerPlayer player) {
      return player.server.overworld().getGameTime();
   }

   public static boolean activate(ServerPlayer player) {
      if (isActive(player)) {
         return true;
      } else if (player.isAlive()
         && !player.isSpectator()
         && player.getMainHandItem().is((Item)ModItems.ARMOR_CONTROLLER.get())
         && !player.getCooldowns().isOnCooldown((Item)ModItems.ARMOR_CONTROLLER.get())
         && cooldownRemaining(player) <= 0) {
         player.closeContainer();
         ACTIVE.put(player.getUUID(), new ArmorMode.Session(player.getInventory().selected, now(player)));
         player.getPersistentData().putLong("fuelingimagination:armor_cooldown_until", now(player) + 600L + 1800L);
         player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 600, 1, false, false, true));
         player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.JUMP, 600, 1, false, false, true));
         player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.CONDUIT_POWER, 600, 0, false, false, true));
         ArmorModeNetwork.broadcast(player, true, player.getInventory().selected);
         return true;
      } else {
         return false;
      }
   }

   public static void tick(ServerPlayer player) {
      ArmorMode.Session session = ACTIVE.get(player.getUUID());
      if (session != null) {
         if (now(player) < session.endsAt
            && player.isAlive()
            && !player.isSpectator()
            && player.getInventory().getItem(session.slot).is((Item)ModItems.ARMOR_CONTROLLER.get())) {
            if (player.getInventory().selected != session.slot) {
               player.getInventory().selected = session.slot;
               player.connection.send(new ClientboundSetCarriedItemPacket(session.slot));
            }

            if (player.containerMenu != player.inventoryMenu) {
               player.closeContainer();
            }
         } else {
            stop(player);
         }
      }
   }

   public static void stop(ServerPlayer player) {
      ArmorMode.Session session = ACTIVE.remove(player.getUUID());
      if (session != null) {
         player.stopUsingItem();
         player.getPersistentData().putLong("fuelingimagination:armor_cooldown_until", now(player) + 1800L);
         player.getCooldowns().addCooldown((Item)ModItems.ARMOR_CONTROLLER.get(), 1800);
         ArmorModeNetwork.broadcast(player, false, session.slot);
      }
   }

   public static int cooldownRemaining(ServerPlayer player) {
      return (int)Math.max(0L, Math.min(1800L, player.getPersistentData().getLong("fuelingimagination:armor_cooldown_until") - now(player)));
   }

   public static void restoreCooldown(ServerPlayer player) {
      int remaining = cooldownRemaining(player);
      if (remaining > 0) {
         player.getCooldowns().addCooldown((Item)ModItems.ARMOR_CONTROLLER.get(), remaining);
      }

      ArmorModeNetwork.send(player, player, false, player.getInventory().selected);
   }

   public static void fireWhileHeld(ServerPlayer player) {
      ArmorMode.Session session = ACTIVE.get(player.getUUID());
      long time = now(player);
      if (session != null && time < session.endsAt && time >= session.nextShot && player.getUseItem().getItem() == ModItems.ARMOR_CONTROLLER.get()) {
         ItemStack rocketStack = new ItemStack(Items.FIREWORK_ROCKET);
         CompoundTag fireworks = rocketStack.getOrCreateTagElement("Fireworks");
         fireworks.putByte("Flight", (byte)1);
         CompoundTag explosion = new CompoundTag();
         explosion.putByte("Type", (byte)1);
         explosion.putIntArray("Colors", new int[]{16738816, 16758062});
         explosion.putBoolean("Trail", true);
         ListTag explosions = new ListTag();
         explosions.add(explosion);
         fireworks.put("Explosions", explosions);
         Vec3 aim = player.getLookAngle();
         Vec3 start = player.getEyePosition().add(aim.scale(0.9));
         FireworkRocketEntity rocket = new FireworkRocketEntity(player.level(), rocketStack, player, start.x, start.y, start.z, true);
         rocket.shoot(aim.x, aim.y, aim.z, 1.6F, 0.0F);
         player.level().addFreshEntity(rocket);
         session.shotInBurst++;
         if (session.shotInBurst == 3) {
            session.shotInBurst = 0;
            session.nextShot = time + 35L;
         } else {
            session.nextShot = time + 5L;
         }
      }
   }

   public static void acceptClientState(UUID player, boolean active, int slot) {
      if (active) {
         CLIENT_ACTIVE.put(player, Math.max(0, Math.min(8, slot)));
      } else {
         CLIENT_ACTIVE.remove(player);
      }
   }

   public static void clearClientState() {
      CLIENT_ACTIVE.clear();
   }

   public static void clearServerState() {
      ACTIVE.clear();
   }

   private static final class Session {
      final int slot;
      final long endsAt;
      long nextShot;
      int shotInBurst;

      Session(int slot, long now) {
         this.slot = slot;
         this.endsAt = now + 600L;
         this.nextShot = now;
      }
   }
}
