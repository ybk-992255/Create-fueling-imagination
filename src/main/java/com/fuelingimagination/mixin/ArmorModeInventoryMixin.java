package com.fuelingimagination.mixin;

import com.fuelingimagination.content.armor.ArmorMode;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {ServerGamePacketListenerImpl.class},
   remap = false
)
public abstract class ArmorModeInventoryMixin {
   @Unique
   private ServerGamePacketListenerImpl cgf$connection() {
      return (ServerGamePacketListenerImpl) (Object) this;
   }

   @Unique
   private boolean cgf$locked(Packet<ServerGamePacketListener> packet) {
      ServerGamePacketListenerImpl connection = this.cgf$connection();
      PacketUtils.ensureRunningOnSameThread(packet, connection, connection.player.serverLevel());
      return ArmorMode.isActive(connection.player);
   }

   @Unique
   private void cgf$resyncInventory() {
      ServerPlayer player = this.cgf$connection().player;
      player.inventoryMenu.sendAllDataToRemote();
      if (player.containerMenu != player.inventoryMenu) {
         player.containerMenu.sendAllDataToRemote();
      }
   }

   @Inject(
      method = {"m_7798_(Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockSelectedSlot(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         ServerPlayer player = this.cgf$connection().player;
         int slot = ArmorMode.lockedSlot(player);
         if (slot >= 0 && slot < 9) {
            player.getInventory().selected = slot;
         }

         player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_5914_(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockContainer(ServerboundContainerClickPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_5964_(Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockCreativeInventory(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_7502_(Lnet/minecraft/network/protocol/game/ServerboundPlayerActionPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockActions(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet) && packet.getAction() != Action.RELEASE_USE_ITEM) {
         ServerGamePacketListenerImpl connection = this.cgf$connection();
         connection.ackBlockChangesUpTo(packet.getSequence());
         connection.send(new ClientboundBlockUpdatePacket(connection.player.serverLevel(), packet.getPos()));
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_6946_(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockEntityInteraction(ServerboundInteractPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_7965_(Lnet/minecraft/network/protocol/game/ServerboundPickItemPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockPickItem(ServerboundPickItemPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_7191_(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockRecipePlacement(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }

   @Inject(
      method = {"m_6557_(Lnet/minecraft/network/protocol/game/ServerboundContainerButtonClickPacket;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 1
   )
   private void cgf$lockContainerButtons(ServerboundContainerButtonClickPacket packet, CallbackInfo ci) {
      if (this.cgf$locked(packet)) {
         this.cgf$resyncInventory();
         ci.cancel();
      }
   }
}
