package com.fuelingimagination.content.armor;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ArmorModeNetwork {
   private static final String VERSION = "1";
   private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
      new ResourceLocation("fuelingimagination", "armor_mode"), () -> "1", "1"::equals, "1"::equals
   );

   private ArmorModeNetwork() {
   }

   public static void register() {
      CHANNEL.messageBuilder(ArmorModeNetwork.Exit.class, 1, NetworkDirection.PLAY_TO_SERVER)
         .encoder(ArmorModeNetwork.Exit::encode)
         .decoder(ArmorModeNetwork.Exit::decode)
         .consumerMainThread(ArmorModeNetwork.Exit::handle)
         .add();
      CHANNEL.messageBuilder(ArmorModeNetwork.State.class, 0, NetworkDirection.PLAY_TO_CLIENT)
         .encoder(ArmorModeNetwork.State::encode)
         .decoder(ArmorModeNetwork.State::decode)
         .consumerMainThread(ArmorModeNetwork.State::handle)
         .add();
   }

   public static void broadcast(ServerPlayer subject, boolean active, int slot) {
      CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> subject), new ArmorModeNetwork.State(subject.getUUID(), active, slot));
   }

   public static void send(ServerPlayer recipient, ServerPlayer subject, boolean active, int slot) {
      CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new ArmorModeNetwork.State(subject.getUUID(), active, slot));
   }

   /** The player asked to leave armour mode early by holding sneak. */
   public static void requestExit() {
      CHANNEL.sendToServer(new ArmorModeNetwork.Exit());
   }

   private static record Exit() {
      void encode(FriendlyByteBuf buffer) {
      }

      static ArmorModeNetwork.Exit decode(FriendlyByteBuf buffer) {
         return new ArmorModeNetwork.Exit();
      }

      void handle(Supplier<Context> context) {
         ServerPlayer sender = context.get().getSender();
         if (sender != null) {
            ArmorMode.stop(sender);
         }
         context.get().setPacketHandled(true);
      }
   }

   private static record State(UUID player, boolean active, int slot) {
      void encode(FriendlyByteBuf buffer) {
         buffer.writeUUID(this.player);
         buffer.writeBoolean(this.active);
         buffer.writeVarInt(this.slot);
      }

      static ArmorModeNetwork.State decode(FriendlyByteBuf buffer) {
         return new ArmorModeNetwork.State(buffer.readUUID(), buffer.readBoolean(), buffer.readVarInt());
      }

      void handle(Supplier<Context> context) {
         ArmorMode.acceptClientState(this.player, this.active, this.slot);
         context.get().setPacketHandled(true);
      }
   }
}
