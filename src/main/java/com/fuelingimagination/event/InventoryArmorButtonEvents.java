package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.client.InventoryArmorHider;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Puts a tiny 6x6 toggle button in the bottom-left corner of the inventory's player preview so the
 * player can hide their armour models while looking at the character. No label: the effect itself
 * shows the state.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
public final class InventoryArmorButtonEvents {
    /** The preview box is (guiLeft + 26, guiTop + 8) .. (guiLeft + 75, guiTop + 78). */
    private static final int PREVIEW_LEFT = 26;
    private static final int PREVIEW_BOTTOM = 78;
    private static final int SIZE = 6;

    private InventoryArmorButtonEvents() { }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen inventory)) return;
        int x = inventory.getGuiLeft() + PREVIEW_LEFT;
        int y = inventory.getGuiTop() + PREVIEW_BOTTOM - SIZE;
        Button button = Button.builder(Component.empty(), b -> InventoryArmorHider.toggle())
                .bounds(x, y, SIZE, SIZE).build();
        event.addListener(button);
    }
}
