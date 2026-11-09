package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.block.GearCookieBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodBlockInteractions {
    private FoodBlockInteractions() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getUseBlock() != Event.Result.DENY
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof GearCookieBlock) {
            // Handle the plate before any held item's onItemUseFirst, also while sneaking.
            // A full plate still owns the click and must not place, eat or use the held item.
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
