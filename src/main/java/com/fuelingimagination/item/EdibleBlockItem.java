package com.fuelingimagination.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EdibleBlockItem extends BlockItem {
    private final int useDuration;

    public EdibleBlockItem(Block block, Properties properties, int useDuration) {
        super(block, properties);
        this.useDuration = useDuration;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return useDuration;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        PotionUtils.addPotionTooltip(
                getFoodProperties().getEffects().stream().map(effect -> effect.getFirst()).toList(),
                tooltip, 1.0F);
        tooltip.add(Component.translatable("tooltip.fuelingimagination.sneak_to_place")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
