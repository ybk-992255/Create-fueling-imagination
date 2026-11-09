package com.fuelingimagination.client;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.registry.ModEntities;
import com.fuelingimagination.registry.ModMenus;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Map;

@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void registerClientReloadListeners(
            net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(com.fuelingimagination.content.upgrade.UpgradeConfig.INSTANCE);
        event.registerReloadListener(com.fuelingimagination.client.armor.ArmorModeModel.INSTANCE);
    }

    @SubscribeEvent
    public static void addArmorModeLayer(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            net.minecraft.client.renderer.entity.player.PlayerRenderer renderer =
                    (net.minecraft.client.renderer.entity.player.PlayerRenderer) event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new com.fuelingimagination.client.armor.ArmorModeLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SCAVENGER.get(), ScavengerRenderer::new);
        event.registerBlockEntityRenderer(com.fuelingimagination.registry.ModBlockEntities.PROJECTOR.get(), com.fuelingimagination.client.ProjectorRenderer::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.POWERED_ARTISAN_TABLE.get(), PoweredArtisanTableScreen::new);
            registerCratePackageModel();
            net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(com.fuelingimagination.registry.ModBlocks.PROJECTOR.get(), net.minecraft.client.renderer.RenderType.cutout());
        });
    }

    /**
     * Create's package renderer resolves its model with
     * {@code PACKAGES.get(ForgeRegistries.ITEMS.getKey(box.getItem()))}.
     * Our crate is a PackageItem registered under our own id, so that lookup returned null and
     * the placed crate rendered as nothing. Point our id at a stock package model.
     */
    private static void registerCratePackageModel() {
        ResourceLocation crate = new ResourceLocation(FuelingImagination.MOD_ID, "scrap_crate");
        // Ground packages and packages hung on a chain are two different model tables.
        mapPackageModel(AllPartialModels.PACKAGES, crate);
        mapPackageModel(AllPartialModels.PACKAGE_RIGGING, crate);
    }

    private static void mapPackageModel(Map<ResourceLocation, PartialModel> table, ResourceLocation crate) {
        PartialModel model = table.get(new ResourceLocation("create", "package"));
        if (model == null) model = table.values().stream().findFirst().orElse(null);
        if (model != null) table.put(crate, model);
    }

    private ClientSetup() { }
}
