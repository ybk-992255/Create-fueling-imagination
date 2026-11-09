package com.fuelingimagination.item;

import com.fuelingimagination.content.armor.ArmorMode;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public final class ArmorControllerItem extends Item {
   public ArmorControllerItem() {
      super(new Properties().stacksTo(1));
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (hand == InteractionHand.MAIN_HAND && !player.getCooldowns().isOnCooldown(this)) {
         if (player instanceof ServerPlayer serverPlayer && !ArmorMode.activate(serverPlayer)) {
            return InteractionResultHolder.fail(stack);
         }

         player.startUsingItem(hand);
         return InteractionResultHolder.consume(stack);
      } else {
         return InteractionResultHolder.fail(stack);
      }
   }

   public int getUseDuration(ItemStack stack) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
      if (entity instanceof ServerPlayer player) {
         ArmorMode.fireWhileHeld(player);
      }
   }

   public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flags) {
      tooltip.add(Component.translatable("tooltip.FuelingImagination.armor_controller").withStyle(ChatFormatting.GOLD));
      tooltip.add(Component.translatable("tooltip.FuelingImagination.armor_controller_fire").withStyle(ChatFormatting.GRAY));
   }
}
