package com.fuelingimagination.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Snapshot shared by preview, vanilla/Create matching, recipe events and remainder calculation. */
public final class ArtisanCraftingGrid extends SimpleContainer implements CraftingContainer {
    public ArtisanCraftingGrid(Container source) {
        super(9);
        for (int i = 0; i < 9; i++) setItem(i, source.getItem(i).copy());
    }

    @Override public int getWidth() { return 3; }
    @Override public int getHeight() { return 3; }
    @Override public List<ItemStack> getItems() {
        List<ItemStack> items = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) items.add(getItem(i));
        return items;
    }
}
