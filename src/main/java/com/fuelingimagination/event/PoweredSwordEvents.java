package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.upgrade.UpgradeData;
import com.fuelingimagination.item.PoweredSwordItem;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

/**
 * Counts kills for the powered sword.
 *
 * <p>Hooked on death rather than on {@code hurtEnemy}, so sweep-attack kills and any other kill
 * credited to the sword's wielder are all counted. Only hostile and neutral mobs are tallied.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PoweredSwordEvents {
    private static final int KILL_CAP = 999;

    /** Vanilla mobs that are neutral (attack only when provoked) rather than purely passive. */
    private static final Set<EntityType<?>> NEUTRAL = Set.of(
            EntityType.IRON_GOLEM, EntityType.WOLF, EntityType.POLAR_BEAR, EntityType.ENDERMAN,
            EntityType.ZOMBIFIED_PIGLIN, EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.PANDA,
            EntityType.BEE, EntityType.DOLPHIN, EntityType.GOAT, EntityType.FOX, EntityType.PUFFERFISH,
            EntityType.TURTLE);

    private PoweredSwordEvents() { }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ItemStack sword = player.getMainHandItem();
        if (!(sword.getItem() instanceof PoweredSwordItem)) return;
        countKill(sword, event.getEntity());
    }

    /** Tallies one kill if the victim is hostile or neutral; shared with the saw's direct hits. */
    public static void countKill(ItemStack sword, LivingEntity target) {
        if (!isHostileOrNeutral(target)) return;
        var tag = sword.getOrCreateTag();
        int kills = tag.getInt(PoweredSwordItem.NBT_KILLS);
        if (kills < KILL_CAP) tag.putInt(PoweredSwordItem.NBT_KILLS, kills + 1);
    }

    private static boolean isHostileOrNeutral(LivingEntity entity) {
        return entity instanceof Monster || NEUTRAL.contains(entity.getType());
    }

    /** The drill only speeds up mining while the wearer actually has backtank air. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof PoweredSwordItem)) return;
        if (UpgradeData.hasComponent(tool, "create:mechanical_drill")
                && !BacktankUtil.getAllWithAir(player).isEmpty()) {
            event.setNewSpeed(12.0F); // gold tool speed
        }
    }

    /**
     * Unleashing a mob leashed by the rope pulley must not drop a lead item (no lead was spent).
     * Cancel the vanilla right-click unleash and drop the leash silently instead.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getTarget() instanceof Mob mob) || !mob.isLeashed()) return;
        if (mob.getLeashHolder() != event.getEntity()) return;
        if (!mob.getPersistentData().getBoolean("fuelingimagination:rope_leashed")) return;
        mob.getPersistentData().remove("fuelingimagination:rope_leashed");
        mob.dropLeash(true, false);
        event.setCanceled(true);
    }

    /**
     * The blaze burner ignites every creature the sword hits — the main target, the vanilla sweep
     * targets and the saw's panel sweep alike — regardless of which accessory performs the sweep.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        ItemStack sword = player.getMainHandItem();
        if (!(sword.getItem() instanceof PoweredSwordItem)) return;
        if (UpgradeData.hasComponent(sword, "create:blaze_burner")) {
            event.getEntity().setSecondsOnFire(5);
        }
    }

    /** Creature packages must not be opened by shift-right-click; they release on place+break only. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() && isCreaturePackage(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isShiftKeyDown() && isCreaturePackage(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    private static boolean isCreaturePackage(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("fuelingimagination:entity");
    }
}
