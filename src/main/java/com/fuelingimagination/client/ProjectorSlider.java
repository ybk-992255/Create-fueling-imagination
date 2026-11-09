package com.fuelingimagination.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

/** One labelled value slider on the projector editor. The caption shows the property name only. */
public class ProjectorSlider extends AbstractSliderButton {
    private final Component label;
    private final double min;
    private final double max;
    private final java.util.function.DoubleConsumer applier;

    public ProjectorSlider(int x, int y, int width, int height, Component label,
                           double min, double max, double value,
                           java.util.function.DoubleConsumer applier) {
        super(x, y, width, height, Component.empty(), (value - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.applier = applier;
        updateMessage();
    }

    public double current() {
        return min + value * (max - min);
    }

    @Override
    protected void updateMessage() {
        setMessage(label);
    }

    @Override
    protected void applyValue() {
        applier.accept(current());
    }
}