package com.fuelingimagination.mixin;

import com.fuelingimagination.content.upgrade.UpgradeData;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Doubles the capacity when the compressed component is installed.
 *
 * <p>Create reads capacity from a static helper, so there is no event to hook. The target is
 * Create's own method (never obfuscated), hence {@code remap = false} and no refmap.
 */
@Mixin(value = BacktankUtil.class, remap = false)
public abstract class BacktankUtilMixin {
    @Inject(
            method = "maxAir(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void fuelingimagination$compressedCapacity(ItemStack backtank, CallbackInfoReturnable<Integer> cir) {
        if (UpgradeData.hasComponent(backtank, "fuelingimagination:compressed_gear_train")) {
            cir.setReturnValue(cir.getReturnValueI() * 2);
        }
    }
}
