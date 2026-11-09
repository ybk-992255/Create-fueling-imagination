package com.fuelingimagination.content.artisan;

public enum ArtisanProcessStage {
    IDLE, MODIFICATION_OPENING, MODIFICATION_READY, REPAIR_DISASSEMBLED, REPAIR_CLEANING, REPAIR_READY;

    public boolean locksMainItem() { return this != IDLE; }

    public static ArtisanProcessStage fromIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : IDLE;
    }

    public static ArtisanProcessStage fromId(String id) {
        for (ArtisanProcessStage stage : values()) if (stage.name().equals(id)) return stage;
        return IDLE;
    }
}
