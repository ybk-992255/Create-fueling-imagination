package com.fuelingimagination.mixin;

import com.fuelingimagination.client.InventoryArmorHider;
import com.fuelingimagination.content.armor.ArmorMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the local player's armour layer while the toggle is on. Applies everywhere (inventory and
 * world) so the choice survives leaving the inventory. Uses the SRG name because the mod ships
 * without a refmap; the vanilla render method is {@code m_6494_}.
 */
@Mixin(HumanoidArmorLayer.class)
public class HumanoidArmorLayerMixin {
    @Inject(
            method = "m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void fuelingimagination$hideArmor(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                              LivingEntity entity, float limbSwing, float limbSwingAmount,
                                              float partialTicks, float ageInTicks, float netHeadYaw,
                                              float headPitch, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ArmorMode.isActive(entity) || (InventoryArmorHider.isHiding() && minecraft.player != null && entity == minecraft.player)) {
            ci.cancel();
        }
    }
}
