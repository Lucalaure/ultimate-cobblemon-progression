package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfigHubScreen extends BaseConfigScreen {
    public ConfigHubScreen(Screen parent) {
        super(parent, "CJP – Config");
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 4;

        addRenderableWidget(Button.builder(Component.literal("General"), b ->
                this.minecraft.setScreen(new CommonConfigScreen(this))
        ).bounds(cx - 100, y, 200, 20).build());

        y += 24;

        addRenderableWidget(Button.builder(Component.literal("Spawning"), b ->
                this.minecraft.setScreen(new SpawnConfigScreen(this))
        ).bounds(cx - 100, y, 200, 20).build());

        int btnY = this.height - 28;
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(cx - 50, btnY, 100, 20).build());

    }

    @Override
    protected void applyAndSave() {
        // Nothing to save at the hub level.
    }
}
