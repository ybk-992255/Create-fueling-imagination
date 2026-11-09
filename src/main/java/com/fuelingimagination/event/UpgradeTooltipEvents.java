package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.upgrade.UpgradeData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Explains what each upgrade / accessory component does while it is hovered. */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class UpgradeTooltipEvents {
    private UpgradeTooltipEvents() { }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;
        // Creature packages: show the stored mob's name.
        if (stack.hasTag() && stack.getTag().contains("fuelingimagination:entity_name")) {
            event.getToolTip().add(Component.literal(stack.getTag().getString("fuelingimagination:entity_name"))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return;
        String sid = id.toString();

        // Command plugins describe their own effect.
        String pluginDesc = switch (sid) {
            case "fuelingimagination:instruction_interface" -> "tooltip.fuelingimagination.plugin.interface";
            case "fuelingimagination:command_plugin_touch" -> "tooltip.fuelingimagination.plugin.touch";
            case "fuelingimagination:command_plugin_weather" -> "tooltip.fuelingimagination.plugin.weather";
            case "fuelingimagination:command_plugin_anchor" -> "tooltip.fuelingimagination.plugin.anchor";
            case "fuelingimagination:command_plugin_effect" -> "tooltip.fuelingimagination.plugin.effect";
            case "fuelingimagination:command_plugin_power" -> "tooltip.fuelingimagination.plugin.power";
            case "fuelingimagination:command_plugin_title" -> "tooltip.fuelingimagination.plugin.title";
            case "fuelingimagination:command_plugin_sound" -> "tooltip.fuelingimagination.plugin.sound";
            case "fuelingimagination:command_plugin_redstone" -> "tooltip.fuelingimagination.plugin.redstone";
            default -> null;
        };
        if (pluginDesc != null) {
            event.getToolTip().add(Component.translatable(pluginDesc).withStyle(ChatFormatting.GRAY));
            return;
        }

        // Backtanks and jetpacks list what is installed.
        if (sid.equals("create:copper_backtank") || sid.equals("create:netherite_backtank")
                || sid.equals("create_jetpack:jetpack") || sid.equals("create_jetpack:netherite_jetpack")) {
            appendInstalled(event, stack);
            return;
        }

        // Portable command block: show the plugin in its socket.
        if (sid.equals("fuelingimagination:command_block_q")) {
            ItemStack plugin = UpgradeData.readSocket(stack);
            if (plugin.isEmpty()) {
                event.getToolTip().add(Component.literal("插口: 空").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                event.getToolTip().add(Component.literal("插口: ").append(plugin.getHoverName())
                        .withStyle(ChatFormatting.AQUA));
            }
            return;
        }

        // Accessory components for the powered multi-tool: always visible.
        String accessory = switch (sid) {
            case "create:mechanical_saw" -> "tooltip.fuelingimagination.assembly.saw";
            case "create:mechanical_drill" -> "tooltip.fuelingimagination.assembly.drill";
            case "create:blaze_burner" -> "tooltip.fuelingimagination.assembly.blaze";
            case "create:encased_fan" -> "tooltip.fuelingimagination.assembly.fan";
            case "create:rope_pulley" -> "tooltip.fuelingimagination.assembly.rope";
            case "create:package_frogport" -> "tooltip.fuelingimagination.assembly.frogport";
            case "create:mechanical_plough" -> "tooltip.fuelingimagination.assembly.plough";
            default -> null;
        };
        if (accessory != null) {
            if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof com.fuelingimagination.client.PoweredArtisanTableScreen)) return;
            event.getToolTip().add(Component.translatable("tooltip.fuelingimagination.assembly.header")
                    .withStyle(ChatFormatting.GOLD));
            event.getToolTip().add(Component.translatable(accessory).withStyle(ChatFormatting.GRAY));
            return;
        }

        // Upgrade components for backtanks; the effect only matters while the station is open.
        String key = switch (sid) {
            case "create:precision_mechanism" -> "tooltip.fuelingimagination.upgrade.precision";
            case "fuelingimagination:universal_component" -> "tooltip.fuelingimagination.upgrade.universal";
            case "fuelingimagination:compressed_gear_train" -> "tooltip.fuelingimagination.upgrade.compressed";
            default -> null;
        };
        if (key == null) return;
        if (!(net.minecraft.client.Minecraft.getInstance().screen instanceof com.fuelingimagination.client.PoweredArtisanTableScreen)) return;
        event.getToolTip().add(Component.translatable("tooltip.fuelingimagination.upgrade.header")
                .withStyle(ChatFormatting.GOLD));
        event.getToolTip().add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private static void appendInstalled(ItemTooltipEvent event, ItemStack stack) {
        List<String> components = UpgradeData.readComponents(stack);
        boolean armor = UpgradeData.hasArmor(stack);
        boolean elytra = UpgradeData.hasElytra(stack);
        if (components.isEmpty() && !armor && !elytra) return;
        event.getToolTip().add(Component.literal("已安装").withStyle(ChatFormatting.GOLD));
        for (String c : components) {
            ItemStack s = UpgradeData.stackOf(c);
            if (!s.isEmpty()) {
                event.getToolTip().add(Component.literal("  ").append(s.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        }
        if (armor) event.getToolTip().add(Component.literal("  护甲").withStyle(ChatFormatting.GRAY));
        if (elytra) event.getToolTip().add(Component.literal("  鞘翅").withStyle(ChatFormatting.GRAY));
    }
}
