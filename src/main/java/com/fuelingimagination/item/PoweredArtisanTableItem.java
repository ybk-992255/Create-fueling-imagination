package com.fuelingimagination.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public final class PoweredArtisanTableItem extends BlockItem {
    public PoweredArtisanTableItem(Block block, Properties properties) { super(block, properties); }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.fuelingimagination.table_drive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.fuelingimagination.table_requirements").withStyle(ChatFormatting.AQUA));
    }
}
