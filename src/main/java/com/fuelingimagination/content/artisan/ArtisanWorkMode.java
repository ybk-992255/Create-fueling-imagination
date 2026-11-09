package com.fuelingimagination.content.artisan;

import net.minecraft.network.chat.Component;

public enum ArtisanWorkMode {
    MANUFACTURE("manufacture", 0xB66212), MODIFY("modify", 0x236DAD), REPAIR("repair", 0x30834B);

    private final String id;
    private final int textColor;

    ArtisanWorkMode(String id, int textColor) { this.id = id; this.textColor = textColor; }

    public String id() { return id; }
    public int textColor() { return textColor; }

    public Component title() {
        return Component.translatable("gui.fuelingimagination.artisan.mode." + id);
    }

    public ArtisanWorkMode cycle(int direction) {
        return values()[Math.floorMod(ordinal() + direction, values().length)];
    }

    public static ArtisanWorkMode fromIndex(int index) {
        return index >= 0 && index < values().length ? values()[index] : MANUFACTURE;
    }

    public static ArtisanWorkMode fromId(String id) {
        for (ArtisanWorkMode mode : values()) if (mode.id.equals(id)) return mode;
        return MANUFACTURE;
    }
}
