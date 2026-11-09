package com.fuelingimagination;

import com.fuelingimagination.registry.ModBlockEntities;
import com.fuelingimagination.registry.ModBlocks;
import com.fuelingimagination.registry.ModCreativeTabs;
import com.fuelingimagination.registry.ModEntities;
import com.fuelingimagination.registry.ModItems;
import com.fuelingimagination.registry.ModMenus;
import com.fuelingimagination.registry.ModRecipes;
import com.fuelingimagination.config.ArtisanConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FuelingImagination.MOD_ID)
public final class FuelingImagination {
    public static final String MOD_ID = "fuelingimagination";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public FuelingImagination() {
        LOGGER.info("[cgf] loaded version {}", net.minecraftforge.fml.ModList.get().getModContainerById(MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("?"));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArtisanConfig.SPEC,
                "fuelingimagination-server.toml");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        com.fuelingimagination.content.armor.ArmorModeNetwork.register();
        com.fuelingimagination.content.projector.ProjectorNetwork.register();
        modBus.addListener(this::registerEntityAttributes);
    }

    private void registerEntityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ModEntities.SCAVENGER.get(), net.minecraft.world.entity.npc.Villager.createAttributes().build());
    }
}
