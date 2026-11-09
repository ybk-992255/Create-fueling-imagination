package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.entity.Scavenger;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The scavenger dies rather than turning into a zombie villager. */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class ScavengerEvents {
    private ScavengerEvents() { }

    @SubscribeEvent
    public static void onConversion(LivingConversionEvent.Pre event) {
        if (event.getEntity() instanceof Scavenger) event.setCanceled(true);
    }
}
