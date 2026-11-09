package com.fuelingimagination.content.upgrade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads and writes the modification record stored on the item itself. */
public final class UpgradeData {
    public static final String NBT_ROOT = "fuelingimagination:upgrades";
    public static final String NBT_COMPONENTS = "components";
    /** The registry id of the item installed in a "material" slot (e.g. minecraft:iron_ingot). */
    public static final String NBT_MATERIAL = "material";
    public static final String NBT_ARMOR = "armor";
    public static final String NBT_TOUGHNESS = "toughness";
    public static final String NBT_KNOCKBACK = "knockback";
    /** The chestplate itself, so the armour slot can be emptied again. */
    public static final String NBT_ARMOR_STACK = "armor_stack";
    /** Enchantments copied onto the item, so they can be taken back off. */
    public static final String NBT_ARMOR_ENCH = "armor_enchants";
    /** The elytra installed in the jetpack's elytra slot (durability is not consumed). */
    public static final String NBT_ELYTRA_STACK = "elytra_stack";
    /** The plugin installed in the command block's socket slot. */
    public static final String NBT_SOCKET_STACK = "socket_stack";
    /** The music disc installed in the command block's disc slot. */
    public static final String NBT_DISC_STACK = "disc_stack";

    /** Extra air the compressed component adds. */
    public static final int COMPRESSED_CAPACITY_BONUS = 800;

    private UpgradeData() { }

    // ---------------------------------------------------------------- raw access

    /** The sub-tag must be re-attached: getOrCreateTagElement hands back a detached copy. */
    private static CompoundTag root(ItemStack stack, boolean create) {
        if (!stack.hasTag()) {
            if (!create) return new CompoundTag();
            stack.getOrCreateTag();
        }
        CompoundTag tag = stack.getTag();
        if (tag.contains(NBT_ROOT, Tag.TAG_COMPOUND)) return tag.getCompound(NBT_ROOT);
        return new CompoundTag();
    }

    private static void commit(ItemStack stack, CompoundTag root) {
        stack.getOrCreateTag().put(NBT_ROOT, root);
    }

    /** Component registry ids currently installed, in slot order. */
    public static List<String> readComponents(ItemStack stack) {
        CompoundTag root = root(stack, false);
        ListTag list = root.getList(NBT_COMPONENTS, Tag.TAG_STRING);
        List<String> ids = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) ids.add(list.getString(i));
        return ids;
    }

    /** Replaces the whole component list; an empty list clears it. */
    public static void setComponents(ItemStack stack, List<String> ids) {
        CompoundTag root = root(stack, true);
        ListTag list = new ListTag();
        for (String id : ids) list.add(StringTag.valueOf(id));
        root.put(NBT_COMPONENTS, list);
        commit(stack, root);
    }

    /** Same check against a raw item tag, used where only the transferred NBT is available. */
    public static boolean tagHasComponent(CompoundTag itemTag, String id) {
        if (itemTag == null) return false;
        ListTag list = itemTag.getCompound(NBT_ROOT).getList(NBT_COMPONENTS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (list.getString(i).equals(id)) return true;
        return false;
    }

    /** True when the armour slot currently contributes values to this item. */
    public static boolean hasArmor(ItemStack stack) {
        return root(stack, false).getDouble(NBT_ARMOR) > 0.0D;
    }

    public static boolean hasComponent(ItemStack stack, String id) {
        return readComponents(stack).contains(id);
    }

    /** The material installed in the material slot, or empty string when none. */
    public static String readMaterial(ItemStack stack) {
        return root(stack, false).getString(NBT_MATERIAL);
    }

    public static void setMaterial(ItemStack stack, String materialId) {
        CompoundTag root = root(stack, true);
        if (materialId == null || materialId.isEmpty()) root.remove(NBT_MATERIAL);
        else root.putString(NBT_MATERIAL, materialId);
        commit(stack, root);
    }

    public static String idOf(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key == null ? "" : key.toString();
    }

    public static ItemStack stackOf(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ---------------------------------------------------------------- armour slot

    /** Writes the armour values inherited from a chestplate; durability is deliberately not copied. */
    public static void applyArmor(ItemStack target, ItemStack chestplate) {
        if (!(chestplate.getItem() instanceof ArmorItem armor)) return;
        CompoundTag root = root(target, true);
        root.putDouble(NBT_ARMOR, armor.getDefense());
        root.putDouble(NBT_TOUGHNESS, armor.getToughness());
        root.putDouble(NBT_KNOCKBACK, armor.getDefaultAttributeModifiers(
                        net.minecraft.world.entity.EquipmentSlot.CHEST)
                .get(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                .stream()
                .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount)
                .sum());
        root.put(NBT_ARMOR_STACK, chestplate.copyWithCount(1).save(new CompoundTag()));
        commit(target, root);
        mergeEnchantments(target, chestplate);
    }

    public static void clearArmor(ItemStack target) {
        CompoundTag root = root(target, true);
        root.remove(NBT_ARMOR);
        root.remove(NBT_TOUGHNESS);
        root.remove(NBT_KNOCKBACK);
        commit(target, root);
    }

    /** Same enchantment on both items keeps the higher level; what was copied is remembered. */
    private static void mergeEnchantments(ItemStack target, ItemStack source) {
        CompoundTag root = root(target, true);
        ListTag copied = new ListTag();
        Map<Enchantment, Integer> incoming = EnchantmentHelper.getEnchantments(source);
        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            int current = EnchantmentHelper.getItemEnchantmentLevel(entry.getKey(), target);
            if (entry.getValue() > current) {
                target.enchant(entry.getKey(), entry.getValue());
                CompoundTag record = new CompoundTag();
                record.putString("id", String.valueOf(
                        net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey())));
                record.putInt("level", entry.getValue());
                copied.add(record);
            }
        }
        root.put(NBT_ARMOR_ENCH, copied);
        commit(target, root);
    }

    /** Takes the chestplate back out and undoes everything its installation did. */
    public static ItemStack extractArmor(ItemStack target) {
        CompoundTag root = root(target, true);
        ItemStack chestplate = root.contains(NBT_ARMOR_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_ARMOR_STACK)) : ItemStack.EMPTY;
        root.remove(NBT_ARMOR);
        root.remove(NBT_TOUGHNESS);
        root.remove(NBT_KNOCKBACK);
        root.remove(NBT_ARMOR_STACK);
        // Drop the enchantments that this chestplate contributed.
        ListTag copied = root.getList(NBT_ARMOR_ENCH, Tag.TAG_COMPOUND);
        if (!copied.isEmpty()) {
            Map<Enchantment, Integer> current = new java.util.HashMap<>(EnchantmentHelper.getEnchantments(target));
            for (int i = 0; i < copied.size(); i++) {
                CompoundTag record = copied.getCompound(i);
                Enchantment enchantment =
                        net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getValue(
                                new ResourceLocation(record.getString("id")));
                if (enchantment == null) continue;
                if (current.getOrDefault(enchantment, 0) == record.getInt("level")) current.remove(enchantment);
            }
            EnchantmentHelper.setEnchantments(current, target);
        }
        root.remove(NBT_ARMOR_ENCH);
        commit(target, root);
        return chestplate;
    }

    // ---------------------------------------------------------------- elytra slot

    /** Stores the elytra verbatim; it grants flight but never takes durability damage. */
    public static void applyElytra(ItemStack target, ItemStack elytra) {
        CompoundTag root = root(target, true);
        ItemStack copy = elytra.copyWithCount(1);
        // Mark it unbreakable so no elytra-flight tick can wear it down while installed.
        copy.getOrCreateTag().putBoolean("Unbreakable", true);
        root.put(NBT_ELYTRA_STACK, copy.save(new CompoundTag()));
        commit(target, root);
    }

    public static ItemStack extractElytra(ItemStack target) {
        CompoundTag root = root(target, true);
        ItemStack elytra = root.contains(NBT_ELYTRA_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_ELYTRA_STACK)) : ItemStack.EMPTY;
        root.remove(NBT_ELYTRA_STACK);
        commit(target, root);
        return elytra;
    }

    public static boolean hasElytra(ItemStack stack) {
        return root(stack, false).contains(NBT_ELYTRA_STACK, Tag.TAG_COMPOUND);
    }

    public static void applySocket(ItemStack target, ItemStack plugin) {
        CompoundTag root = root(target, true);
        root.put(NBT_SOCKET_STACK, plugin.copyWithCount(1).save(new CompoundTag()));
        commit(target, root);
    }

    public static ItemStack extractSocket(ItemStack target) {
        CompoundTag root = root(target, true);
        ItemStack plugin = root.contains(NBT_SOCKET_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_SOCKET_STACK)) : ItemStack.EMPTY;
        root.remove(NBT_SOCKET_STACK);
        commit(target, root);
        return plugin;
    }

    public static boolean hasSocket(ItemStack stack) {
        return root(stack, false).contains(NBT_SOCKET_STACK, Tag.TAG_COMPOUND);
    }

    /** The plugin installed in the socket slot, without removing it. */
    public static ItemStack readSocket(ItemStack stack) {
        CompoundTag root = root(stack, false);
        return root.contains(NBT_SOCKET_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_SOCKET_STACK)) : ItemStack.EMPTY;
    }

    public static void applyDisc(ItemStack target, ItemStack disc) {
        CompoundTag root = root(target, true);
        root.put(NBT_DISC_STACK, disc.copyWithCount(1).save(new CompoundTag()));
        commit(target, root);
    }

    public static ItemStack extractDisc(ItemStack target) {
        CompoundTag root = root(target, true);
        ItemStack disc = root.contains(NBT_DISC_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_DISC_STACK)) : ItemStack.EMPTY;
        root.remove(NBT_DISC_STACK);
        commit(target, root);
        return disc;
    }

    public static ItemStack readDisc(ItemStack stack) {
        CompoundTag root = root(stack, false);
        return root.contains(NBT_DISC_STACK, Tag.TAG_COMPOUND)
                ? ItemStack.of(root.getCompound(NBT_DISC_STACK)) : ItemStack.EMPTY;
    }
}
