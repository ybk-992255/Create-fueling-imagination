package com.fuelingimagination.client;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.entity.Scavenger;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Vanilla villager geometry with the project's own 64x64 skin. */
public final class ScavengerRenderer extends MobRenderer<Scavenger, VillagerModel<Scavenger>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FuelingImagination.MOD_ID, "textures/entity/scavenger.png");

    public ScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Scavenger entity) {
        return TEXTURE;
    }
}