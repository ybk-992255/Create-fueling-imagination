package com.fuelingimagination.content.upgrade;

import com.fuelingimagination.FuelingImagination;
import com.simibubi.create.content.equipment.armor.BacktankItem;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The universal component's effect: cranking the tank by hand refills it.
 *
 * <p>Hold the tank in the main hand, sneak, and keep a Create hand crank in the off hand. Air is
 * added gradually so it feels like pumping rather than a single jump.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class BacktankHandPump {
    /** Air added per pump; ten ticks is half a second, so this is 50 air per second. */
    private static final float AIR_PER_PUMP = 25.0F;
    /** One pump every ten ticks. */
    private static final int PUMP_INTERVAL = 10;

    private BacktankHandPump() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!isPumping(player)) return;
        if (player.tickCount % PUMP_INTERVAL != 0) return;

        ItemStack tank = player.getMainHandItem();
        CompoundTag tag = tank.getOrCreateTag();
        int max = BacktankUtil.maxAir(tank);
        float air = Math.min(tag.getFloat("Air"), (float) max);
        if (air >= max) return;
        tag.putFloat("Air", Math.min((float) max, air + AIR_PER_PUMP));
        tank.setTag(tag);
    }

    /** Pumping takes precedence over using the tank, so clicks must not place or equip it. */
    @SubscribeEvent
    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (isPumping(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (isPumping(event.getEntity())) event.setCanceled(true);
    }

    /** Sneaking with an upgraded tank in the main hand and a hand crank in the off hand. */
    public static boolean isPumping(Player player) {
        if (!player.isShiftKeyDown()) return false;
        ItemStack tank = player.getMainHandItem();
        if (!(tank.getItem() instanceof BacktankItem)) return false;
        if (!UpgradeData.hasComponent(tank, "fuelingimagination:universal_component")) return false;
        ItemStack crank = player.getOffhandItem();
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(crank.getItem());
        return id != null && "create:hand_crank".equals(id.toString());
    }
}
