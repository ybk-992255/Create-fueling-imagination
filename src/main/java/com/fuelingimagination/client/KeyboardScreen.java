package com.fuelingimagination.client;

import com.fuelingimagination.content.plugin.WirelessTypewriter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Read-only key map of the hand-held wireless typewriter. */
public class KeyboardScreen extends Screen {
    private final ItemStack source;

    public KeyboardScreen(ItemStack source) {
        super(Component.empty());
        this.source = source;
    }

    private ItemStack current() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null) return this.source;
        ItemStack held = com.fuelingimagination.event.PluginRedstoneEvents.held(minecraft.player);
        return held.isEmpty() ? this.source : held;
    }

    private String lastKey;
    private long lastClick;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean showAll = Screen.hasShiftDown();
        int cell = showAll ? 34 : 22;
        int gap = showAll ? 10 : 2;
        int rows = PluginRedstoneClientEvents.ROWS.size();
        int totalH = rows * cell + (rows - 1) * gap;
        int top = this.height / 2 - totalH / 2;
        int x0 = this.width / 2 - 10 * cell / 2;
        for (int row = 0; row < rows; row++) {
            String keys = PluginRedstoneClientEvents.ROWS.get(row);
            int offset = (10 - keys.length()) * cell / 2;
            for (int i = 0; i < keys.length(); i++) {
                String name = String.valueOf(keys.charAt(i));
                int x = x0 + offset + i * cell;
                int y = top + row * (cell + gap);
                if (mouseX >= x && mouseX < x + cell - 1 && mouseY >= y && mouseY < y + cell - 1) {
                    long now = System.currentTimeMillis();
                    if (name.equals(this.lastKey) && now - this.lastClick < 400L) {
                        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
                        if (minecraft.player != null) {
                            minecraft.player.connection.sendCommand("cgf redstone clearkey " + name);
                        }
                        this.lastKey = null;
                        return true;
                    }
                    this.lastKey = name;
                    this.lastClick = now;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

        private static ItemStack stack(String id) {
        if (id == null || id.isEmpty()) return ItemStack.EMPTY;
        var item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        boolean showAll = Screen.hasShiftDown();
        int cell = showAll ? 34 : 22;
        int gap = showAll ? 10 : 2;
        int rows = PluginRedstoneClientEvents.ROWS.size();
        int totalH = rows * cell + (rows - 1) * gap;
        int top = this.height / 2 - totalH / 2;
        int x0 = this.width / 2 - 10 * cell / 2;
        String hovered = null;

        for (int row = 0; row < rows; row++) {
            String keys = PluginRedstoneClientEvents.ROWS.get(row);
            int offset = (10 - keys.length()) * cell / 2;
            for (int i = 0; i < keys.length(); i++) {
                String name = String.valueOf(keys.charAt(i));
                int x = x0 + offset + i * cell;
                int y = top + row * (cell + gap);
                String[] freq = WirelessTypewriter.keyFrequency(current(), name);
                boolean bound = freq != null;
                int border = bound ? 0xFF4CD964 : 0xFF555555;
                graphics.fill(x, y, x + cell - 1, y + cell - 1, bound ? 0xAA2E5E33 : 0xAA222222);
                graphics.fill(x, y, x + cell - 1, y + 1, border);
                graphics.fill(x, y + cell - 2, x + cell - 1, y + cell - 1, border);
                graphics.fill(x, y, x + 1, y + cell - 1, border);
                graphics.fill(x + cell - 2, y, x + cell - 1, y + cell - 1, border);
                graphics.drawString(this.font, name, x + 3, y + 3, bound ? 0xFFFFFF : 0x888888, false);
                if (showAll && bound) {
                    ItemStack first = stack(freq[0]);
                    ItemStack second = stack(freq[1]);
                    int ix = x + 2;
                    if (!first.isEmpty()) {
                        graphics.renderItem(first, ix, y + 14);
                        ix += 16;
                    }
                    if (!second.isEmpty()) {
                        graphics.renderItem(second, ix, y + 14);
                    }
                }
                if (mouseX >= x && mouseX < x + cell - 1 && mouseY >= y && mouseY < y + cell - 1) {
                    hovered = name;
                }
            }
        }

        if (hovered != null) {
            String[] freq = WirelessTypewriter.keyFrequency(current(), hovered);
            String text = freq == null ? hovered + ": 未绑定"
                    : hovered + ": " + stack(freq[0]).getHoverName().getString()
                    + (stack(freq[1]).isEmpty() ? "" : " + " + stack(freq[1]).getHoverName().getString());
            graphics.drawString(this.font, text, 8, this.height - 20, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}