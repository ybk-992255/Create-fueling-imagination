package com.fuelingimagination.block.entity;

import com.fuelingimagination.block.PoweredDismantlerBlock;
import com.fuelingimagination.content.dismantler.DismantlingRecipes;
import com.fuelingimagination.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Powered dismantler: takes one stack of blocks, turns each into parts, and buffers the parts.
 *
 * <p>No menu. Right-click with a block to insert, empty-handed to take the input back. Nine
 * output slots are exposed on the bottom face so hoppers and chutes can pull the parts out;
 * once seven of them hold something the machine stops dismantling until space frees up. The
 * stress cost is constant, so a stalled machine still loads the network.
 */
public final class PoweredDismantlerBlockEntity extends KineticBlockEntity {
    public static final float MIN_SPEED = 32.0F;
    public static final float STRESS_IMPACT = 8.0F;
    public static final int PROCESS_TIME = 40;
    public static final int OUTPUT_SLOTS = 9;
    /** Processing halts once this many output slots are occupied. */
    public static final int OUTPUT_STOP_AT = 7;

    private ItemStack input = ItemStack.EMPTY;
    private final ItemStack[] output = new ItemStack[OUTPUT_SLOTS];
    private int progress;
    /** Drives the slow bleed-off that keeps the buffer from backing up. */
    private int spillTimer;

    public PoweredDismantlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWERED_DISMANTLER.get(), pos, state);
        for (int i = 0; i < OUTPUT_SLOTS; i++) output[i] = ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- input side

    private final IItemHandler inputHandler = new IItemHandler() {
        @Override public int getSlots() { return 1; }

        @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? input.copy() : ItemStack.EMPTY; }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty() || !DismantlingRecipes.canProcess(stack)) return stack;
            if (!input.isEmpty() && !ItemStack.isSameItemSameTags(input, stack)) return stack;

            int limit = Math.min(64, input.isEmpty() ? stack.getMaxStackSize() : input.getMaxStackSize());
            int room = limit - input.getCount();
            if (room <= 0) return stack;

            int moved = Math.min(room, stack.getCount());
            if (!simulate) {
                if (input.isEmpty()) input = stack.copyWithCount(moved);
                else input.grow(moved);
                setChanged();
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(moved);
            return remainder;
        }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Automation may feed the machine but must not pull its input back out.
            return ItemStack.EMPTY;
        }

        @Override public int getSlotLimit(int slot) { return 64; }

        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && DismantlingRecipes.canProcess(stack);
        }
    };
    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inputHandler);

    // ---------------------------------------------------------------- output side

    /** Bottom face: extraction only, so hoppers and chutes can take the parts out. */
    private final IItemHandler outputHandler = new IItemHandler() {
        @Override public int getSlots() { return OUTPUT_SLOTS; }

        @Override public ItemStack getStackInSlot(int slot) { return validSlot(slot) ? output[slot].copy() : ItemStack.EMPTY; }

        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }

        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!validSlot(slot) || amount <= 0 || output[slot].isEmpty()) return ItemStack.EMPTY;
            int taken = Math.min(amount, output[slot].getCount());
            ItemStack result = output[slot].copyWithCount(taken);
            if (!simulate) {
                output[slot].shrink(taken);
                if (output[slot].isEmpty()) output[slot] = ItemStack.EMPTY;
                setChanged();
            }
            return result;
        }

        @Override public int getSlotLimit(int slot) { return 64; }

        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };
    private final LazyOptional<IItemHandler> outputItemHandler = LazyOptional.of(() -> outputHandler);

    private boolean validSlot(int slot) { return slot >= 0 && slot < OUTPUT_SLOTS; }

    // ---------------------------------------------------------------- kinetics

    @Override public float calculateStressApplied() {
        lastStressApplied = STRESS_IMPACT;
        return STRESS_IMPACT;
    }

    @Override public boolean isSpeedRequirementFulfilled() {
        return !isOverStressed() && Math.abs(getSpeed()) >= MIN_SPEED;
    }

    // ---------------------------------------------------------------- player helpers

    /** Inserts as much of the stack as fits; returns the number actually moved. */
    public int insert(ItemStack stack) {
        int before = stack.getCount();
        ItemStack remainder = inputHandler.insertItem(0, stack, false);
        return before - remainder.getCount();
    }

    /** Player-facing extraction of the input. Automation cannot do this. */
    public ItemStack extractAll() {
        if (input.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = input;
        input = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return result;
    }

    public ItemStack getInput() { return input.copy(); }

    public void dropContents() {
        if (level == null || level.isClientSide) return;
        if (!input.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5, input.copy());
        }
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (!output[i].isEmpty()) dropVertically(output[i]);
            output[i] = ItemStack.EMPTY;
        }
        input = ItemStack.EMPTY;
        progress = 0;
        setChanged();
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) return;

        drainOutput();

        boolean powered = !input.isEmpty() && isSpeedRequirementFulfilled();
        boolean hasRoom = occupiedOutputSlots() < OUTPUT_STOP_AT;
        setActive(powered && hasRoom);
        if (!powered || !hasRoom) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        if (++progress < PROCESS_TIME) return;
        progress = 0;
        processOne();
    }

    /**
     * Empties one whole slot out of the buffer every ten ticks.
     *
     * <p>This is the overflow guard: the machine produces a whole batch every 40 ticks but only
     * sheds one slot per 10 ticks, so the buffer slowly fills. When seven slots are taken the machine
     * pauses, which is the hard stop behind this soft one.
     */
    private void drainOutput() {
        if (occupiedOutputSlots() == 0) {
            spillTimer = 0;
            return;
        }
        if (++spillTimer < 10) return;
        spillTimer = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            if (output[i].isEmpty()) continue;
            ItemStack whole = output[i];
            output[i] = ItemStack.EMPTY;
            dropVertically(whole);
            setChanged();
            return;
        }
    }
    private int occupiedOutputSlots() {
        int used = 0;
        for (int i = 0; i < OUTPUT_SLOTS; i++) if (!output[i].isEmpty()) used++;
        return used;
    }

    private void processOne() {
        ItemStack one = input.copyWithCount(1);
        List<ItemStack> results = DismantlingRecipes.roll(one, level);
        input.shrink(1);
        if (input.isEmpty()) input = ItemStack.EMPTY;

        for (ItemStack result : results) {
            if (result.isEmpty()) continue;
            storeOrSpill(result);
        }
        level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.7F, 1.0F);
        setChanged();
    }

    /** Merges into a matching slot, then an empty one; spills straight down if neither exists. */
    private void storeOrSpill(ItemStack result) {
        ItemStack leftover = result.copy();

        for (int i = 0; i < OUTPUT_SLOTS && !leftover.isEmpty(); i++) {
            if (output[i].isEmpty() || !ItemStack.isSameItemSameTags(output[i], leftover)) continue;
            int room = Math.min(64, output[i].getMaxStackSize()) - output[i].getCount();
            if (room <= 0) continue;
            int moved = Math.min(room, leftover.getCount());
            output[i].grow(moved);
            leftover.shrink(moved);
        }
        for (int i = 0; i < OUTPUT_SLOTS && !leftover.isEmpty(); i++) {
            if (!output[i].isEmpty()) continue;
            output[i] = leftover.copy();
            leftover = ItemStack.EMPTY;
        }
        if (!leftover.isEmpty()) dropVertically(leftover);
    }

    /**
     * Drops with no horizontal spread, so parts fall through the block's own column.
     *
     * <p>The entity is spawned one block below the machine, not just under its bottom face:
     * spawning inside the block's collision shape made the game eject the item sideways.
     */
    private void dropVertically(ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() - 0.5D,
                worldPosition.getZ() + 0.5D, stack.copy());
        entity.setDeltaMovement(0.0D, -0.1D, 0.0D);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (!state.hasProperty(PoweredDismantlerBlock.ACTIVE)
                || state.getValue(PoweredDismantlerBlock.ACTIVE) == active) return;
        level.setBlock(worldPosition, state.setValue(PoweredDismantlerBlock.ACTIVE, active),
                Block.UPDATE_CLIENTS);
    }

    // ---------------------------------------------------------------- capabilities

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (side == Direction.DOWN) return outputItemHandler.cast();
            return itemHandler.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        itemHandler.invalidate();
        outputItemHandler.invalidate();
        super.invalidateCaps();
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (!clientPacket) {
            tag.put("Input", input.save(new CompoundTag()));
            tag.putInt("Progress", progress);
            ListTag outputs = new ListTag();
            for (ItemStack stack : output) outputs.add(stack.save(new CompoundTag()));
            tag.put("Output", outputs);
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (!clientPacket) {
            input = ItemStack.of(tag.getCompound("Input"));
            progress = tag.getInt("Progress");
            ListTag outputs = tag.getList("Output", Tag.TAG_COMPOUND);
            for (int i = 0; i < OUTPUT_SLOTS; i++) {
                output[i] = i < outputs.size() ? ItemStack.of(outputs.getCompound(i)) : ItemStack.EMPTY;
            }
        }
    }
}
