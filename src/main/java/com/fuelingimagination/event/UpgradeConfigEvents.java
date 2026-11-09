package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.upgrade.UpgradeConfig;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Loads the data-driven upgrade layouts whenever datapacks reload. */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class UpgradeConfigEvents {
    private UpgradeConfigEvents() { }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(UpgradeConfig.INSTANCE);
    }
}
