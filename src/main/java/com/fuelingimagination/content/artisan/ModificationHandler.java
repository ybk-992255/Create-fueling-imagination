package com.fuelingimagination.content.artisan;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.block.entity.PoweredArtisanTableBlockEntity;
import com.fuelingimagination.content.upgrade.UpgradeConfig;
import com.fuelingimagination.content.upgrade.UpgradeData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modification mode.
 *
 * <p>Opening the ring <em>extracts</em> whatever is already installed back into the ring slots, so
 * the player can take a part out, swap it, or close the ring again unchanged. Confirming rebuilds
 * the record from the ring's current contents; cancelling hands them back to the player.
 *
 * <p>Slot types: {@code armor} (chestplate inheritance), {@code material} (one reshapes the tool),
 * {@code upgrade} and {@code accessory} (a list of component ids).
 */
public final class ModificationHandler implements ArtisanModeHandler {
    /** Right-click accessories whose behaviours would clash if more than one is installed. */
    private static final Set<String> EXCLUSIVE = Set.of(
            "create:mechanical_saw", "create:encased_fan", "create:rope_pulley",
            "create:package_frogport", "create:mechanical_plough");

    @Override
    public boolean handleAction(PoweredArtisanTableBlockEntity table, Player player, ArtisanAction action, int detail) {
        if (table.getLevel() == null || table.getLevel().isClientSide) return false;
        ArtisanProcessStage stage = table.getProcessStage();
        ItemStack main = table.getInventory().getItem(PoweredArtisanTableBlockEntity.MAIN_SLOT);

        if (action == ArtisanAction.CANCEL && stage != ArtisanProcessStage.IDLE) {
            returnSlots(table, player);
            table.resetProcess();
            return true;
        }
        if (!UpgradeConfig.isModifiable(main)) {
            return ArtisanCraftingHandler.fail(player, "modify_not_supported");
        }
        List<UpgradeConfig.Slot> slots = UpgradeConfig.slotsFor(main);
        int maxSlots = Math.min(8, slots.size());

        if (action == ArtisanAction.CRANK) {
            if (stage == ArtisanProcessStage.IDLE) {
                extractInstalled(table, main);
                table.setProcessState(maxSlots <= 1 ? ArtisanProcessStage.MODIFICATION_READY
                        : ArtisanProcessStage.MODIFICATION_OPENING, 1);
                return true;
            }
            int open = table.getOpenSlots();
            if (open >= maxSlots) return ArtisanCraftingHandler.fail(player, "modify_all_open");
            int next = open + 1;
            table.setProcessState(next >= maxSlots ? ArtisanProcessStage.MODIFICATION_READY
                    : ArtisanProcessStage.MODIFICATION_OPENING, next);
            return true;
        }

        if (action == ArtisanAction.CONFIRM) {
            if (stage == ArtisanProcessStage.IDLE || table.getOpenSlots() <= 0) {
                return ArtisanCraftingHandler.fail(player, "modify_nothing_open");
            }
            List<String> installed = new ArrayList<>();
            String material = null;
            for (int i = 0; i < maxSlots && i < table.getOpenSlots(); i++) {
                ItemStack inserted = table.getInventory().getItem(PoweredArtisanTableBlockEntity.COMPONENT_START + i);
                if (inserted.isEmpty()) continue;
                UpgradeConfig.Slot slot = slots.get(i);
                if (!slot.accepts(inserted)) return ArtisanCraftingHandler.fail(player, "modify_wrong_item");
                String type = slot.type();
                if ("armor".equals(type)) {
                    UpgradeData.applyArmor(main, inserted);
                } else if ("elytra".equals(type)) {
                    UpgradeData.applyElytra(main, inserted);
                } else if ("socket".equals(type)) {
                    UpgradeData.applySocket(main, inserted);
                } else if ("material".equals(type)) {
                    material = UpgradeData.idOf(inserted);
                } else {
                    String id = UpgradeData.idOf(inserted);
                    if (installed.contains(id)) {
                        return ArtisanCraftingHandler.fail(player, "modify_duplicate_component");
                    }
                    installed.add(id);
                }
            }

            long exclusiveCount = installed.stream().filter(EXCLUSIVE::contains).count();
            if (exclusiveCount > 1) return ArtisanCraftingHandler.fail(player, "modify_conflicting");

            UpgradeData.setComponents(main, installed);
            UpgradeData.setMaterial(main, material == null ? "" : material);
            for (int i = 0; i < maxSlots && i < table.getOpenSlots(); i++) {
                table.getInventory().setItem(PoweredArtisanTableBlockEntity.COMPONENT_START + i, ItemStack.EMPTY);
            }
            FuelingImagination.LOGGER.info("modify confirmed components={} material={} tag={}", installed, material, main.getTag());
            table.getInventory().setChanged();
            table.getLevel().playSound(null, table.getBlockPos(), net.minecraft.sounds.SoundEvents.ANVIL_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 1.2F);
            table.resetProcess();
            return true;
        }
        return false;
    }

    /** Pulls every installed part back into its slot so it can be handled again. */
    private void extractInstalled(PoweredArtisanTableBlockEntity table, ItemStack main) {
        List<UpgradeConfig.Slot> slots = UpgradeConfig.slotsFor(main);

        ItemStack armour = UpgradeData.extractArmor(main);
        String material = UpgradeData.readMaterial(main);
        UpgradeData.setMaterial(main, "");
        ItemStack elytra = UpgradeData.extractElytra(main);
        ItemStack socket = UpgradeData.extractSocket(main);
        List<String> components = new ArrayList<>(UpgradeData.readComponents(main));
        UpgradeData.setComponents(main, List.of());

        int compIndex = 0;
        for (int i = 0; i < slots.size() && i < 8; i++) {
            String type = slots.get(i).type();
            ItemStack toPlace = ItemStack.EMPTY;
            if ("armor".equals(type)) {
                if (!armour.isEmpty()) { toPlace = armour; armour = ItemStack.EMPTY; }
            } else if ("elytra".equals(type)) {
                if (!elytra.isEmpty()) { toPlace = elytra; elytra = ItemStack.EMPTY; }
            } else if ("socket".equals(type)) {
                if (!socket.isEmpty()) { toPlace = socket; socket = ItemStack.EMPTY; }
            } else if ("material".equals(type)) {
                if (!material.isEmpty()) { toPlace = UpgradeData.stackOf(material); material = ""; }
            } else {
                if (compIndex < components.size()) toPlace = UpgradeData.stackOf(components.get(compIndex++));
            }
            if (!toPlace.isEmpty()) {
                table.getInventory().setItem(PoweredArtisanTableBlockEntity.COMPONENT_START + i, toPlace);
            }
        }
    }

    private void returnSlots(PoweredArtisanTableBlockEntity table, Player player) {
        for (int i = 0; i < 8; i++) {
            ItemStack stack = table.getInventory().getItem(PoweredArtisanTableBlockEntity.COMPONENT_START + i);
            if (stack.isEmpty()) continue;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                    new PlayerMainInvWrapper(player.getInventory()), stack.copy(), false);
            if (!remainder.isEmpty()) player.drop(remainder, false);
            table.getInventory().setItem(PoweredArtisanTableBlockEntity.COMPONENT_START + i, ItemStack.EMPTY);
        }
    }

    @Override
    public List<Component> describeMainItem(ItemStack stack, Level level) {
        List<Component> lines = new ArrayList<>();
        List<UpgradeConfig.Slot> slots = UpgradeConfig.slotsFor(stack);
        if (slots.isEmpty()) return lines;
        Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (UpgradeConfig.Slot slot : slots) {
            counts.merge(slot.type(), 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            lines.add(Component.translatable("gui.fuelingimagination.modify.slot." + e.getKey())
                    .append(Component.literal(" ×" + e.getValue())));
        }
        int total = UpgradeData.readComponents(stack).size()
                + (UpgradeData.readMaterial(stack).isEmpty() ? 0 : 1)
                + (UpgradeData.hasArmor(stack) ? 1 : 0)
                + (UpgradeData.hasSocket(stack) ? 1 : 0);
        if (total > 0) {
            lines.add(Component.translatable("gui.fuelingimagination.modify.installed", total));
        }
        return lines;
    }
}
