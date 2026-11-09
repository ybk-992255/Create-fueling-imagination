package com.fuelingimagination.mixin;

import com.fuelingimagination.content.armor.ArmorMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides whatever a player is holding while the armour mode suit is worn.
 *
 * <p>The first person hand is already cancelled by {@code RenderHandEvent}; this covers the third
 * person model, where the item is drawn by a render layer rather than the hand renderer.
 */
@Mixin(value = net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer.class, remap = false)
public abstract class PlayerItemInHandLayerMixin {
    @Inject(method = "m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/Entity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void fuelingimagination$hideHeldItem(PoseStack pose, MultiBufferSource buffers, int light,
                                                 Entity entity, float limbSwing, float limbSwingAmount,
                                                 float partialTick, float ageInTicks, float netHeadYaw,
                                                 float headPitch, CallbackInfo ci) {
        if (entity instanceof Player player && ArmorMode.isActive(player)) {
            ci.cancel();
        }
    }
}