package com.fuelingimagination.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;

/** Pick up to two vanilla potion effects for the command block's effect plugin. */
public class EffectSelectScreen extends Screen {
    private static final int ROWS = 12;
    private static final int ROW_HEIGHT = 14;
    private static final String EMPTY = "minecraft:empty";

    private final List<ResourceLocation> effects = new ArrayList<>();
    private final List<ResourceLocation> selected = new ArrayList<>();
    private int scroll = 0;

    public EffectSelectScreen() {
        super(Component.literal("选择药水效果"));
        // Vanilla, non-instant effects only: instant ones have no meaningful 5 second duration.
        for (var key : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (!"minecraft".equals(key.getNamespace())) continue;
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(key);
            if (effect == null || effect.isInstantenous()) continue;
            this.effects.add(key);
        }
        this.effects.sort(java.util.Comparator.comparing(ResourceLocation::toString));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int top = this.height / 2 - 74;
        this.addRenderableWidget(Button.builder(Component.literal("确认"), b -> confirm())
                .bounds(cx - 40, top + ROWS * ROW_HEIGHT + 8, 80, 18).build());
    }

    private void confirm() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        String a = this.selected.size() > 0 ? this.selected.get(0).toString() : EMPTY;
        String b = this.selected.size() > 1 ? this.selected.get(1).toString() : EMPTY;
        minecraft.player.connection.sendCommand("cgf effect set " + a + " " + b);
        minecraft.setScreen(null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int max = Math.max(0, this.effects.size() - ROWS);
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        int top = this.height / 2 - 74;
        int row = (int) ((mouseY - top) / ROW_HEIGHT);
        if (mouseX >= cx - 110 && mouseX <= cx + 110 && mouseY >= top && row >= 0 && row < ROWS) {
            int index = this.scroll + row;
            if (index < this.effects.size()) {
                ResourceLocation id = this.effects.get(index);
                if (this.selected.contains(id)) this.selected.remove(id);
                else if (this.selected.size() < 2) this.selected.add(id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int top = this.height / 2 - 74;
        graphics.drawCenteredString(this.font, this.title, cx, top - 16, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("已选 " + this.selected.size() + " / 2"), cx, top - 4, 0xAAAAAA);
        for (int i = 0; i < ROWS; i++) {
            int index = this.scroll + i;
            if (index >= this.effects.size()) break;
            ResourceLocation id = this.effects.get(index);
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
            boolean sel = this.selected.contains(id);
            String name = effect == null ? id.toString() : effect.getDisplayName().getString();
            int y = top + i * ROW_HEIGHT;
            graphics.fill(cx - 110, y, cx + 110, y + ROW_HEIGHT - 1, sel ? 0x70FFD700 : 0x40000000);
            graphics.drawString(this.font, name, cx - 106, y + 3, sel ? 0xFFD700 : 0xCCCCCC);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
