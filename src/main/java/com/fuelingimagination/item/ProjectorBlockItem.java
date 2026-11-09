package com.fuelingimagination.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.List;

/** Block item that explains what the projector does in its tooltip. */
public class ProjectorBlockItem extends BlockItem {
    private final String tooltipKey;
    private final String hintKey;

    public ProjectorBlockItem(Block block, Properties properties, String tooltipKey, String hintKey) {
        super(block, properties);
        this.tooltipKey = tooltipKey;
        this.hintKey = hintKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(hintKey).withStyle(ChatFormatting.GRAY));
    }
}