package com.fuelingimagination.client;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side state for the anchor teleport windup (60 ticks). Carries a per-windup random seed so
 * the renderer can pick a different "style" of effects every time.
 */
public final class AnchorWindup {
    private static final int DURATION = 60;
    private static boolean active = false;
    private static long start = 0L;
    private static long clock = 0L;
    private static long seed = 0L;
    private static double targetX;
    private static double targetY;
    private static double targetZ;

    public static void clientTick() {
        clock++;
        if (active && clock - start >= DURATION) active = false;
    }

    public static void start(double x, double y, double z) {
        active = true;
        start = clock;
        seed = ThreadLocalRandom.current().nextLong();
        targetX = x;
        targetY = y;
        targetZ = z;
    }

    public static boolean isActive() {
        return active;
    }

    public static long seed() {
        return seed;
    }

    public static int ticksSinceStart() {
        return active ? (int) (clock - start) : -1;
    }

    /** 0 at the start of the windup, 1 when the teleport fires. */
    public static float progress() {
        if (!active) return 0.0F;
        return Math.min(1.0F, (clock - start) / (float) DURATION);
    }

    public static double targetX() {
        return targetX;
    }

    public static double targetY() {
        return targetY;
    }

    public static double targetZ() {
        return targetZ;
    }

    private AnchorWindup() { }
}
