package com.fuelingimagination.client;

import com.fuelingimagination.content.upgrade.UpgradeData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

/**
 * Renders the powered multi-tool from three sub-models so each group can animate independently:
 * the body stays still, the gear (chilun) spins on the X axis and the saw blade (jvpian) bobs up
 * and down, the latter only while the saw accessory is installed.
 */
public final class PoweredSwordRenderer extends BlockEntityWithoutLevelRenderer {
    public static final PoweredSwordRenderer INSTANCE = new PoweredSwordRenderer();

    private static final ResourceLocation BODY = new ResourceLocation("fuelingimagination", "item/powered_sword_body");
    private static final ResourceLocation CHILUN = new ResourceLocation("fuelingimagination", "item/powered_sword_chilun");
    private static final ResourceLocation JVPIAN = new ResourceLocation("fuelingimagination", "item/powered_sword_jvpian");

    private final ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
    private static boolean loggedOnce = false;

    private PoweredSwordRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    private BakedModel model(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        if (!loggedOnce) {
            loggedOnce = true;
            com.fuelingimagination.FuelingImagination.LOGGER.info("[cgf] powered_sword renderByItem called");
        }
        pose.pushPose();

        // Base transform from the body model, then move into model space.
        ForgeHooksClient.handleCameraTransforms(pose, model(BODY), context, false);
        pose.translate(-0.5F, -0.5F, -0.5F);

        renderModel(model(BODY), stack, pose, buffer, light, overlay);

        float time = System.currentTimeMillis() / 1000.0F;

        // Gear: constant rotation around X.
        pose.pushPose();
        pose.mulPose(Axis.XP.rotation(time * 1.5F));
        renderModel(model(CHILUN), stack, pose, buffer, light, overlay);
        pose.popPose();

        // Saw blade: only with the saw accessory, bobbing in Y between -0.5 and -2.5 (pixels).
        if (UpgradeData.hasComponent(stack, "create:mechanical_saw")) {
            pose.pushPose();
            pose.translate(0.0F, (-1.5F + Mth.sin(time * 3.0F) * 1.0F) / 16.0F, 0.0F);
            renderModel(model(JVPIAN), stack, pose, buffer, light, overlay);
            pose.popPose();
        }

        pose.popPose();
    }

    private void renderModel(BakedModel model, ItemStack stack, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType type : pass.getRenderTypes(stack, true)) {
                VertexConsumer consumer = buffer.getBuffer(type);
                itemRenderer.renderModelLists(pass, stack, light, overlay, pose, consumer);
            }
        }
    }
}
