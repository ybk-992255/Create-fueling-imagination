package com.fuelingimagination.mixin;

import com.fuelingimagination.content.upgrade.UpgradeData;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Backtank upgrades that act on a placed tank: capacity doubling and the refill boost.
 *
 * <p>{@code tick} is not renamed by SRG (absent from the mapping file), so it is targeted by name
 * with {@code remap = false} and no refmap is needed.
 */
@Mixin(value = BacktankBlockEntity.class, remap = false)
public abstract class BacktankBlockEntityMixin {
    @Shadow private int airLevel;
    @Shadow private int capacityEnchantLevel;
    @Unique private int fuelingimagination$airBefore;

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void fuelingimagination$prepareTick(CallbackInfo ci) {
        BacktankBlockEntity self = (BacktankBlockEntity) (Object) this;
        if (UpgradeData.tagHasComponent(self.getVanillaTag(), "fuelingimagination:compressed_gear_train")) {
            // Rebuilt from the item's own enchantment every tick, so placement, pickup and reload
            // all end up with the same value instead of stacking.
            ItemStack vanilla = ItemStack.of(self.getVanillaTag());
            int base = vanilla.isEmpty() ? 0
                    : EnchantmentHelper.getTagEnchantmentLevel(AllEnchantments.CAPACITY.get(), vanilla);
            // Doubling means adding enough extra levels to cover the base maximum again.
            int perLevel = BacktankUtil.maxAir(1) - BacktankUtil.maxAir(0);
            int baseMax = BacktankUtil.maxAir(base);
            int extra = perLevel > 0 ? (baseMax + perLevel - 1) / perLevel : 0;
            capacityEnchantLevel = base + extra;
        }
        fuelingimagination$airBefore = airLevel;
    }

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void fuelingimagination$finishTick(CallbackInfo ci) {
        BacktankBlockEntity self = (BacktankBlockEntity) (Object) this;
        int gained = airLevel - fuelingimagination$airBefore;
        if (gained <= 0) return;
        if (!UpgradeData.tagHasComponent(self.getVanillaTag(), "create:precision_mechanism")) return;
        int ceiling = BacktankUtil.maxAir(capacityEnchantLevel);
        airLevel = Math.min(ceiling, airLevel + Math.max(1, gained * 3 / 10));
    }
}
