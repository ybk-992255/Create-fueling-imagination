package com.fuelingimagination.content.damaged;

import com.fuelingimagination.FuelingImagination;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * The "damaged" state of a placeable block.
 *
 * <p>The state lives in the stack's NBT rather than in a separate item, because Create's
 * blocks are registered by Create and cannot be subclassed. Keeping it in NBT means no
 * new registry entries, no impact on existing saves, and it works for any mod's block.
 *
 * <p>Which items may carry the state is defined entirely by the item tag
 * {@code fuelingimagination:damageable_blocks}. Another mod or datapack extends it by
 * shipping its own tag file with {@code "replace": false}; no code change is required.
 */
public final class DamagedBlocks {
    /** Item tag listing every item that is allowed to become damaged. */
    public static final TagKey<Item> DAMAGEABLE_BLOCKS =
            TagKey.create(Registries.ITEM, new ResourceLocation(FuelingImagination.MOD_ID, "damageable_blocks"));

    /**
     * Extra pool for scrap crates: items that only appear with a low chance, and always as a
     * single item. Kept separate from {@link #DAMAGEABLE_BLOCKS} so entries can be added for
     * special cases (creative blocks now, more later) without touching the crate code.
     */
    public static final TagKey<Item> SCRAP_CRATE_SPECIAL =
            TagKey.create(Registries.ITEM, new ResourceLocation(FuelingImagination.MOD_ID, "scrap_crate_special"));

    /** NBT key holding the damaged flag. Namespaced to avoid collisions with other mods. */
    public static final String NBT_DAMAGED = FuelingImagination.MOD_ID + ":damaged";

    private DamagedBlocks() { }

    /** True for non-empty item stacks that belong to the definition file. */
    public static boolean canBeDamaged(ItemStack stack) {
        return !stack.isEmpty() && stack.is(DAMAGEABLE_BLOCKS);
    }

    /** True when this stack already carries the damaged flag. */
    public static boolean isDamaged(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(NBT_DAMAGED);
    }

    /** Flags the stack in place. Silently does nothing for items outside the definition file. */
    public static void markDamaged(ItemStack stack) {
        if (!canBeDamaged(stack)) return;
        stack.getOrCreateTag().putBoolean(NBT_DAMAGED, true);
    }

    /** Builds a fresh single damaged stack for the given block item. */
    public static ItemStack damaged(ItemLike item) {
        ItemStack stack = new ItemStack(item);
        markDamaged(stack);
        return stack;
    }

    /**
     * Repairs the stack in place by removing the flag.
     *
     * <p>Only the mod's own tag key is removed; any other NBT the stack carries (a custom
     * name, a block entity tag) survives the repair untouched.
     */
    public static void repair(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (tag == null) return;
        tag.remove(NBT_DAMAGED);
        if (tag.isEmpty()) stack.setTag(null);
    }
}
