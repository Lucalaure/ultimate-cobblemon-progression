package org.ppvon.ultimateCobblemonProgression.client.gym;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.common.gym.RematchPayloads;

import java.util.List;

/**
 * A Gym Challenge block's rematch menu: every leader the player has beaten, a page at a time, each with a button for
 * its Normal version and (when it has one) its Hard version.
 */
public class RematchScreen extends Screen {
    private static final int ROW_H = 24, ROW_W = 320, BUTTON_W = 60;

    private final BlockPos gym;
    private final List<RematchPayloads.Entry> entries;
    private int page;

    public RematchScreen(BlockPos gym, List<RematchPayloads.Entry> entries) {
        super(Component.literal("Rematch a Gym Leader"));
        this.gym = gym;
        this.entries = entries;
    }

    private int perPage() {
        return Math.max(1, (height - 90) / ROW_H);
    }

    private int pages() {
        return Math.max(1, (entries.size() + perPage() - 1) / perPage());
    }

    @Override
    protected void init() {
        page = Math.min(page, pages() - 1);
        int x0 = (width - ROW_W) / 2, y = 40;
        int from = page * perPage(), to = Math.min(entries.size(), from + perPage());
        for (int i = from; i < to; i++) {
            RematchPayloads.Entry e = entries.get(i);
            int bx = x0 + ROW_W - BUTTON_W;
            if (!e.hardId().isEmpty()) {
                addRenderableWidget(Button.builder(Component.literal("Hard").withStyle(ChatFormatting.RED), b -> pick(e.hardId()))
                        .bounds(bx, y, BUTTON_W, 20).build());
                bx -= BUTTON_W + 4;
            }
            addRenderableWidget(Button.builder(Component.literal("Normal"), b -> pick(e.normalId()))
                    .bounds(bx, y, BUTTON_W, 20).build());
            y += ROW_H;
        }

        int by = height - 30;
        if (pages() > 1) {
            Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1)).bounds(width / 2 - 110, by, 20, 20).build());
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1)).bounds(width / 2 + 90, by, 20, 20).build());
            prev.active = page > 0;
            next.active = page < pages() - 1;
        }
        addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose()).bounds(width / 2 - 50, by, 100, 20).build());
    }

    private void turn(int by) {
        page = Math.max(0, Math.min(pages() - 1, page + by));
        rebuildWidgets();
    }

    private void pick(String trainerId) {
        ClientPlayNetworking.send(new RematchPayloads.Pick(gym, trainerId));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.literal("A rematch doesn't change your progress, and your level cap doesn't apply.")
                .withStyle(ChatFormatting.GRAY), width / 2, 26, 0xFFA0A0A0);

        int x0 = (width - ROW_W) / 2, y = 40;
        int from = page * perPage(), to = Math.min(entries.size(), from + perPage());
        for (int i = from; i < to; i++) {
            RematchPayloads.Entry e = entries.get(i);
            int buttons = e.hardId().isEmpty() ? BUTTON_W : 2 * BUTTON_W + 4;
            Component label = Component.literal(e.region() + "  ").withStyle(ChatFormatting.AQUA).append(e.name().copy().withStyle(ChatFormatting.WHITE));
            int maxW = ROW_W - buttons - 8;
            if (font.width(label) > maxW) {
                g.drawString(font, font.substrByWidth(label, maxW - font.width("…")).getString() + "…", x0, y + 6, 0xFFFFFFFF);
            } else {
                g.drawString(font, label, x0, y + 6, 0xFFFFFFFF);
            }
            y += ROW_H;
        }
        if (pages() > 1) {
            g.drawCenteredString(font, (page + 1) + " / " + pages(), width / 2, height - 44, 0xFFA0A0A0);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
