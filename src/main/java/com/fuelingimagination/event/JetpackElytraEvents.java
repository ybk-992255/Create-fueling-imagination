package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.upgrade.UpgradeData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Simple approximation of the jetpack's elytra slot flight: while wearing a jetpack that has the
 * elytra installed and the jetpack is not holding the player up (hover/thrust off), falling enters
 * vanilla elytra gliding. The boost pushes horizontally in the movement direction only, and the
 * player is immune to fall damage while gliding and for 40 ticks after landing.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class JetpackElytraEvents {
    /** Falling faster than this counts as "hover off"; above it the jetpack is holding the player up. */
    private static final double GLIDE_TRIGGER = -0.2D;
    /** Per-tick forward boost that counteracts elytra drag (the "reduced drag" effect). */
    private static final double BOOST = 0.012D;
    /** Maximum horizontal speed before the boost stops applying. */
    private static final double MAX_FORWARD = 3.0D;
    private static final String GLIDING_FLAG = "fuelingimagination:gliding";
    private static final String IMMUNITY_FLAG = "fuelingimagination:landing_immunity";

    private JetpackElytraEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide()) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!isElytraJetpack(chest)) return;

        boolean gliding = player.getPersistentData().getBoolean(GLIDING_FLAG);

        // Fall-damage immunity: while gliding, and for 40 ticks after landing.
        int immunity = player.getPersistentData().getInt(IMMUNITY_FLAG);
        if (gliding) {
            player.fallDistance = 0.0F;
            player.getPersistentData().putInt(IMMUNITY_FLAG, 40);
        } else if (immunity > 0) {
            player.fallDistance = 0.0F;
            player.getPersistentData().putInt(IMMUNITY_FLAG, immunity - 1);
        }

        if (gliding) {
            if (player.onGround() || player.isInWater()) {
                player.stopFallFlying();
                player.getPersistentData().remove(GLIDING_FLAG);
            } else {
                if (!player.isFallFlying()) player.startFallFlying();
                // No upward thrust while gliding: neutralise any upward velocity (e.g. from a jump),
                // keeping only horizontal, movement-direction motion.
                Vec3 vel = player.getDeltaMovement();
                if (vel.y > 0.0D) {
                    player.setDeltaMovement(vel.x, 0.0D, vel.z);
                    vel = player.getDeltaMovement();
                }
                // Horizontal boost in the movement direction only.
                double horiz = vel.horizontalDistance();
                if (horiz > 1.0E-4D && horiz < MAX_FORWARD) {
                    Vec3 dir = new Vec3(vel.x / horiz, 0.0D, vel.z / horiz);
                    player.setDeltaMovement(vel.add(dir.x * BOOST, 0.0D, dir.z * BOOST));
                }
            }
        } else if (!player.onGround() && !player.isInWater() && player.getDeltaMovement().y < GLIDE_TRIGGER) {
            player.startFallFlying();
            player.getPersistentData().putBoolean(GLIDING_FLAG, true);
        }
    }

    private static boolean isElytraJetpack(ItemStack chest) {
        if (chest.isEmpty() || !UpgradeData.hasElytra(chest)) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(chest.getItem());
        if (id == null) return false;
        String s = id.toString();
        return s.equals("create_jetpack:jetpack") || s.equals("create_jetpack:netherite_jetpack");
    }
}
