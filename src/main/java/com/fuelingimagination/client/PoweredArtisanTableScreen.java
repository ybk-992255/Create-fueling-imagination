package com.fuelingimagination.client;

import com.fuelingimagination.content.artisan.ArtisanProcessStage;
import com.fuelingimagination.content.artisan.ArtisanWorkMode;
import com.fuelingimagination.content.damaged.DamagedBlocks;
import com.fuelingimagination.menu.ArtisanMenuLayout;
import com.fuelingimagination.menu.PoweredArtisanTableMenu;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PoweredArtisanTableScreen extends AbstractContainerScreen<PoweredArtisanTableMenu> {
   private static final ResourceLocation TEXTURE = new ResourceLocation("fuelingimagination", "textures/gui/powered_artisan_table.png");
   private static final int SHIFT_X = 137;
   private static final int CRANK_X = 11;
   private static final int CRANK_Y = 98;
   private static final ItemStack SPONGE = new ItemStack(Items.SPONGE);
   private PoweredArtisanTableScreen.Drag drag = PoweredArtisanTableScreen.Drag.NONE;
   private double dragStart;
   private double lastAngle;
   private double crankTravel;
   private double wipeX;
   private double wipeY;
   private float shiftOffset;
   private float leverOffset;
   private float crankAngle;
   private long lastFrame;
   private long lastWipe;
   private int selectedPart = -1;
   private ArtisanWorkMode lastMode;

   public PoweredArtisanTableScreen(PoweredArtisanTableMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 280;
      this.imageHeight = 238;
   }

   protected void init() {
      super.init();
      this.leftPos += 8;
      this.drag = PoweredArtisanTableScreen.Drag.NONE;
      this.shiftOffset = this.leverOffset = this.crankAngle = 0.0F;
      this.selectedPart = -1;
      this.lastMode = ((PoweredArtisanTableMenu)this.menu).getWorkMode();
      this.lastFrame = Util.getMillis();
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      long now = Util.getMillis();
      float spring = (float)Math.exp(-18.0 * (double)Math.min(now - this.lastFrame, 100L) / 1000.0);
      this.lastFrame = now;
      if (this.drag != PoweredArtisanTableScreen.Drag.SHIFT) {
         this.shiftOffset *= spring;
      }

      if (this.drag != PoweredArtisanTableScreen.Drag.LEVER) {
         this.leverOffset *= spring;
      }

      if (this.drag != PoweredArtisanTableScreen.Drag.CRANK && ((PoweredArtisanTableMenu)this.menu).getProcessStage() == ArtisanProcessStage.IDLE) {
         this.crankAngle *= spring;
      }

      if (this.lastMode != ((PoweredArtisanTableMenu)this.menu).getWorkMode()) {
         this.drag = PoweredArtisanTableScreen.Drag.NONE;
         this.selectedPart = -1;
         this.crankAngle = 0.0F;
         this.lastMode = ((PoweredArtisanTableMenu)this.menu).getWorkMode();
      }

      if (!((PoweredArtisanTableMenu)this.menu).isRepairDisassembled()
         || ((PoweredArtisanTableMenu)this.menu).getProcessStage() == ArtisanProcessStage.REPAIR_READY) {
         this.selectedPart = -1;
      }

      this.renderBackground(graphics);
      super.render(graphics, mouseX, mouseY, partialTick);
      this.renderTooltip(graphics, mouseX, mouseY);
      String hint = null;
      if (this.inside((double)mouseX, (double)mouseY, 111.0, 0.0, 58, 26)) {
         hint = "shift_hint";
      } else if (this.overCrank((double)mouseX, (double)mouseY)) {
         hint = ((PoweredArtisanTableMenu)this.menu).getWorkMode() == ArtisanWorkMode.MANUFACTURE ? "use_lever" : "crank_hint";
      } else if (this.inside((double)mouseX, (double)mouseY, 243.0, 72.0, 22, 62)) {
         hint = ((PoweredArtisanTableMenu)this.menu).getWorkMode() == ArtisanWorkMode.REPAIR ? "use_crank" : "lever_hint";
      }

      if (((PoweredArtisanTableMenu)this.menu).isCrafting() && this.inside((double)mouseX, (double)mouseY, 243.0, 72.0, 22, 62)) {
         hint = "batch_stop_hint";
      }

      if (hint != null) {
         graphics.renderTooltip(this.font, this.tr(hint), mouseX, mouseY);
      }

      if (this.selectedPart >= 0 && (this.drag == PoweredArtisanTableScreen.Drag.SPONGE || this.inside((double)mouseX, (double)mouseY, 157.0, 78.0, 54, 46))) {
         graphics.pose().pushPose();
         graphics.pose().translate(0.0F, 0.0F, 400.0F);
         graphics.renderItem(SPONGE, mouseX - 8, mouseY - 8);
         graphics.pose().popPose();
      }
   }

   protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
      this.region(graphics, 111, 1, 58, 24, 219, 99, 72, 28);
      this.region(graphics, 137 + Math.round(this.shiftOffset), 0, 6, 26, 294, 97, 7, 32);
      this.region(graphics, 44, 27, 190, 124, 135, 130, 238, 155);
      this.region(graphics, 56, 151, 168, 57, 163, 288, 186, 63);
      this.region(graphics, 50, 208, 180, 30, 156, 351, 200, 33);
      if (((PoweredArtisanTableMenu)this.menu).getWorkMode() != ArtisanWorkMode.MANUFACTURE
         && !((PoweredArtisanTableMenu)this.menu).isRepairDisassembled()) {
         if (((PoweredArtisanTableMenu)this.menu).getWorkMode() == ArtisanWorkMode.MODIFY) {
            for (int i = 0; i < 8; i++) {
               int x = ArtisanMenuLayout.RING[i][0];
               int y = ArtisanMenuLayout.RING[i][1];
               if (i < ((PoweredArtisanTableMenu)this.menu).getOpenSlots()) {
                  this.region(graphics, x - 7, y - 4, 32, 26, 466, 332, 40, 33);
               } else {
                  this.region(graphics, x - 3, y - 4, 22, 26, 295, 408, 26, 32);
               }
            }
         }
      } else {
         this.region(graphics, 65, 61, 72, 65, 430, 189, 80, 72);
      }

      if (((PoweredArtisanTableMenu)this.menu).getWorkMode() == ArtisanWorkMode.MANUFACTURE) {
         this.region(graphics, 173, 119, 22, 22, 189, 207, 28, 28);
      }

      this.region(graphics, -8, 79, 38, 38, 61, 194, 48, 48);
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + 11), (float)(this.topPos + 98), 0.0F);
      graphics.pose().mulPose(Axis.ZP.rotation(this.crankAngle));
      graphics.blit(TEXTURE, -11, -8, 47, 16, 69.0F, 246.0F, 59, 20, 512, 512);
      graphics.pose().popPose();
      this.region(graphics, 244, 91, 19, 29, 372, 207, 24, 36);
      graphics.pose().pushPose();
      graphics.pose().translate((float)(this.leftPos + 261), (float)(this.topPos + 108) + this.leverOffset, 0.0F);
      graphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
      graphics.blit(TEXTURE, 0, 0, 16, 33, 404.0F, 223.0F, 20, 41, 512, 512);
      graphics.pose().popPose();
   }

   private void region(GuiGraphics graphics, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight) {
      graphics.blit(TEXTURE, this.leftPos + x, this.topPos + y, width, height, (float)u, (float)v, sourceWidth, sourceHeight, 512, 512);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
      Component modeTitle = ((PoweredArtisanTableMenu)this.menu).getWorkMode().title();
      float titleScale = Math.min(1.0F, 108.0F / (float)Math.max(1, this.font.width(modeTitle)));
      graphics.pose().pushPose();
      graphics.pose().translate(92.0F, (float)(35 - Math.round(9.0F / 5.0F)), 0.0F);
      graphics.pose().scale(titleScale, titleScale, 1.0F);
      graphics.drawString(this.font, modeTitle, 0, 0, ((PoweredArtisanTableMenu)this.menu).getWorkMode().textColor(), false);
      graphics.pose().popPose();
      switch (((PoweredArtisanTableMenu)this.menu).getWorkMode()) {
         case MANUFACTURE:
            this.renderCraftingInfo(graphics);
            break;
         case MODIFY:
            if (!((PoweredArtisanTableMenu)this.menu).getMainItem().isEmpty()) {
               List<Component> lines = ((PoweredArtisanTableMenu)this.menu).describeMainItem();
               if (lines.isEmpty()) {
                  this.paragraph(graphics, this.tr("modification_slots_pending"), 158, 64, 54, 5849648, 6);
               } else {
                  int y = 63;

                  for (Component line : lines) {
                     y = this.paragraph(graphics, line, 158, y, 54, 5849648, Math.max(0, (133 - y) / 10));
                  }
               }
            }
            break;
         case REPAIR:
            this.renderRepairInfo(graphics);
      }
   }

   private void renderCraftingInfo(GuiGraphics graphics) {
      this.drawFitted(graphics, this.tr("preview"), 184, 63, 55, 5849648);
      ItemStack preview = ((PoweredArtisanTableMenu)this.menu).getCraftingPreview();
      if (!preview.isEmpty()) {
         graphics.renderItem(preview, 176, 77);
         graphics.renderItemDecorations(this.font, preview, 176, 77);
      } else {
         this.drawFitted(graphics, this.tr("no_match"), 184, 80, 55, 6968645);
      }

      this.drawFitted(graphics, this.tr(((PoweredArtisanTableMenu)this.menu).isCrafting() ? "batch_running" : "use_lever"), 184, 99, 55, 5849648);
      this.drawFitted(graphics, this.tr("output"), 184, 110, 55, 5849648);
   }

   private void renderRepairInfo(GuiGraphics graphics) {
      if (!((PoweredArtisanTableMenu)this.menu).isRepairDisassembled()) {
         if (((PoweredArtisanTableMenu)this.menu).getMainItem().isEmpty()) {
            this.paragraph(graphics, this.tr("repair_insert"), 158, 65, 54, 5849648, 6);
         } else {
            ItemStack stack = ((PoweredArtisanTableMenu)this.menu).getMainItem();
            if (stack.getItem() instanceof BlockItem) {
               boolean damaged = DamagedBlocks.isDamaged(stack);
               this.paragraph(graphics, this.tr(damaged ? "repair_block_damaged" : "repair_block_intact"), 158, 64, 54, 5849648, 4);
               this.paragraph(graphics, this.tr(damaged ? "repair_disassemble" : "repair_undamaged"), 158, 100, 54, 3179339, 4);
            } else {
               this.paragraph(
                  graphics,
                  Component.translatable("gui.fuelingimagination.artisan.durability", new Object[]{stack.getMaxDamage() - stack.getDamageValue(), stack.getMaxDamage()}),
                  158,
                  64,
                  54,
                  5849648,
                  3
               );
               this.paragraph(graphics, this.tr(stack.isDamaged() ? "repair_disassemble" : "repair_undamaged"), 158, 100, 54, 3179339, 4);
            }
         }
      } else {
         for (int i = 0; i < 9; i++) {
            int x = 75 + i % 3 * 18;
            int y = 65 + i / 3 * 18;
            if (((PoweredArtisanTableMenu)this.menu).isBrokenPart(i)) {
               this.broken(graphics, x, y, 16);
            } else if ((((PoweredArtisanTableMenu)this.menu).getDirtyParts() & 1 << i) != 0) {
               this.dirt(graphics, x, y, 16, i, 3);
            }
         }

         if (this.selectedPart >= 0 && !((PoweredArtisanTableMenu)this.menu).getRepairPart(this.selectedPart).isEmpty()) {
            boolean brokenPart = ((PoweredArtisanTableMenu)this.menu).isBrokenPart(this.selectedPart);
            this.drawFitted(
               graphics,
               this.tr(
                  brokenPart
                     ? "part_broken"
                     : ((((PoweredArtisanTableMenu)this.menu).getDirtyParts() & 1 << this.selectedPart) == 0 ? "clean_done" : "clean_part")
               ),
               184,
               64,
               55,
               5849648
            );
            float previewScale = brokenPart ? 1.6F : 2.5F;
            graphics.pose().pushPose();
            graphics.pose().translate(brokenPart ? 168.0F : 164.0F, brokenPart ? 78.0F : 80.0F, 0.0F);
            graphics.pose().scale(previewScale, previewScale, 1.0F);
            graphics.renderItem(((PoweredArtisanTableMenu)this.menu).getRepairPart(this.selectedPart), 0, 0);
            graphics.pose().popPose();
            if (brokenPart) {
               int expectedId = ((PoweredArtisanTableMenu)this.menu).getExpectedPartId(this.selectedPart);
               Component need = (Component)(expectedId >= 0
                  ? Component.translatable("gui.fuelingimagination.artisan.part_need", new Object[]{new ItemStack(Item.byId(expectedId)).getHoverName()})
                  : this.tr("part_expect"));
               this.paragraph(graphics, need, 158, 104, 54, 9124410, 2);
            } else {
               int remaining = (((PoweredArtisanTableMenu)this.menu).getDirtyParts() & 1 << this.selectedPart) == 0
                  ? 0
                  : 6 - ((PoweredArtisanTableMenu)this.menu).getCleanProgress(this.selectedPart);
               this.dirt(graphics, 164, 80, 40, this.selectedPart, remaining);
               graphics.fill(158, 125, 210, 128, -9743552);
               graphics.fill(158, 125, 158 + (6 - remaining) * 52 / 6, 128, -9853876);
            }
         } else {
            boolean anyBroken = ((PoweredArtisanTableMenu)this.menu).countBrokenParts() != 0;
            this.paragraph(
               graphics,
               this.tr(
                  ((PoweredArtisanTableMenu)this.menu).getProcessStage() == ArtisanProcessStage.REPAIR_READY
                     ? "repair_assemble"
                     : (anyBroken ? "repair_replace_hint" : "repair_clean")
               ),
               158,
               64,
               54,
               5849648,
               5
            );
            this.drawFitted(
               graphics,
               Component.translatable(
                  "gui.fuelingimagination.artisan.part_count",
                  new Object[]{((PoweredArtisanTableMenu)this.menu).countBrokenParts(), ((PoweredArtisanTableMenu)this.menu).countDirtyParts()}
               ),
               184,
               117,
               55,
               6965552
            );
         }

         graphics.fill(157, 132, 211, 143, -2007219406);
         this.drawFitted(graphics, this.tr("cancel_repair"), 184, 133, 50, 16773850);
      }
   }

   public void onClose() {
      if (((PoweredArtisanTableMenu)this.menu).isRepairDisassembled() && ((PoweredArtisanTableMenu)this.menu).isAssemblyIncomplete()) {
         this.send(4);
      }

      super.onClose();
   }

   private void broken(GuiGraphics graphics, int x, int y, int size) {
      int color = -3123883;
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 250.0F);
      graphics.fill(x - 1, y - 1, x + size + 1, y, color);
      graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, color);
      graphics.fill(x - 1, y, x, y + size, color);
      graphics.fill(x + size, y, x + size + 1, y + size, color);
      graphics.pose().popPose();
   }

   private void dirt(GuiGraphics graphics, int x, int y, int size, int seed, int count) {
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, 300.0F);

      for (int i = 0; i < count; i++) {
         int dx = Math.floorMod(seed * 7 + i * 13 + 3, size - 4);
         int dy = Math.floorMod(seed * 11 + i * 7 + 5, size - 4);
         int stain = size > 16 ? 6 : 3;
         graphics.fill(x + dx, y + dy, x + dx + stain, y + dy + stain, -529775320);
      }

      graphics.pose().popPose();
   }

   private int paragraph(GuiGraphics graphics, Component text, int x, int y, int width, int color, int maxLines) {
      int lines = 0;

      for (FormattedCharSequence line : this.font.split(text, width)) {
         if (lines++ >= maxLines) {
            break;
         }

         graphics.drawString(this.font, line, x, y, color, false);
         y += 10;
      }

      return y;
   }

   private void drawFitted(GuiGraphics graphics, Component text, int centerX, int y, int maxWidth, int color) {
      float scale = Math.min(1.0F, (float)maxWidth / (float)Math.max(1, this.font.width(text)));
      graphics.pose().pushPose();
      graphics.pose().translate((float)centerX, (float)y, 0.0F);
      graphics.pose().scale(scale, scale, 1.0F);
      graphics.drawString(this.font, text, -this.font.width(text) / 2, 0, color, false);
      graphics.pose().popPose();
   }

   private Component tr(String key) {
      return Component.translatable("gui.fuelingimagination.artisan." + key);
   }

   private boolean inside(double mouseX, double mouseY, double x, double y, int width, int height) {
      return mouseX >= (double)this.leftPos + x
         && mouseX < (double)this.leftPos + x + (double)width
         && mouseY >= (double)this.topPos + y
         && mouseY < (double)this.topPos + y + (double)height;
   }

   private void send(int button) {
      if (this.minecraft != null && this.minecraft.gameMode != null) {
         this.minecraft.gameMode.handleInventoryButtonClick(((PoweredArtisanTableMenu)this.menu).containerId, button);
      }
   }

   private double angle(double x, double y) {
      return Math.atan2(y - (double)this.topPos - 98.0, x - (double)this.leftPos - 11.0);
   }

   private boolean overCrank(double x, double y) {
      double gripX = (double)(this.leftPos + 11) + Math.cos((double)this.crankAngle) * 28.0;
      double gripY = (double)(this.topPos + 98) + Math.sin((double)this.crankAngle) * 28.0;
      return this.inside(x, y, -8.0, 79.0, 38, 38) || Math.hypot(x - gripX, y - gripY) <= 11.0;
   }

   private boolean overPart(double mouseX, double mouseY, int part) {
      return this.inside(mouseX, mouseY, (double)(75 + part % 3 * 18), (double)(65 + part / 3 * 18), 16, 16);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 1 && ((PoweredArtisanTableMenu)this.menu).isRepairDisassembled()) {
         for (int i = 0; i < 9; i++) {
            if (!((PoweredArtisanTableMenu)this.menu).getRepairPart(i).isEmpty() && this.overPart(mouseX, mouseY, i)) {
               this.selectedPart = i;
               return true;
            }
         }
      }

      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      } else if (this.inside(mouseX, mouseY, (double)(137.0F + this.shiftOffset - 3.0F), 0.0, 12, 26)) {
         this.drag = PoweredArtisanTableScreen.Drag.SHIFT;
         this.dragStart = mouseX - (double)this.shiftOffset;
         return true;
      } else {
         if (((PoweredArtisanTableMenu)this.menu).isRepairDisassembled()) {
            if (this.inside(mouseX, mouseY, 157.0, 132.0, 54, 11)) {
               this.send(4);
               return true;
            }

            for (int ix = 0; ix < 9; ix++) {
               if (!((PoweredArtisanTableMenu)this.menu).getRepairPart(ix).isEmpty() && this.overPart(mouseX, mouseY, ix)) {
                  this.selectedPart = ix;
                  return super.mouseClicked(mouseX, mouseY, button);
               }
            }

            if (this.selectedPart >= 0 && this.inside(mouseX, mouseY, 157.0, 78.0, 54, 46)) {
               this.drag = PoweredArtisanTableScreen.Drag.SPONGE;
               this.wipeX = mouseX;
               this.wipeY = mouseY;
               this.lastWipe = 0L;
               return true;
            }
         }

         if (this.overCrank(mouseX, mouseY)) {
            if (((PoweredArtisanTableMenu)this.menu).getWorkMode() != ArtisanWorkMode.MANUFACTURE) {
               this.drag = PoweredArtisanTableScreen.Drag.CRANK;
               this.lastAngle = this.angle(mouseX, mouseY);
               this.crankTravel = 0.0;
            }

            return true;
         } else if (this.inside(mouseX, mouseY, 243.0, (double)(74.0F + this.leverOffset), 22, 36)
            && ((PoweredArtisanTableMenu)this.menu).getWorkMode() != ArtisanWorkMode.REPAIR) {
            this.drag = PoweredArtisanTableScreen.Drag.LEVER;
            this.dragStart = (double)this.leverOffset;
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (button == 0 && this.drag != PoweredArtisanTableScreen.Drag.NONE) {
         switch (this.drag) {
            case SHIFT:
               this.shiftOffset = Mth.clamp((float)(mouseX - this.dragStart), -18.0F, 18.0F);
               break;
            case LEVER:
               this.leverOffset = Mth.clamp((float)(mouseY - this.dragStart), 0.0F, 22.0F);
               break;
            case CRANK:
               double next = this.angle(mouseX, mouseY);
               double delta = next - this.lastAngle;
               if (delta > Math.PI) {
                  delta -= Math.PI * 2;
               }

               if (delta < -Math.PI) {
                  delta += Math.PI * 2;
               }

               this.crankAngle += (float)delta;
               this.crankTravel += delta;
               this.lastAngle = next;
               // One full turn counts as one operation, fired the moment the angle is reached.
               if (Math.abs(this.crankTravel) >= Math.PI * 2) {
                  this.send(2);
                  this.crankTravel -= Math.signum(this.crankTravel) * Math.PI * 2;
               }
               break;
            case SPONGE:
               if (this.selectedPart >= 0
                  && (((PoweredArtisanTableMenu)this.menu).getDirtyParts() & 1 << this.selectedPart) != 0
                  && this.inside(mouseX, mouseY, 157.0, 78.0, 54, 46)
                  && Math.hypot(mouseX - this.wipeX, mouseY - this.wipeY) >= 4.0
                  && Util.getMillis() - this.lastWipe >= 160L) {
                  this.send(32 + this.selectedPart);
                  this.lastWipe = Util.getMillis();
                  this.wipeX = mouseX;
                  this.wipeY = mouseY;
               }
         }

         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0 && this.drag != PoweredArtisanTableScreen.Drag.NONE) {
         switch (this.drag) {
            case SHIFT:
               this.shiftOffset = Mth.clamp((float)(mouseX - this.dragStart), -18.0F, 18.0F);
               if (Math.abs(this.shiftOffset) >= 7.0F) {
                  this.send(this.shiftOffset < 0.0F ? 0 : 1);
               }
               break;
            case LEVER:
               this.leverOffset = Mth.clamp((float)(mouseY - this.dragStart), 0.0F, 22.0F);
               if (this.leverOffset >= 12.0F) {
                  this.send(3);
               }
               break;
            case CRANK:
               // Handled while dragging; releasing no longer triggers anything.
               break;
         }

         this.drag = PoweredArtisanTableScreen.Drag.NONE;
         return true;
      } else {
         return super.mouseReleased(mouseX, mouseY, button);
      }
   }

   private static enum Drag {
      NONE,
      SHIFT,
      LEVER,
      CRANK,
      SPONGE;
   }
}
