package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FuelingImagination.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GOURMET_FOOD = TABS.register("gourmet_food",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fuelingimagination"))
                    .icon(() -> new ItemStack(ModItems.PRECISION_MECHANISM_PIE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.GEAR_COOKIE.get());
                        output.accept(ModItems.COMPONENT_BASE.get());
                        output.accept(ModItems.COMPONENT_BLANK.get());
                        output.accept(ModItems.COMPRESSED_GEAR_TRAIN.get());
                        output.accept(ModItems.COMPONENT_GEAR_TRAIN.get());
                        output.accept(ModItems.UNIVERSAL_COMPONENT.get());
                        output.accept(ModItems.DAMAGED_COMPONENT.get());
                        output.accept(ModItems.POWERED_SWORD.get());
                        output.accept(ModItems.ARMOR_CONTROLLER.get());
                        // Contents are rolled per stack on its first inventory tick, so each
                        // crate the player takes is its own blind box.
                        output.accept(new ItemStack(ModItems.SCRAP_CRATE.get()));
                        output.accept(ModItems.REPAIR_MANUAL.get());
                        output.accept(ModItems.SCAVENGER_SPAWN_EGG.get());
                        output.accept(ModItems.PRECISION_MECHANISM_PIE.get());
                        output.accept(ModItems.POWERED_ARTISAN_TABLE.get());
                        output.accept(ModItems.PROJECTOR.get());
                        output.accept(ModItems.FILM_REEL.get());
                        output.accept(ModItems.POWERED_DISMANTLER.get());
                        output.accept(ModItems.TRASH_BLOCK.get());
                        output.accept(ModItems.COMMAND_BLOCK_Q.get());
                        output.accept(ModItems.COMMAND_BLOCK_FRAME.get());
                        ItemStack damagedCmd = new ItemStack(ModItems.DAMAGED_COMMAND_BLOCK.get());
                        com.fuelingimagination.content.damaged.DamagedBlocks.markDamaged(damagedCmd);
                        output.accept(damagedCmd);
                        output.accept(new ItemStack(ModItems.DAMAGED_COMMAND_BLOCK_FRAME.get()));
                        output.accept(new ItemStack(ModItems.COMMAND_CORE.get()));
                        output.accept(ModItems.INSTRUCTION_INTERFACE.get());
                        output.accept(ModItems.COMMAND_PLUGIN_TOUCH.get());
                        output.accept(ModItems.COMMAND_PLUGIN_WEATHER.get());
                        output.accept(ModItems.COMMAND_PLUGIN_ANCHOR.get());
                        output.accept(ModItems.COMMAND_PLUGIN_EFFECT.get());
                        output.accept(ModItems.COMMAND_PLUGIN_POWER.get());
                        output.accept(ModItems.COMMAND_PLUGIN_TITLE.get());
                        output.accept(ModItems.COMMAND_PLUGIN_SOUND.get());
                        output.accept(ModItems.COMMAND_PLUGIN_REDSTONE.get());
                    })
                    .build());

    private ModCreativeTabs() { }
}
