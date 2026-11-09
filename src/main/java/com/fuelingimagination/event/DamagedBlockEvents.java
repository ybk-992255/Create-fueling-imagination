package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.damaged.DamagedBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that turns a block into a damaged block item, and everything a damaged
 * block item refuses to do.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class DamagedBlockEvents {
    private DamagedBlockEvents() { }

    /**
     * Refuses the placement before anything happens.
     *
     * <p>This is the event that matters for feedback: it fires before {@code ItemStack.useOn},
     * on the client as well as the server. Denying the item use on the client means the
     * interaction packet is never sent, so the block never appears at all.
     *
     * <p>Only the item use is denied, not the block interaction, so right-clicking a chest
     * while holding a damaged block still opens the chest.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!DamagedBlocks.isDamaged(event.getItemStack())) return;
        event.setUseItem(Event.Result.DENY);
        if (event.getLevel().isClientSide) {
            event.getEntity().displayClientMessage(
                    Component.translatable("message.fuelingimagination.damaged_cannot_place")
                            .withStyle(ChatFormatting.RED),
                    true);
        }
    }

    /**
     * Server-side safety net for placement paths that do not go through a player's
     * right-click (dispensers, other mods calling {@code useOn} directly).
     *
     * <p>Note that in 1.20.1 this event fires <em>after</em> the block has been written;
     * cancelling makes Forge restore the snapshot. That is why it is not the primary
     * handler: cancelling here produces the short-lived "placed then vanished" flicker.
     */
    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (!DamagedBlocks.isDamaged(placingStack(living, event.getPlacedBlock()))) return;
        event.setCanceled(true);
    }

    /** Shared by both hands: the stack that matches the block about to be placed. */
    private static ItemStack placingStack(LivingEntity living, BlockState placed) {
        ItemStack main = living.getMainHandItem();
        if (isStackFor(main, placed)) return main;
        ItemStack off = living.getOffhandItem();
        if (isStackFor(off, placed)) return off;
        return main;
    }

    private static boolean isStackFor(ItemStack stack, BlockState placed) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == placed.getBlock();
    }

    /**
     * Explosions always yield the damaged item for affected blocks in the definition file,
     * regardless of the vanilla per-block drop chance.
     *
     * <p>Those positions are removed from the blast list so vanilla neither destroys nor
     * drops them; this handler destroys them itself and spawns exactly one damaged stack.
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        List<BlockPos> affected = event.getAffectedBlocks();
        if (affected.isEmpty()) return;

        List<BlockPos> captured = new ArrayList<>();
        for (BlockPos pos : affected) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!DamagedBlocks.canBeDamaged(new ItemStack(state.getBlock()))) continue;
            captured.add(pos);
        }

        for (BlockPos pos : captured) {
            affected.remove(pos);
        }
        for (BlockPos pos : captured) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            // Contents would otherwise vanish together with the block.
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                Containers.dropContents(level, pos, container);
            }
            level.destroyBlock(pos, false, null, 512);
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    DamagedBlocks.damaged(state.getBlock()));
        }
    }

    /** The red/white/blue repair hint, shown only on damaged stacks. */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!DamagedBlocks.isDamaged(event.getItemStack())) return;
        event.getToolTip().add(Component.translatable("tooltip.fuelingimagination.damaged",
                Component.translatable("tooltip.fuelingimagination.damaged.state").withStyle(ChatFormatting.RED),
                Component.translatable("tooltip.fuelingimagination.damaged.station").withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.WHITE));
    }
}
