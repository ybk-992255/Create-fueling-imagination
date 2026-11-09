package com.fuelingimagination.item;

import com.fuelingimagination.client.ClientBridge;
import com.fuelingimagination.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The repair manual: a read-only index of every block the workstation can repair.
 *
 * <p>Shift-right-click on a block plants the note it carries. The manual is the only item in
 * this chain: placing it puts the book in the world, and breaking the book hands the manual
 * back, so the pair behaves like a single item that can sit in a block or in your hand.
 */
public final class RepairManualItem extends Item {
    public RepairManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Must test the logical side, not the physical one: in single player the integrated
        // server runs in the same JVM, so FMLEnvironment.dist is CLIENT on both sides and the
        // server thread would try to open a screen. That crashed the game.
        if (level.isClientSide) ClientBridge.openManual();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // A plain right click keeps opening the manual; only shift plants the book.
        if (!context.isSecondaryUseActive()) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext place = new BlockPlaceContext(context);
        if (!place.canPlace()) {
            return InteractionResult.FAIL;
        }
        BlockState state = ModBlocks.NOTE.get().getStateForPlacement(place);
        if (state == null) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = place.getClickedPos();
        if (!level.setBlock(pos, state, 11)) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);

        // The book leaves the hand; breaking it gives the manual back.
        if (!level.isClientSide && !player.isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
