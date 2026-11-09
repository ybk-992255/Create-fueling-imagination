package com.fuelingimagination.registry;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.item.DamagedComponentItem;
import com.fuelingimagination.item.EdibleBlockItem;
import com.fuelingimagination.item.PrecisionMechanismPieItem;
import com.fuelingimagination.item.PoweredArtisanTableItem;
import com.fuelingimagination.item.PoweredDismantlerItem;
import com.fuelingimagination.item.RepairManualItem;
import com.fuelingimagination.item.ScrapCrateItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FuelingImagination.MOD_ID);

    private static final FoodProperties GEAR_COOKIE_FOOD = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.5F).alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 200, 1), 1.0F)
            .build();

    private static final FoodProperties PIE_FOOD = new FoodProperties.Builder()
            .nutrition(0).saturationMod(0.0F).alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.SATURATION, 60, 2), 1.0F)
            .build();

    public static final RegistryObject<Item> GEAR_COOKIE = ITEMS.register("gear_cookie",
            () -> new EdibleBlockItem(ModBlocks.GEAR_COOKIE.get(),
                    new Item.Properties().stacksTo(64).food(GEAR_COOKIE_FOOD), 10));

    public static final RegistryObject<Item> PRECISION_MECHANISM_PIE = ITEMS.register("precision_mechanism_pie",
            () -> new PrecisionMechanismPieItem(ModBlocks.PRECISION_MECHANISM_PIE.get(),
                    new Item.Properties().stacksTo(1).food(PIE_FOOD)));

    public static final RegistryObject<Item> PRECISION_MECHANISM_PIE_BASE =
            ITEMS.register("precision_mechanism_pie_base", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_GEAR_COOKIE = ITEMS.register("incomplete_gear_cookie",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_PRECISION_MECHANISM_PIE =
            ITEMS.register("incomplete_precision_mechanism_pie", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> INCOMPLETE_PRECISION_MECHANISM_PIE_BASE =
            ITEMS.register("incomplete_precision_mechanism_pie_base", () -> new Item(new Item.Properties()));

        /** A sword whose plastic look is still a placeholder (vanilla wooden sword). */
    public static final RegistryObject<Item> POWERED_SWORD = ITEMS.register("powered_sword",
            com.fuelingimagination.item.PoweredSwordItem::new);

public static final RegistryObject<Item> COMPONENT_BASE = material("component_base");
    public static final RegistryObject<Item> COMPONENT_BLANK = material("component_blank");
    public static final RegistryObject<Item> COMPRESSED_GEAR_TRAIN = material("compressed_gear_train");
    public static final RegistryObject<Item> COMPONENT_GEAR_TRAIN = material("component_gear_train");
    public static final RegistryObject<Item> UNIVERSAL_COMPONENT = material("universal_component");
    public static final RegistryObject<Item> DAMAGED_COMPONENT = ITEMS.register("damaged_component",
            () -> new DamagedComponentItem(new Item.Properties()));
    public static final RegistryObject<Item> SCRAP_CRATE = ITEMS.register("scrap_crate",
            () -> new ScrapCrateItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> REPAIR_MANUAL = ITEMS.register("repair_manual",
            () -> new RepairManualItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SCAVENGER_SPAWN_EGG = ITEMS.register("scavenger_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(ModEntities.SCAVENGER,
                    0x2E8B2E, 0xFFEB3B, new Item.Properties()));
    public static final RegistryObject<Item> INCOMPLETE_COMPONENT_BLANK = material("incomplete_component_blank");
    public static final RegistryObject<Item> INCOMPLETE_COMPRESSED_GEAR_TRAIN = material("incomplete_compressed_gear_train");
    public static final RegistryObject<Item> INCOMPLETE_UNIVERSAL_COMPONENT = material("incomplete_universal_component");

    public static final RegistryObject<Item> POWERED_ARTISAN_TABLE = ITEMS.register("powered_artisan_table",
            () -> new PoweredArtisanTableItem(ModBlocks.POWERED_ARTISAN_TABLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> POWERED_DISMANTLER = ITEMS.register("powered_dismantler",
            () -> new PoweredDismantlerItem(ModBlocks.POWERED_DISMANTLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> TRASH_BLOCK = ITEMS.register("trash_block",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.TRASH_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> COMMAND_BLOCK_Q = ITEMS.register("command_block_q",
            () -> new com.fuelingimagination.item.CommandBlockItem(new Item.Properties()));
    public static final RegistryObject<Item> COMMAND_BLOCK_FRAME = material("command_block_frame");
    public static final RegistryObject<Item> DAMAGED_COMMAND_BLOCK = material("damaged_command_block");
    public static final RegistryObject<Item> DAMAGED_COMMAND_BLOCK_FRAME = material("damaged_command_block_frame");
    public static final RegistryObject<Item> COMMAND_CORE = material("command_core");

    public static final RegistryObject<Item> INSTRUCTION_INTERFACE = material("instruction_interface");
    public static final RegistryObject<Item> COMMAND_PLUGIN_TOUCH = material("command_plugin_touch");
    public static final RegistryObject<Item> COMMAND_PLUGIN_WEATHER = material("command_plugin_weather");
    public static final RegistryObject<Item> COMMAND_PLUGIN_ANCHOR = material("command_plugin_anchor");
    public static final RegistryObject<Item> COMMAND_PLUGIN_EFFECT = material("command_plugin_effect");
    public static final RegistryObject<Item> COMMAND_PLUGIN_POWER = material("command_plugin_power");
    public static final RegistryObject<Item> COMMAND_PLUGIN_TITLE = material("command_plugin_title");
    public static final RegistryObject<Item> COMMAND_PLUGIN_SOUND = material("command_plugin_sound");
    public static final RegistryObject<Item> COMMAND_PLUGIN_REDSTONE = material("command_plugin_redstone");
    public static final RegistryObject<Item> ARMOR_CONTROLLER = ITEMS.register("armor_controller",
            () -> new com.fuelingimagination.item.ArmorControllerItem());

    public static final RegistryObject<Item> PROJECTOR = ITEMS.register("projector",
            () -> new com.fuelingimagination.item.ProjectorBlockItem(ModBlocks.PROJECTOR.get(),
                    new Item.Properties().stacksTo(1),
                    "tooltip.fuelingimagination.projector",
                    "tooltip.fuelingimagination.projector.hint"));

    public static final RegistryObject<Item> FILM_REEL = ITEMS.register("film_reel",
            () -> new com.fuelingimagination.item.FilmReelItem(new Item.Properties()));

    private static RegistryObject<Item> material(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private ModItems() { }
}
