package com.fuelingimagination.api;

import net.minecraft.world.entity.player.Player;

/**
 * Duck interface for the hand crank. Deliberately outside {@code com.fuelingimagination.mixin}:
 * classes inside a mixin package may not be referenced directly.
 */
public interface HandCrankCranker {
    void cgf$setCranker(Player player);

    Player cgf$getCranker();
}
