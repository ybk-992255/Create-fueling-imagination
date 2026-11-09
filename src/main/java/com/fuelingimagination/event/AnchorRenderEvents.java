package com.fuelingimagination.event;

import com.fuelingimagination.FuelingImagination;
import com.fuelingimagination.client.AnchorWindup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Anchor windup visuals, randomly assembled per windup. A colour wash and vignette are always
 * present; scanlines, glitch bars, a border and corner marks are drawn at random, and the FOV and
 * camera may also be disturbed. All effects fade out as the teleport approaches.
 */
@Mod.EventBusSubscriber(modid = FuelingImagination.MOD_ID, value = Dist.CLIENT)
public final class AnchorRenderEvents {
    private static final int[] PALETTE = {0x00E5FF, 0xB388FF, 0xFF4081, 0xFFEB3B, 0x69F0AE, 0xFFAB40};

    private AnchorRenderEvents() { }

    private static int color(Random random) {
        return PALETTE[random.nextInt(PALETTE.length)];
    }

    @SubscribeEvent
    public static void onGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (!AnchorWindup.isActive()) return;
        Random random = new Random(AnchorWindup.seed());
        int rgb = color(random);
        float progress = AnchorWindup.progress();
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        // Always: colour wash that pulses while it brightens with progress.
        float pulse = (float) (0.6D + 0.4D * Math.sin(progress * Math.PI * 3.0D));
        int alpha = (int) (progress * pulse * 0x60);
        graphics.fill(0, 0, width, height, (alpha << 24) | rgb);

        // Always: vignette that darkens the edges.
        int edge = (int) (progress * 0x90);
        int band = Math.max(16, width / 5);
        graphics.fillGradient(0, 0, width, band, edge << 24, 0x00000000);
        graphics.fillGradient(0, height - band, width, height, 0x00000000, edge << 24);

        // Random extras: one to three of the following.
        int extra = 1 + random.nextInt(3);
        for (int i = 0; i < extra; i++) {
            switch (random.nextInt(4)) {
                case 0 -> scanlines(graphics, width, height, rgb, progress);
                case 1 -> glitchBars(graphics, width, height, rgb, progress, random);
                case 2 -> border(graphics, width, height, rgb, progress);
                default -> corners(graphics, width, height, rgb, progress, random);
            }
        }
    }

    private static void scanlines(GuiGraphics graphics, int width, int height, int rgb, float progress) {
        int line = (int) (progress * 0x55);
        int lineColor = (line << 24) | rgb;
        for (int y = 0; y < height; y += 8) graphics.fill(0, y, width, y + 1, lineColor);
    }

    private static void glitchBars(GuiGraphics graphics, int width, int height, int rgb, float progress, Random random) {
        int bars = 2 + random.nextInt(4);
        int alpha = (int) (progress * 0x70);
        int color = (alpha << 24) | rgb;
        for (int i = 0; i < bars; i++) {
            int y = random.nextInt(height);
            int h = 2 + random.nextInt(10);
            graphics.fill(0, y, width, Math.min(height, y + h), color);
        }
    }

    private static void border(GuiGraphics graphics, int width, int height, int rgb, float progress) {
        int alpha = (int) (progress * 0xA0);
        int color = (alpha << 24) | rgb;
        int t = 2;
        graphics.fill(0, 0, width, t, color);
        graphics.fill(0, height - t, width, height, color);
        graphics.fill(0, 0, t, height, color);
        graphics.fill(width - t, 0, width, height, color);
    }

    private static void corners(GuiGraphics graphics, int width, int height, int rgb, float progress, Random random) {
        int alpha = (int) (progress * 0xB0);
        int color = (alpha << 24) | rgb;
        int size = 10 + random.nextInt(20);
        graphics.fill(0, 0, size, size, color);
        graphics.fill(width - size, 0, width, size, color);
        graphics.fill(0, height - size, size, height, color);
        graphics.fill(width - size, height - size, width, height, color);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!AnchorWindup.isActive()) return;
        Random random = new Random(AnchorWindup.seed());
        if (random.nextInt(3) == 0) return;
        float progress = AnchorWindup.progress();
        float squeeze = 1.0F - 0.45F * progress;
        event.setFOV(event.getFOV() * squeeze);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!AnchorWindup.isActive()) return;
        Random random = new Random(AnchorWindup.seed());
        if (random.nextInt(3) == 0) return;
        float progress = AnchorWindup.progress();
        float shake = progress * 1.8F;
        event.setYaw(event.getYaw() + (float) (Math.sin(progress * 80.0D) * shake));
        event.setPitch(event.getPitch() + (float) (Math.cos(progress * 65.0D) * shake * 0.6F));
    }
}
