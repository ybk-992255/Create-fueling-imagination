package com.fuelingimagination.mixin;

import com.fuelingimagination.api.HandCrankCranker;
import com.simibubi.create.content.kinetics.crank.HandCrankBlock;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Records the player who turned the crank; {@code m_6227_} is Block#use. */
@Mixin(HandCrankBlock.class)
public class HandCrankBlockMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"), remap = false)
    private void fuelingimagination$recordCranker(BlockState state, Level level, BlockPos pos, Player player,
                                                  InteractionHand hand, BlockHitResult hit,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (level.getBlockEntity(pos) instanceof HandCrankBlockEntity crank) {
            ((HandCrankCranker) crank).cgf$setCranker(player);
        }
    }
}
