package com.fuelingimagination.client;

import net.minecraft.client.Minecraft;

/** Kept separate so the item class never loads client-only types on a dedicated server. */
public final class ClientBridge {
    public static void openManual() {
        Minecraft.getInstance().setScreen(new RepairManualScreen());
    }

    private ClientBridge() { }
}
