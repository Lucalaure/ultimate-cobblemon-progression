package org.ppvon.ultimateCobblemonProgression.client.gym;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymChoice;

import java.util.List;

/**
 * Two leagues at once: a Gym Challenge block's menu for picking which league's leader to battle, one row per league
 * with the leader's name, strongest Pokémon's level and difficulty, and a Challenge button.
 */
public class GymChoiceScreen extends Screen {
    private static final int ROW_H = 24, ROW_W = 320, BUTTON_W = 80;

    private final BlockPos gym;
    private final List<GymChoice.Option> options;

    public GymChoiceScreen(BlockPos gym, List<GymChoice.Option> options) {
        super(Component.literal("Choose Your Challenge"));
        this.gym = gym;
        this.options = options;
    }

    @Override
    protected void init() {
        int x0 = (width - ROW_W) / 2, y = 44;
        for (GymChoice.Option o : options) {
            addRenderableWidget(Button.builder(Component.literal("Challenge"), b -> pick(o.trainerId()))
                    .bounds(x0 + ROW_W - BUTTON_W, y, BUTTON_W, 20).build());
            y += ROW_H;
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(width / 2 - 50, height - 30, 100, 20).build());
    }

    private void pick(String trainerId) {
        ClientPlayNetworking.send(new GymChoice.Pick(gym, trainerId));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("Both of your leagues have a gym leader ready. Which one do you want to battle?")
                .withStyle(ChatFormatting.GRAY), width / 2, 26, 0xFFA0A0A0);

        int x0 = (width - ROW_W) / 2, y = 44;
        int maxW = ROW_W - BUTTON_W - 8;
        for (GymChoice.Option o : options) {
            Component label = Component.literal(o.region() + "  ").withStyle(ChatFormatting.AQUA)
                    .append(o.name().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  Lv " + o.aceLevel()).withStyle(ChatFormatting.GRAY));
            if (o.hard()) label = label.copy().append(Component.literal("  Hard").withStyle(ChatFormatting.RED));
            if (font.width(label) > maxW) {
                g.drawString(font, font.substrByWidth(label, maxW - font.width("…")).getString() + "…", x0, y + 6, 0xFFFFFFFF);
            } else {
                g.drawString(font, label, x0, y + 6, 0xFFFFFFFF);
            }
            y += ROW_H;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
