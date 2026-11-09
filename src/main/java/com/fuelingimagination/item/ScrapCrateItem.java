package com.fuelingimagination.item;

import com.fuelingimagination.content.damaged.DamagedBlocks;
import com.fuelingimagination.registry.ModItems;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrap crate: a Create cardboard package whose contents are random damaged blocks.
 *
 * <p>It is a real {@link PackageItem}, so placing it, breaking it and opening it all go
 * through Create's own package code — the crate is not a separate container system.
 * Only two things are ours: the name is light purple, and shift-right-click empties the
 * crate straight into the player's inventory.
 */
public final class ScrapCrateItem extends PackageItem {
    /** Chances and counts are deliberately here rather than in a config for now. */
    private static final int MAX_KINDS = 9;
    private static final int MIN_KINDS = 1;
    private static final int MAX_PER_KIND = 3;
    /** One crate in a hundred also carries one special (creative) block. */
    private static final float SPECIAL_CHANCE = 0.01F;
    /** Marks a crate whose contents were already rolled, so emptying it does not reroll. */
    private static final String NBT_ROLLED = "fuelingimagination:rolled";

    public ScrapCrateItem(Item.Properties properties) {
        super(properties, defaultStyle());
        // PackageItem's constructor registers every package into Create's global pools, and
        // PackageStyles.getRandomBox() picks from those pools. Left in, our crate would be handed
        // out as if it were a stock cardboard box (and then fill itself with scrap on first tick).
        PackageStyles.ALL_BOXES.remove(this);
        PackageStyles.STANDARD_BOXES.remove(this);
        PackageStyles.RARE_BOXES.remove(this);
    }

    /**
     * A style of our own.
     *
     * <p>Reusing one of Create's stock styles was a serious bug: Create creates packages by
     * looking the item up from the style, so sharing a stock style made every package from
     * storage come back as one of ours. A private style keeps the two sets apart; the renderer
     * is pointed at a stock package model separately.
     */
    private static PackageStyles.PackageStyle defaultStyle() {
        return new PackageStyles.PackageStyle("scrap_crate", 12, 12, 0.0F, false);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.fuelingimagination.scrap_crate").withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /** Builds a crate with freshly rolled contents. */
    public static ItemStack create(RandomSource random) {
        ItemStack crate = new ItemStack(ModItems.SCRAP_CRATE.get());
        fill(crate, random);
        return crate;
    }

    /**
     * Rolls contents on first tick in an inventory instead of at creation.
     *
     * <p>The creative tab builds its item list once and hands out copies of the same stack,
     * so rolling at creation made every crate taken from the tab identical. Rolling per
     * stack, once, keeps them independent blind boxes.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity,
                              int slot, boolean selected) {
        if (level.isClientSide) return;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.getBoolean(NBT_ROLLED)) return;
        fill(stack, level.random);
        stack.getOrCreateTag().putBoolean(NBT_ROLLED, true);
    }

    private static void fill(ItemStack crate, RandomSource random) {
        ItemStack template = PackageItem.containing(rollContents(random));
        CompoundTag tag = template.getTag();
        if (tag != null) crate.setTag(tag.copy());
        else crate.setTag(new CompoundTag());
        crate.getOrCreateTag().putBoolean(NBT_ROLLED, true);
    }

    private static List<ItemStack> rollContents(RandomSource random) {
        List<ItemStack> pool = new ArrayList<>();
        List<ItemStack> special = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            ItemStack candidate = new ItemStack(item);
            if (candidate.is(DamagedBlocks.DAMAGEABLE_BLOCKS) && !candidate.is(DamagedBlocks.SCRAP_CRATE_SPECIAL)) pool.add(candidate);
            if (candidate.is(DamagedBlocks.SCRAP_CRATE_SPECIAL)) special.add(candidate);
        }

        List<ItemStack> contents = new ArrayList<>();
        // The special entry takes a package slot of its own, so reserve it before rolling kinds.
        boolean addSpecial = !special.isEmpty() && random.nextFloat() < SPECIAL_CHANCE;
        int maxKinds = addSpecial ? MAX_KINDS - 1 : MAX_KINDS;
        int kinds = MIN_KINDS + random.nextInt(maxKinds - MIN_KINDS + 1);
        for (int i = 0; i < kinds && !pool.isEmpty(); i++) {
            ItemStack pick = pool.remove(random.nextInt(pool.size()));
            ItemStack damaged = DamagedBlocks.damaged(pick.getItem());
            damaged.setCount(1 + random.nextInt(MAX_PER_KIND));
            contents.add(damaged);
        }

        // The special pool is separate, never repeats a kind, and always comes as a single item.
        if (addSpecial) {
            ItemStack pick = special.get(random.nextInt(special.size()));
            ItemStack damaged = DamagedBlocks.damaged(pick.getItem());
            damaged.setCount(1);
            contents.add(damaged);
        }
        return contents;
    }

    /** Shift-right-click: empty the crate into the inventory, spilling what does not fit. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return super.use(level, player, hand);

        if (!level.isClientSide) {
            ItemStackHandler contents = PackageItem.getContents(stack);
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                ItemStack inside = contents.getStackInSlot(slot);
                if (inside.isEmpty()) continue;
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                        new PlayerMainInvWrapper(player.getInventory()), inside.copy(), false);
                if (!remainder.isEmpty()) player.drop(remainder, false);
            }
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
