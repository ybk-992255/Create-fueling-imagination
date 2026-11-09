package com.fuelingimagination.block.entity;

import com.fuelingimagination.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds the projected caption and its framing. */
public class ProjectorBlockEntity extends BlockEntity {
    public static final int MAX_LENGTH = 256;
    public static final float MIN_UNIT_SIZE = 0.25F;
    public static final float MAX_UNIT_SIZE = 4.0F;
    public static final float MIN_DISTANCE = 1.0F;
    public static final float MAX_DISTANCE = 16.5F;
    public static final float MIN_OPACITY = 0.1F;
    public static final float MAX_OPACITY = 1.0F;
    private static final String TAG_TEXT = "Text";
    private static final String TAG_UNIT_SIZE = "UnitSize";
    private static final String TAG_DISTANCE = "Distance";
    private static final String TAG_HUE = "Hue";
    private static final String TAG_OPACITY = "Opacity";

    private String text = "";
    private float unitSize = 1.0F;
    private float distance = 2.4F;
    private float hue = 45.0F;
    private float opacity = 1.0F;

    public ProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROJECTOR.get(), pos, state);
    }

    public String getText() { return text; }
    public float getUnitSize() { return unitSize; }
    public float getDistance() { return distance; }
    public float getHue() { return hue; }
    public float getOpacity() { return opacity; }

    public void apply(String text, float unitSizeValue, float distanceValue, float hueValue, float opacityValue) {
        String trimmed = text == null ? "" : text;
        if (trimmed.length() > MAX_LENGTH) trimmed = trimmed.substring(0, MAX_LENGTH);
        this.text = trimmed;
        this.unitSize = clamp(unitSizeValue, MIN_UNIT_SIZE, MAX_UNIT_SIZE);
        this.distance = clamp(distanceValue, MIN_DISTANCE, MAX_DISTANCE);
        this.hue = ((hueValue % 360.0F) + 360.0F) % 360.0F;
        this.opacity = clamp(opacityValue, MIN_OPACITY, MAX_OPACITY);
        setChanged();
        sync();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString(TAG_TEXT, text);
        tag.putFloat(TAG_UNIT_SIZE, unitSize);
        tag.putFloat(TAG_DISTANCE, distance);
        tag.putFloat(TAG_HUE, hue);
        tag.putFloat(TAG_OPACITY, opacity);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        text = tag.getString(TAG_TEXT);
        unitSize = tag.contains(TAG_UNIT_SIZE) ? tag.getFloat(TAG_UNIT_SIZE) : 1.0F;
        distance = tag.contains(TAG_DISTANCE) ? tag.getFloat(TAG_DISTANCE) : 2.4F;
        hue = tag.contains(TAG_HUE) ? tag.getFloat(TAG_HUE) : 45.0F;
        opacity = tag.contains(TAG_OPACITY) ? tag.getFloat(TAG_OPACITY) : 1.0F;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) load(packet.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}