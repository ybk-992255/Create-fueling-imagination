package com.fuelingimagination.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes the protected hurt sound so the sound plugin can replay a saved mob's cry. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker(value = "m_7975_", remap = false)
    SoundEvent cgf$getHurtSound(DamageSource source);
}
