package com.fuelingimagination.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.client.model.BakedModelWrapper;

/**
 * Wraps the powered multi-tool's JSON model so {@code isCustomRenderer()} returns {@code true}.
 * ItemRenderer then routes this item to {@link PoweredSwordRenderer} instead of drawing the static
 * JSON model directly.
 *
 * <p>{@code applyTransform} must return {@code this}: ItemRenderer applies camera transforms before
 * checking {@code isCustomRenderer()}, and the default wrapper returns the original model, which
 * would drop the custom-renderer flag and skip our renderer entirely.
 */
public final class PoweredSwordBakedModel extends BakedModelWrapper<BakedModel> {
    public PoweredSwordBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean applyLeftHandTransform) {
        originalModel.applyTransform(context, poseStack, applyLeftHandTransform);
        return this;
    }
}
