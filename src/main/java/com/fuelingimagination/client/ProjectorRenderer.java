package com.fuelingimagination.client;

import com.fuelingimagination.block.ProjectorBlock;
import com.fuelingimagination.block.entity.ProjectorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the caption in front of the projector.
 *
 * <p>A {@code holographic} projector billboards its caption so it always faces the viewer. The
 * plain variant walks the blocks along its facing and lays the caption on the near face of the
 * first one that is not see-through, falling back to a floating plane when the path is clear.
 *
 * <p>Caption size is a unit multiplier applied together with the final distance, so the apparent
 * size from the lens stays the same no matter where it lands.
 */
public class ProjectorRenderer implements BlockEntityRenderer<ProjectorBlockEntity> {
    /** World scale per (unit size x block of distance). 1 unit at 2.4 blocks is about 0.03. */
    private static final float SCALE_PER_BLOCK = 0.0125F;
    private static final float MAX_RENDER_SCALE = 0.5F;
    /** Lifted off the wall face so the caption never z-fights with the block it landed on. */
    private static final double SURFACE_OFFSET = 0.05D;
    private static final double HALF_WIDTH = 0.9D;
    private static final double HALF_HEIGHT = 0.55D;

    public ProjectorRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(ProjectorBlockEntity projector, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if (!projector.getBlockState().getValue(ProjectorBlock.ACTIVE)) return;
        String text = projector.getText();
        if (text == null || text.isEmpty()) return;

        Direction facing = projector.getBlockState().getValue(ProjectorBlock.FACING);
        boolean holographic = ProjectorBlock.isHologram(projector.getBlockState());
        Level level = projector.getLevel();
        Minecraft minecraft = Minecraft.getInstance();

        int rgb = Mth.hsvToRgb(projector.getHue() / 360.0F, 0.45F, 1.0F);
        int alpha = (int) (projector.getOpacity() * 255.0F) & 0xFF;
        int argb = (alpha << 24) | (rgb & 0xFFFFFF);

        Vec3 lens = new Vec3(0.5D + facing.getStepX() * 0.3D, 1.35D, 0.5D + facing.getStepZ() * 0.3D);
        double reach = projector.getDistance();

        Vec3 anchor = lens.add(facing.getStepX() * reach, 0.0D, facing.getStepZ() * reach);
        Direction surface = null;
        double travelled = reach;

        if (!holographic && level != null) {
            // Walk the blocks straight ahead; the first one that is not empty stops the projection.
            BlockPos cursor = projector.getBlockPos();
            int limit = Math.max(1, (int) Math.ceil(reach));
            for (int step = 1; step <= limit; step++) {
                cursor = cursor.relative(facing);
                BlockState state = level.getBlockState(cursor);
                if (false) {
                    // Near face of that block, nudged out towards the lens.
                    double faceX = cursor.getX() + 0.5D + facing.getStepX() * (-0.5D - SURFACE_OFFSET);
                    double faceZ = cursor.getZ() + 0.5D + facing.getStepZ() * (-0.5D - SURFACE_OFFSET);
                    anchor = new Vec3(faceX, lens.y, faceZ);
                    surface = facing.getOpposite();
                    travelled = Math.max(0.1D, lens.distanceTo(anchor));
                    break;
                }
            }
        }

        float scale = Math.min(MAX_RENDER_SCALE,
                projector.getUnitSize() * (float) travelled * SCALE_PER_BLOCK);

        Direction right = facing.getClockWise();
        Vec3 beamStart = lens.add(0.0D, 1.0D / 16.0D, 0.0D);
        Vec3 beamEnd = beamStart.add(facing.getStepX() * (1.3D + 1.0D / 16.0D), 0.0D,
                facing.getStepZ() * (1.3D + 1.0D / 16.0D));
        renderBeam(pose, buffers, beamStart,
                beamEnd.add(right.getStepX() * HALF_WIDTH, 0.0D, right.getStepZ() * HALF_WIDTH),
                beamEnd.add(-right.getStepX() * HALF_WIDTH, 0.0D, -right.getStepZ() * HALF_WIDTH),
                rgb, alpha);

        pose.pushPose();
        pose.translate(anchor.x, anchor.y, anchor.z);
        if (holographic) {
            pose.mulPose(minecraft.gameRenderer.getMainCamera().rotation());
        } else if (surface != null) {
            pose.mulPose(Axis.YP.rotationDegrees(-surface.toYRot()));
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        }

        String[] lines = text.split("\n", 8);
        Font font = minecraft.font;
        float lineHeight = font.lineHeight + 2.0F;
        // Anchor the block at its bottom edge so scaling grows upwards from the base line.
        float top = -(lines.length) * lineHeight;

        pose.scale(-scale, -scale, scale);
        for (int i = 0; i < lines.length; i++) {
            float halfWidth = font.width(lines[i]) / 2.0F;
            font.drawInBatch(lines[i], -halfWidth, top + i * lineHeight, argb, false,
                    pose.last().pose(), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, 0xF000F0);
        }
        pose.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource source) source.endBatch();
    }

    /** Anything that blocks sight for projection purposes: has a collision box and is not air. */
    private static boolean isSolid(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getFluidState().isEmpty() && level.isEmptyBlock(pos)) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }
    private void renderBeam(PoseStack pose, MultiBufferSource buffers, Vec3 lens,
                            Vec3 edgeA, Vec3 edgeB, int rgb, int alpha) {
        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float near = Math.min(0.30F, alpha / 255.0F * 0.32F);
        float far = Math.min(0.05F, alpha / 255.0F * 0.05F);
        var m = pose.last().pose();
        Vec3 up = new Vec3(0.0D, HALF_HEIGHT * 0.7D, 0.0D);
        Vec3[] ring = {edgeA.add(up), edgeB.add(up), edgeB.subtract(up), edgeA.subtract(up)};
        for (int i = 0; i < 4; i++) {
            Vec3 a = ring[i];
            Vec3 c = ring[(i + 1) % 4];
            buffer.vertex(m, (float) lens.x, (float) lens.y, (float) lens.z)
                    .color(r, g, b, near).endVertex();
            buffer.vertex(m, (float) a.x, (float) a.y, (float) a.z)
                    .color(r, g, b, far).endVertex();
            buffer.vertex(m, (float) c.x, (float) c.y, (float) c.z)
                    .color(r, g, b, far).endVertex();
        }
    }
}
