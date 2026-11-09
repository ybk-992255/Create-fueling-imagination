package com.fuelingimagination.client;

import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.event.ModCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Three-field screen for the anchor plugin; pre-fills with whatever was saved last time. */
public class AnchorScreen extends Screen {
    private final ItemStack source;
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;

    public AnchorScreen(ItemStack source) {
        super(Component.literal("锚定坐标"));
        this.source = source;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 40;
        boolean saved = CommandPlugins.hasPluginKey(this.source, ModCommands.KEY_X);
        double x = saved ? CommandPlugins.getPluginDouble(this.source, ModCommands.KEY_X) : currentX();
        double y = saved ? CommandPlugins.getPluginDouble(this.source, ModCommands.KEY_Y) : currentY();
        double z = saved ? CommandPlugins.getPluginDouble(this.source, ModCommands.KEY_Z) : currentZ();
        this.xField = field(cx, top, String.format(java.util.Locale.ROOT, "%.1f", x));
        this.yField = field(cx, top + 24, String.format(java.util.Locale.ROOT, "%.1f", y));
        this.zField = field(cx, top + 48, String.format(java.util.Locale.ROOT, "%.1f", z));
        this.addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                .bounds(cx - 60, top + 76, 120, 18).build());
    }

    private double currentX() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0.0D : minecraft.player.getX();
    }

    private double currentY() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0.0D : minecraft.player.getY();
    }

    private double currentZ() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? 0.0D : minecraft.player.getZ();
    }

    private EditBox field(int cx, int y, String value) {
        EditBox box = new EditBox(this.font, cx - 60, y, 120, 18, Component.empty());
        box.setMaxLength(48);
        box.setValue(value);
        this.addRenderableWidget(box);
        return box;
    }

    private void save() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        try {
            double x = Double.parseDouble(this.xField.getValue().trim());
            double y = Double.parseDouble(this.yField.getValue().trim());
            double z = Double.parseDouble(this.zField.getValue().trim());
            minecraft.player.connection.sendCommand("cgf anchor save " + x + " " + y + " " + z);
            minecraft.setScreen(null);
        } catch (NumberFormatException ignored) {
            // Leave the screen open so the player can fix the number.
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
