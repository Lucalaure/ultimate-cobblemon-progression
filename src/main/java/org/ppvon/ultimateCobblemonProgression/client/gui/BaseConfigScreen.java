package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseConfigScreen extends Screen {
    protected final Screen parent;
    protected Button saveButton;

    protected BaseConfigScreen(Screen parent, String title) {
        super(Component.literal(title));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height - 28;

        saveButton = addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            applyAndSave();
            onClose();
        }).bounds(cx - 155, y, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(cx + 5, y, 150, 20).build());
    }

    /** A scrolling list for the screen's settings, between the title and the Save and Cancel buttons. */
    protected org.ppvon.ultimateCobblemonProgression.client.gui.widgets.ConfigList addSettingsList() {
        return addRenderableWidget(new org.ppvon.ultimateCobblemonProgression.client.gui.widgets.ConfigList(
                this.minecraft, this.width, this.height - 32 - 36, 32));
    }

    protected void setDoneEnabled(boolean enabled) {
        if (saveButton != null) {
            saveButton.active = enabled;
        }
    }

    protected abstract void applyAndSave();

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gfx, int mouseX, int mouseY, float delta) {
        renderBackground(gfx, mouseX, mouseY, delta);
        super.render(gfx, mouseX, mouseY, delta);
        gfx.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
    }
}
