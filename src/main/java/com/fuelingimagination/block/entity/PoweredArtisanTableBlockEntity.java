package com.fuelingimagination.block.entity;

import com.fuelingimagination.block.PoweredArtisanTableBlock;
import com.fuelingimagination.registry.ModBlockEntities;
import com.fuelingimagination.registry.ModItems;
import com.fuelingimagination.content.artisan.ArtisanWorkMode;
import com.fuelingimagination.content.artisan.ArtisanModeHandlers;
import com.fuelingimagination.content.artisan.ArtisanProcessStage;
import com.fuelingimagination.content.artisan.ArtisanCraftingHandler;
import com.fuelingimagination.content.artisan.DamagedBlockRepairHandler;
import com.fuelingimagination.menu.PoweredArtisanTableMenu;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

public final class PoweredArtisanTableBlockEntity extends KineticBlockEntity implements MenuProvider {
    public static final float MIN_SPEED = 32.0F;
    public static final float STRESS_IMPACT = 16.0F;
    public static final int INVENTORY_SIZE = 19, OUTPUT_SLOT = 9, MAIN_SLOT = 10, COMPONENT_START = 11;
    private ArtisanWorkMode workMode = ArtisanWorkMode.MANUFACTURE;
    private ArtisanCraftingHandler.Batch craftingBatch;
    private ArtisanProcessStage processStage = ArtisanProcessStage.IDLE;
    private int openSlots;
    private int dirtyParts;
    private final int[] cleanProgress = new int[9];
    private long lastCleanTick = Long.MIN_VALUE;
    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE) {
        @Override public void setChanged() { super.setChanged(); PoweredArtisanTableBlockEntity.this.setChanged(); }
    };
    // Display-only parts: these are never drops, recipe ingredients, or extractable inventory.
    private final SimpleContainer repairParts = new SimpleContainer(9);
    // What each part should be once repaired. Never rendered; used to validate the swap.
    private final SimpleContainer expectedParts = new SimpleContainer(9);
    // Real ingredients (tags included), rebuilt from the recipe id after a reload.
    private Ingredient[] partIngredients = new Ingredient[9];
    private String recipeId = "";

    public PoweredArtisanTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_ARTISAN_TABLE.get(), pos, state);
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    @Override
    public boolean isSpeedRequirementFulfilled() {
        return Math.abs(getSpeed()) >= MIN_SPEED;
    }

    public ArtisanWorkMode getWorkMode() { return workMode; }
    public SimpleContainer getInventory() { return inventory; }
    public SimpleContainer getRepairParts() { return repairParts; }
    public ArtisanProcessStage getProcessStage() { return processStage; }
    public int getOpenSlots() { return openSlots; }
    public int getDirtyParts() { return dirtyParts; }
    public boolean isDirtyPart(int part) { return part >= 0 && part < 9 && (dirtyParts & 1 << part) != 0; }
    public boolean isBrokenPart(int part) {
        return part >= 0 && part < 9 && repairParts.getItem(part).is(ModItems.DAMAGED_COMPONENT.get());
    }
    public ItemStack getExpectedPart(int part) { return part >= 0 && part < 9 ? expectedParts.getItem(part) : ItemStack.EMPTY; }
    /** Registry id of the part that should occupy the slot, or -1. Used only for client sync. */
    public int getExpectedPartId(int part) {
        ItemStack expected = getExpectedPart(part);
        return expected.isEmpty() ? -1 : Item.getId(expected.getItem());
    }
    public int getCleanProgress(int part) { return cleanProgress[part]; }
    public boolean canSwitchWorkMode() { return !isCrafting() && processStage == ArtisanProcessStage.IDLE && inventory.isEmpty(); }
    public boolean isCrafting() { return craftingBatch != null; }
    public ArtisanCraftingHandler.Batch getCraftingBatch() { return craftingBatch; }
    public void setCraftingBatch(ArtisanCraftingHandler.Batch batch) { craftingBatch = batch; }

    /** Mode-handler extension point. The count unlocks ring slots clockwise from the top. */
    public void setProcessState(ArtisanProcessStage stage, int unlockedSlots) {
        if (level == null || level.isClientSide) return;
        processStage = stage;
        openSlots = Math.max(0, Math.min(8, unlockedSlots));
        setChanged();
        sendData();
    }

    public void beginRepair(NonNullList<ItemStack> parts, int dirtyMask,
                            NonNullList<ItemStack> expected, Ingredient[] ingredients, String recipeId) {
        if (level == null || level.isClientSide) return;
        for (int i = 0; i < 9; i++) {
            repairParts.setItem(i, parts.get(i).copy());
            expectedParts.setItem(i, expected.get(i).copy());
            partIngredients[i] = ingredients[i];
            cleanProgress[i] = 0;
        }
        this.recipeId = recipeId == null ? "" : recipeId;
        dirtyParts = dirtyMask & 511;
        lastCleanTick = Long.MIN_VALUE;
        setProcessState(ArtisanProcessStage.REPAIR_DISASSEMBLED, 0);
    }

    public boolean cleanRepairPart(int part) {
        if (level == null || level.isClientSide || part < 0 || part >= 9) return false;
        if (!isDirtyPart(part)) return false;
        long tick = level.getGameTime();
        if (lastCleanTick != Long.MIN_VALUE && tick - lastCleanTick < 3) return false;
        lastCleanTick = tick;
        if (++cleanProgress[part] >= 6) dirtyParts &= ~(1 << part);
        refreshRepairStage();
        return true;
    }

    /** True when this slot may receive the stack: it is empty, not dirty, and the right ingredient. */
    public boolean acceptsPart(int part, ItemStack stack) {
        if (level == null || part < 0 || part >= 9) return false;
        if (isDirtyPart(part) || !repairParts.getItem(part).isEmpty()) return false;
        return matchesExpected(part, stack);
    }

    /** Compares against the recipe's real ingredient, so tag-based recipes accept any member. */
    public boolean matchesExpected(int part, ItemStack stack) {
        ItemStack expected = expectedParts.getItem(part);
        if (expected.isEmpty() || stack.isEmpty()) return false;
        ensureIngredients();
        Ingredient ingredient = partIngredients[part];
        return ingredient != null ? ingredient.test(stack) : ItemStack.isSameItemSameTags(stack, expected);
    }

    /**
     * Called after a part was lifted out of its slot.
     *
     * <p>Taking anything only leaves a hole; the block is not consumed here. Scrapping happens
     * when the player cancels or closes the screen with the assembly still incomplete, so a
     * mis-click never destroys the block.
     */
    public void onPartTaken(int part, ItemStack taken, Player player) {
        if (level == null || level.isClientSide || part < 0 || part >= 9) return;
        dirtyParts &= ~(1 << part);
        cleanProgress[part] = 0;
        setChanged();
        refreshRepairStage();
    }

    /** A part was put back into an empty slot; a fresh part is never dirty and may complete the repair. */
    public void onPartPlaced(int part) {
        if (level == null || level.isClientSide || part < 0 || part >= 9) return;
        dirtyParts &= ~(1 << part);
        cleanProgress[part] = 6;
        setChanged();
        refreshRepairStage();
    }

    /** Destroys the damaged block and hands over what can be recovered. Dirty parts are lost. */
    public void scrap(Player player) {
        if (level == null || level.isClientSide) return;
        if (player != null) {
            for (int i = 0; i < 9; i++) {
                ItemStack part = repairParts.getItem(i);
                if (part.isEmpty() || isDirtyPart(i)) continue;
                giveToPlayer(player, part.copy());
            }
        }
        inventory.setItem(MAIN_SLOT, ItemStack.EMPTY);
        resetProcess();
    }

    private void giveToPlayer(Player player, ItemStack stack) {
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(
                new PlayerMainInvWrapper(player.getInventory()), stack, false);
        if (!remainder.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1,
                    worldPosition.getZ() + 0.5, remainder);
        }
    }

    /** Ingredients are transient; after a reload they are rebuilt from the stored recipe id. */
    private void ensureIngredients() {
        if (partIngredients[0] != null || recipeId.isEmpty() || level == null) return;
        var recipe = level.getRecipeManager().byKey(new ResourceLocation(recipeId)).orElse(null);
        if (recipe instanceof CraftingRecipe crafting) {
            partIngredients = DamagedBlockRepairHandler.gridIngredients(crafting);
        }
    }

    /** Reassembly needs every dirty part wiped and every slot holding its real ingredient. */
    public boolean canAssemble() {
        if (dirtyParts != 0) return false;
        for (int i = 0; i < 9; i++) {
            if (expectedParts.getItem(i).isEmpty()) continue;
            ItemStack current = repairParts.getItem(i);
            if (current.isEmpty() || !matchesExpected(i, current)) return false;
        }
        return true;
    }

    private void refreshRepairStage() {
        if (processStage == ArtisanProcessStage.IDLE) return;
        setProcessState(canAssemble() ? ArtisanProcessStage.REPAIR_READY : ArtisanProcessStage.REPAIR_CLEANING, 0);
    }

    public void resetProcess() {
        repairParts.clearContent();
        expectedParts.clearContent();
        java.util.Arrays.fill(partIngredients, null);
        recipeId = "";
        dirtyParts = 0;
        java.util.Arrays.fill(cleanProgress, 0);
        setProcessState(ArtisanProcessStage.IDLE, 0);
    }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        craftingBatch = null;
        Containers.dropContents(level, worldPosition, inventory);
        inventory.clearContent();
    }

    public void setWorkMode(ArtisanWorkMode mode) {
        if (level == null || level.isClientSide || mode == null || workMode == mode
                || !isSpeedRequirementFulfilled() || !canSwitchWorkMode()) return;
        ArtisanModeHandlers.get(workMode).onDeselected(this);
        workMode = mode;
        setChanged();
        sendData();
        ArtisanModeHandlers.get(workMode).onSelected(this);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putString("WorkMode", workMode.id());
        tag.putString("ProcessStage", processStage.name());
        tag.putInt("OpenSlots", openSlots);
        tag.putInt("DirtyParts", dirtyParts);
        tag.putIntArray("CleanProgress", cleanProgress);
        if (!clientPacket) {
            tag.put("Inventory", saveContainer(inventory));
            tag.put("RepairParts", saveContainer(repairParts));
            tag.put("ExpectedParts", saveContainer(expectedParts));
            tag.putString("RepairRecipe", recipeId);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        workMode = ArtisanWorkMode.fromId(tag.getString("WorkMode"));
        processStage = ArtisanProcessStage.fromId(tag.getString("ProcessStage"));
        openSlots = Math.max(0, Math.min(8, tag.getInt("OpenSlots")));
        dirtyParts = tag.getInt("DirtyParts") & 511;
        int[] savedProgress = tag.getIntArray("CleanProgress");
        for (int i = 0; i < 9; i++) cleanProgress[i] = i < savedProgress.length ? Math.max(0, Math.min(6, savedProgress[i])) : 0;
        if (!clientPacket) {
            loadContainer(inventory, tag.getCompound("Inventory"));
            loadContainer(repairParts, tag.getCompound("RepairParts"));
            loadContainer(expectedParts, tag.getCompound("ExpectedParts"));
            recipeId = tag.getString("RepairRecipe");
            java.util.Arrays.fill(partIngredients, null);
            // Inputs and completed results persist; an interrupted batch requires a new lever pull.
            craftingBatch = null;
        }
    }

    private static CompoundTag saveContainer(Container container) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) items.set(i, container.getItem(i));
        return ContainerHelper.saveAllItems(new CompoundTag(), items);
    }

    private static void loadContainer(Container container, CompoundTag tag) {
        NonNullList<ItemStack> items = NonNullList.withSize(container.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
        for (int i = 0; i < items.size(); i++) container.setItem(i, items.get(i));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.fuelingimagination.powered_artisan_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PoweredArtisanTableMenu(id, inventory, this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        boolean active = isSpeedRequirementFulfilled();
        if (active) ArtisanModeHandlers.get(workMode).tickPowered(this);
        if (state.getValue(PoweredArtisanTableBlock.ACTIVE) == active) return;
        for (int part = 0; part < 3; part++) {
            BlockPos target = PoweredArtisanTableBlock.partPosition(worldPosition,
                    state.getValue(PoweredArtisanTableBlock.FACING), part);
            BlockState other = level.getBlockState(target);
            if (other.is(state.getBlock()) && other.getValue(PoweredArtisanTableBlock.PART) == part
                    && other.getValue(PoweredArtisanTableBlock.FACING) == state.getValue(PoweredArtisanTableBlock.FACING)) {
                level.setBlock(target, other.setValue(PoweredArtisanTableBlock.ACTIVE, active), Block.UPDATE_ALL);
            }
        }
    }
}
