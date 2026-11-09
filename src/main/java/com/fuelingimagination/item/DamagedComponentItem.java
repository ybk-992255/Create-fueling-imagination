package com.fuelingimagination.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The repair byproduct. Its name is light grey so it reads as scrap rather than loot. */
public final class DamagedComponentItem extends Item {
    public DamagedComponentItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.GRAY);
    }
}
