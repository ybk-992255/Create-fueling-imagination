package com.fuelingimagination.content.artisan;

import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Server-side extension point. Each mode can later supply its own processing logic. */
public interface ArtisanModeHandler {
    default void onSelected(PoweredArtisanTableBlockEntity table) { }

    default void onDeselected(PoweredArtisanTableBlockEntity table) { }

    /** Called only while the table has sufficient usable kinetic power. */
    default void tickPowered(PoweredArtisanTableBlockEntity table) { }

    /** Validated menu action. CLEAN detail is a part index (0..8); other actions use -1. */
    default boolean handleAction(PoweredArtisanTableBlockEntity table, Player player, ArtisanAction action, int detail) {
        return false;
    }

    /** Read-only view hook, called on the client only when a main item is present. */
    default List<Component> describeMainItem(ItemStack stack, Level level) { return List.of(); }
}
