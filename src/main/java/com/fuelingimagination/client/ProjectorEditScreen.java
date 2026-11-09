package com.fuelingimagination.client;

import com.fuelingimagination.block.ProjectorBlock;
import com.fuelingimagination.block.entity.ProjectorBlockEntity;
import com.fuelingimagination.content.projector.ProjectorNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor for a projector caption. Four single-line boxes stand in for a multi-line field, and a
 * button cycles the presentation mode. Everything applies immediately; there is no save step.
 */
public class ProjectorEditScreen extends Screen {
    private static final int LINES = 4;
    private static final int FIELD_HEIGHT = 16;
    private static final int FIELD_GAP = 2;

    private final ProjectorBlockEntity projector;
    private final List<EditBox> fields = new ArrayList<>();
    private float unitSize;
    private float distance;
    private float hue;
    private float opacity;
    private boolean hologram;
    private String lastSent = "";

    public ProjectorEditScreen(ProjectorBlockEntity projector) {
        super(Component.translatable("gui.fuelingimagination.projector.title"));
        this.projector = projector;
        this.unitSize = projector.getUnitSize();
        this.distance = projector.getDistance();
        this.hue = projector.getHue();
        this.opacity = projector.getOpacity();
        this.hologram = ProjectorBlock.isHologram(projector.getBlockState());
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int sliderWidth = 220;
        int fieldTop = height / 2 - 86;

        String[] existing = projector.getText().split("\n", -1);
        for (int i = 0; i < LINES; i++) {
            EditBox field = new EditBox(font, cx - sliderWidth / 2,
                    fieldTop + i * (FIELD_HEIGHT + FIELD_GAP), sliderWidth, FIELD_HEIGHT,
                    Component.translatable("gui.fuelingimagination.projector.text"));
            field.setMaxLength(ProjectorBlockEntity.MAX_LENGTH / LINES);
            field.setValue(i < existing.length ? existing[i] : "");
            field.setResponder(text -> push());
            fields.add(field);
            addRenderableWidget(field);
        }
        setInitialFocus(fields.get(0));

        int sliderTop = fieldTop + LINES * (FIELD_HEIGHT + FIELD_GAP) + 6;
        addRenderableWidget(new ProjectorSlider(cx - sliderWidth / 2, sliderTop, sliderWidth, 20,
                Component.translatable("gui.fuelingimagination.projector.unit_size"),
                ProjectorBlockEntity.MIN_UNIT_SIZE, ProjectorBlockEntity.MAX_UNIT_SIZE, unitSize,
                v -> { unitSize = (float) v; push(); }));
        addRenderableWidget(new ProjectorSlider(cx - sliderWidth / 2, sliderTop + 22, sliderWidth, 20,
                Component.translatable("gui.fuelingimagination.projector.distance"),
                ProjectorBlockEntity.MIN_DISTANCE, ProjectorBlockEntity.MAX_DISTANCE, distance,
                v -> { distance = (float) v; push(); }));
        addRenderableWidget(new ProjectorSlider(cx - sliderWidth / 2, sliderTop + 44, sliderWidth, 20,
                Component.translatable("gui.fuelingimagination.projector.hue"),
                0.0D, 360.0D, hue,
                v -> { hue = (float) v; push(); }));
        addRenderableWidget(new ProjectorSlider(cx - sliderWidth / 2, sliderTop + 66, sliderWidth, 20,
                Component.translatable("gui.fuelingimagination.projector.opacity"),
                ProjectorBlockEntity.MIN_OPACITY, ProjectorBlockEntity.MAX_OPACITY, opacity,
                v -> { opacity = (float) v; push(); }));
        addRenderableWidget(Button.builder(modeLabel(), b -> {
            hologram = !hologram;
            b.setMessage(modeLabel());
            push();
        }).bounds(cx - sliderWidth / 2, sliderTop + 90, sliderWidth, 20).build());
    }

    private Component modeLabel() {
        return Component.translatable("gui.fuelingimagination.projector.mode",
                Component.translatable(hologram
                        ? "gui.fuelingimagination.projector.mode.hologram"
                        : "gui.fuelingimagination.projector.mode.shadow"));
    }

    private void push() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) text.append('\n');
            text.append(fields.get(i).getValue());
        }
        String joined = text.toString().replaceAll("\n+$", "");
        String key = joined + "|" + unitSize + "|" + distance + "|" + hue + "|" + opacity + "|" + hologram;
        if (key.equals(lastSent)) return;
        lastSent = key;
        ProjectorNetwork.send(projector.getBlockPos(), joined, unitSize, distance, hue, opacity, hologram);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 108, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}