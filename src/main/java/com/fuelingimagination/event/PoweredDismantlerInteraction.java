package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.dismantler.DismantlingRecipes;
import com.fuelingimagination.registry.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps block placement from stealing the dismantler's insert click.
 *
 * <p>A valid input is almost always a {@code BlockItem}. Without this hook the held
 * block would be placed against the dismantler before its own {@code use} method ran.
 * Denying only the item use and allowing the block use lets the interaction packet
 * travel normally, so the block entity can insert on the server and the client never
 * predicts a placement.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PoweredDismantlerInteraction {
    private PoweredDismantlerInteraction() { }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSecondaryUseActive()) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.POWERED_DISMANTLER.get())) return;
        ItemStack held = event.getItemStack();
        if (held.isEmpty() || !DismantlingRecipes.canProcess(held)) return;
        event.setUseItem(Event.Result.DENY);
        event.setUseBlock(Event.Result.ALLOW);
    }
}
