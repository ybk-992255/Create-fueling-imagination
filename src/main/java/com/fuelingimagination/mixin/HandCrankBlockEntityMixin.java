package com.fuelingimagination.mixin;

import com.fuelingimagination.api.HandCrankCranker;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Power plugin: a player carrying the command block cranks at 64 RPM instead of 32. */
@Mixin(HandCrankBlockEntity.class)
public abstract class HandCrankBlockEntityMixin implements HandCrankCranker {
    @Unique
    private Player fuelingimagination$cranker;

    @Override
    public void cgf$setCranker(Player player) {
        this.fuelingimagination$cranker = player;
    }

    @Override
    public Player cgf$getCranker() {
        return this.fuelingimagination$cranker;
    }

    @Inject(method = "getGeneratedSpeed", at = @At("RETURN"), cancellable = true, remap = false)
    private void fuelingimagination$boostSpeed(CallbackInfoReturnable<Float> cir) {
        if (this.fuelingimagination$cranker == null) return;
        if (CommandPlugins.anyInInventory(this.fuelingimagination$cranker,
                ModItems.COMMAND_PLUGIN_POWER.get()).isEmpty()) return;
        float value = cir.getReturnValueF();
        if (value == 0.0F) return;
        cir.setReturnValue(value * 2.0F);
    }
}
