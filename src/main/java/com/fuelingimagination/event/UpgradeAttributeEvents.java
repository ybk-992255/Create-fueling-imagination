package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.content.upgrade.UpgradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Turns the armour values stored by an applied armour slot into real attribute modifiers.
 *
 * <p>The values live on the stack's NBT rather than in a subclass, because the mod cannot
 * replace Create's own backtank class.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID)
public final class UpgradeAttributeEvents {
    private static final UUID ARMOR_ID = UUID.nameUUIDFromBytes("cgf:upgrade_armor".getBytes(StandardCharsets.UTF_8));
    private static final UUID TOUGHNESS_ID = UUID.nameUUIDFromBytes("cgf:upgrade_toughness".getBytes(StandardCharsets.UTF_8));
    private static final UUID KNOCKBACK_ID = UUID.nameUUIDFromBytes("cgf:upgrade_knockback".getBytes(StandardCharsets.UTF_8));

    private UpgradeAttributeEvents() { }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.CHEST) return;
        ItemStack stack = event.getItemStack();
        if (!stack.hasTag()) return;
        CompoundTag root = stack.getTag().getCompound(UpgradeData.NBT_ROOT);
        if (root.isEmpty()) return;

        merge(event, Attributes.ARMOR, ARMOR_ID, "fuelingimagination:upgrade_armor", root.getDouble(UpgradeData.NBT_ARMOR));
        merge(event, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, "fuelingimagination:upgrade_toughness", root.getDouble(UpgradeData.NBT_TOUGHNESS));
        merge(event, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "fuelingimagination:upgrade_knockback", root.getDouble(UpgradeData.NBT_KNOCKBACK));
    }

    /**
     * Keeps the better of the item's own value and the one inherited from the chestplate.
     *
     * <p>Adding a second modifier would print a duplicate tooltip line (for example a netherite
     * backtank receiving a netherite chestplate), so the existing entry is replaced instead.
     */
    private static void merge(ItemAttributeModifierEvent event,
                              net.minecraft.world.entity.ai.attributes.Attribute attribute,
                              UUID id, String name, double inherited) {
        if (inherited <= 0.0D) return;
        double existing = 0.0D;
        for (AttributeModifier modifier : event.getModifiers().get(attribute)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) existing += modifier.getAmount();
        }
        if (inherited <= existing) return;
        event.removeAttribute(attribute);
        event.addModifier(attribute,
                new AttributeModifier(id, name, inherited, AttributeModifier.Operation.ADDITION));
    }
}
