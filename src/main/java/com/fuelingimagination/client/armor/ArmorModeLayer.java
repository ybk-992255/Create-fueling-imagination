package com.fuelingimagination.client.armor;

import com.fuelingimagination.content.armor.ArmorMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public final class ArmorModeLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("fuelingimagination", "textures/models/armor/armor_mode.png");

   public ArmorModeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
      super(parent);
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (ArmorMode.isActive(player)) {
         ArmorModeModel.INSTANCE.render((PlayerModel<?>)this.getParentModel(), pose, buffers.getBuffer(RenderType.entityCutoutNoCullZOffset(TEXTURE)), light);
      }
   }
}
