package com.fuelingimagination.content.plugin;

import net.createmod.catnip.data.Couple;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Key bindings for the hand-held "wireless typewriter" (the redstone plugin's control mode). */
public final class WirelessTypewriter {
    private static final String PENDING = "fuelingimagination:wt_pending";
    private static final String KEYS = "fuelingimagination:wt_keys";
    private static final String CONTROL = "fuelingimagination:wt_control";

    private WirelessTypewriter() { }

    private static CompoundTag tag(ItemStack stack, boolean create) {
        if (create) return stack.getOrCreateTag();
        return stack.hasTag() ? stack.getTag() : new CompoundTag();
    }

    /** The two item ids of the frequency captured from a wireless terminal, or null. */
    public static String[] pending(ItemStack stack) {
        CompoundTag root = tag(stack, false);
        if (!root.contains(PENDING, Tag.TAG_COMPOUND)) return null;
        CompoundTag pending = root.getCompound(PENDING);
        return new String[]{pending.getString("a"), pending.getString("b")};
    }

    public static void setPending(ItemStack stack, String a, String b) {
        CompoundTag root = tag(stack, true);
        CompoundTag pending = new CompoundTag();
        pending.putString("a", a);
        pending.putString("b", b);
        root.put(PENDING, pending);
    }

    public static void clearPending(ItemStack stack) {
        CompoundTag root = tag(stack, true);
        root.remove(PENDING);
    }

    /** Key name used by the client (GLFW key name, e.g. "KEY_A", "KEY_1"). */
    public static void bindKey(ItemStack stack, String key, String a, String b) {
        CompoundTag root = tag(stack, true);
        CompoundTag keys = root.contains(KEYS, Tag.TAG_COMPOUND) ? root.getCompound(KEYS) : new CompoundTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("a", a);
        entry.putString("b", b);
        keys.put(key, entry);
        root.put(KEYS, keys);
        clearPending(stack);
    }

    public static String[] keyFrequency(ItemStack stack, String key) {
        CompoundTag root = tag(stack, false);
        if (!root.contains(KEYS, Tag.TAG_COMPOUND)) return null;
        CompoundTag keys = root.getCompound(KEYS);
        if (!keys.contains(key, Tag.TAG_COMPOUND)) return null;
        CompoundTag entry = keys.getCompound(key);
        return new String[]{entry.getString("a"), entry.getString("b")};
    }

    public static void clearKey(ItemStack stack, String key) {
        CompoundTag root = tag(stack, false);
        if (!root.contains(KEYS, Tag.TAG_COMPOUND)) return;
        CompoundTag keys = root.getCompound(KEYS);
        keys.remove(key);
        root.put(KEYS, keys);
        stack.setTag(root);
    }

        public static List<String> boundKeys(ItemStack stack) {
        List<String> out = new ArrayList<>();
        CompoundTag root = tag(stack, false);
        if (!root.contains(KEYS, Tag.TAG_COMPOUND)) return out;
        for (String key : root.getCompound(KEYS).getAllKeys()) out.add(key);
        return out;
    }

    public static boolean control(ItemStack stack) {
        return tag(stack, false).getBoolean(CONTROL);
    }

    public static void setControl(ItemStack stack, boolean on) {
        CompoundTag root = tag(stack, true);
        if (on) root.putBoolean(CONTROL, true);
        else root.remove(CONTROL);
    }

    public static String idOf(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }
}
