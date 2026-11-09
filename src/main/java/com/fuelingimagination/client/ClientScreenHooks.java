package com.fuelingimagination.client;

import com.fuelingimagination.block.entity.ProjectorBlockEntity;
import net.minecraft.client.Minecraft;

/** Dist-safe indirection so the common side never loads a client screen class. */
public final class ClientScreenHooks {
    private ClientScreenHooks() { }

    public static void openProjector(ProjectorBlockEntity projector) {
        Minecraft.getInstance().setScreen(new ProjectorEditScreen(projector));
    }
}