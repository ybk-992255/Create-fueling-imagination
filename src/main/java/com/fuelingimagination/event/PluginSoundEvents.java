package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.mixin.LivingEntityInvoker;
import com.fuelingimagination.registry.ModItems;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "声音" plugin.
 *
 * <p>Right-click a mob to remember it; while the block is carried, being hurt replays that mob's
 * hurt sound. Right-click air clears the memory.
 *
 * <p>To play a disc, hold the command block in the main hand and any music disc in the off hand and
 * right-click: the disc starts playing, right-clicking again stops it.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class PluginSoundEvents {
    public static final String KEY_ENTITY = "fuelingimagination:sound_entity";
    public static final String KEY_PLAYING = "fuelingimagination:disc_playing";

    private PluginSoundEvents() { }

    private static boolean holdsSound(ItemStack stack) {
        return CommandPlugins.isCommandBlock(stack)
                && CommandPlugins.has(stack, ModItems.COMMAND_PLUGIN_SOUND.get());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack main = player.getMainHandItem();
        if (!holdsSound(main)) return;
        CommandPlugins.setPluginString(main, KEY_ENTITY,
                EntityType.getKey(event.getTarget().getType()).toString());
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        for (ItemStack stack : player.getInventory().items) {
            if (!holdsSound(stack)) continue;
            String id = CommandPlugins.getPluginString(stack, KEY_ENTITY);
            if (id.isEmpty()) return;
            EntityType<?> type = EntityType.byString(id).orElse(null);
            if (type == null) return;
            Entity created = type.create(player.level());
            if (!(created instanceof LivingEntity living)) return;
            SoundEvent sound = ((LivingEntityInvoker) living).cgf$getHurtSound(player.damageSources().generic());
            if (sound != null) {
                player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return;
        }
    }

    /** Main hand block + off hand disc: toggle playback; otherwise clear the saved mob. */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        ItemStack main = player.getMainHandItem();
        if (!holdsSound(main)) return;
        boolean playing = player.getPersistentData().getBoolean(KEY_PLAYING);
        if (player.getOffhandItem().getItem() instanceof RecordItem record) {
            if (playing) {
                player.getPersistentData().remove(KEY_PLAYING);
                stopDisc(player);
            } else {
                player.getPersistentData().putBoolean(KEY_PLAYING, true);
                player.level().playSound(null, player.blockPosition(), record.getSound(),
                        SoundSource.RECORDS, 1.0F, 1.0F);
            }
            event.setCanceled(true);
            return;
        }
        if (playing) return;
        CommandPlugins.setPluginString(main, KEY_ENTITY, "");
    }

    private static void stopDisc(Player player) {
        if (player.getServer() == null) return;
        for (ServerPlayer target : player.getServer().getPlayerList().getPlayers()) {
            if (target.level() != player.level()) continue;
            if (target.distanceToSqr(player) > 64.0D * 64.0D) continue;
            target.connection.send(new ClientboundStopSoundPacket(null, SoundSource.RECORDS));
        }
    }
}
