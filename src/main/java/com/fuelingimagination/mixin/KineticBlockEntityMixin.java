package com.fuelingimagination.mixin;

import com.fuelingimagination.api.HandCrankCranker;
import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.registry.ModItems;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Power plugin: a boosted hand crank also provides ten times its stress capacity.
 * {@code calculateAddedStressCapacity} is declared on {@link KineticBlockEntity}, not on the
 * generating subclass, so the injection has to live here.
 */
@Mixin(KineticBlockEntity.class)
public class KineticBlockEntityMixin {
    @Inject(method = "calculateAddedStressCapacity", at = @At("RETURN"), cancellable = true, remap = false)
    private void fuelingimagination$boostCrankCapacity(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof HandCrankBlockEntity crank)) return;
        Player player = ((HandCrankCranker) crank).cgf$getCranker();
        if (player == null) return;
        if (CommandPlugins.anyInInventory(player, ModItems.COMMAND_PLUGIN_POWER.get()).isEmpty()) return;
        float value = cir.getReturnValueF();
        if (value == 0.0F) return;
        cir.setReturnValue(value * 10.0F);
    }
}
