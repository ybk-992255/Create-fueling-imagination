package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.menu.PoweredArtisanTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FuelingImagination.MOD_ID);
    public static final RegistryObject<MenuType<PoweredArtisanTableMenu>> POWERED_ARTISAN_TABLE =
            MENUS.register("powered_artisan_table", () -> IForgeMenuType.create(PoweredArtisanTableMenu::new));

    private ModMenus() { }
}
