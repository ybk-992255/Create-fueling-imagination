package com.fuelingimagination.client;

import com.fuelingimagination.content.plugin.CommandPlugins;
import com.fuelingimagination.event.PluginTitleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Type the title text, choose title / subtitle / self-visible; pre-fills with the last saved values. */
public class TitleScreen extends Screen {
    private final ItemStack source;
    private EditBox textField;
    private boolean main;
    private boolean sub;
    private boolean self;

    public TitleScreen(ItemStack source) {
        super(Component.literal("标题内容"));
        this.source = source;
        boolean saved = CommandPlugins.hasPluginKey(source, PluginTitleEvents.KEY_MAIN);
        this.main = saved ? CommandPlugins.getPluginInt(source, PluginTitleEvents.KEY_MAIN) != 0 : true;
        this.sub = saved ? CommandPlugins.getPluginInt(source, PluginTitleEvents.KEY_SUB) != 0 : true;
        this.self = saved && CommandPlugins.getPluginInt(source, PluginTitleEvents.KEY_SELF) != 0;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 50;
        this.textField = new EditBox(this.font, cx - 110, top, 220, 18, Component.empty());
        this.textField.setMaxLength(200);
        this.textField.setValue(CommandPlugins.getPluginString(this.source, PluginTitleEvents.KEY_TEXT));
        this.addRenderableWidget(this.textField);
        this.addRenderableWidget(Button.builder(mainLabel(), b -> {
            this.main = !this.main;
            b.setMessage(mainLabel());
        }).bounds(cx - 110, top + 26, 70, 18).build());
        this.addRenderableWidget(Button.builder(subLabel(), b -> {
            this.sub = !this.sub;
            b.setMessage(subLabel());
        }).bounds(cx - 36, top + 26, 70, 18).build());
        this.addRenderableWidget(Button.builder(selfLabel(), b -> {
            this.self = !this.self;
            b.setMessage(selfLabel());
        }).bounds(cx + 38, top + 26, 72, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("保存"), b -> save())
                .bounds(cx - 110, top + 50, 220, 18).build());
    }

    private Component mainLabel() {
        return Component.literal("大标题:" + (this.main ? "开" : "关"));
    }

    private Component subLabel() {
        return Component.literal("小标题:" + (this.sub ? "开" : "关"));
    }

    private Component selfLabel() {
        return Component.literal("自己:" + (this.self ? "开" : "关"));
    }

    private void save() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        String text = this.textField.getValue().trim();
        minecraft.player.connection.sendCommand("cgf title set "
                + (this.main ? 1 : 0) + " " + (this.sub ? 1 : 0) + " " + (this.self ? 1 : 0) + " " + text);
        minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 70, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
