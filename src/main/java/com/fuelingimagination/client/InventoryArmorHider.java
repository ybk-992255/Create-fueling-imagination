package com.fuelingimagination.client;

/**
 * Client-side toggle for hiding the armour models in the inventory's player preview.
 *
 * <p>Deliberately a plain static flag: it only affects rendering and must never be saved.
 */
public final class InventoryArmorHider {
    private static boolean hiding = false;

    public static boolean isHiding() {
        return hiding;
    }

    public static void toggle() {
        hiding = !hiding;
    }

    private InventoryArmorHider() { }
}
