package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.Pending;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.PickButton;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RegionEntry;

import java.util.ArrayList;
import java.util.List;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** Every region's progress, with "Pick" buttons for the optional second region. */
final class RegionsTab extends CardTab {
    /** "Pick" buttons, in content coordinates. */
    private final List<PickButton> pickButtons = new ArrayList<>();

    RegionsTab(TrainerCardScreen card) {
        super(card);
    }

    @Override
    boolean click(double x, double cy) {
        for (PickButton b : pickButtons) {
            if (b.contains(x, cy)) {
                card.confirming = b.pick();
                card.clickSound();
                return true;
            }
        }
        return false;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my) {
        TrainerCardData data = data();
        Font font = p.font();
        pickButtons.clear();
        int y = CY0 + 2;
        int contentMouseY = contentMouseY(my);
        boolean mouseInView = mouseInView(mx, my);

        if (data.canPickStart()) {
            String first = data.regionTitle();
            int bx0 = CX0 + 2, bx1 = CX1 - 6;
            List<FormattedCharSequence> lines = font.split(Component.literal("Pick a second region to work through alongside "
                    + first + ": another league with its own gym leaders, or a region without gyms that grows with your badges. "
                    + "Its Pokémon can spawn for you right away. You only get one pick."), bx1 - bx0 - 12);
            int by1 = y + 16 + lines.size() * 10;
            g.fill(bx0, y, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, y + 1, bx1 - 1, by1 - 1, DARK);
            p.text(g, Component.literal("CHOOSE YOUR SECOND REGION").withStyle(ChatFormatting.BOLD), bx0 + 6, y + 5, GOLD, 0.75f, false);
            int ly = y + 14;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, bx0 + 6, ly, WHITE, false);
                ly += 10;
            }
            y = by1 + 4;
        }

        for (RegionEntry r : data.regions()) {
            int rx0 = CX0 + 2, rx1 = CX1 - 6, ry1 = y + 18;
            boolean locked = r.state() == TrainerCardData.LOCKED;
            p.panel(g, rx0, y, rx1, ry1);
            if (locked && !r.choosable()) g.fill(rx0 + 1, y + 1, rx1 - 1, ry1 - 1, SCREEN);

            MutableComponent title = Component.literal(r.title()).withStyle(ChatFormatting.BOLD);
            if (r.startPick()) title.append(Component.literal(" ★").withStyle(s -> s.withColor(GOLD)));
            int titleColour = locked && !r.choosable() ? GREY : INK;

            int rightEdge;
            if (r.choosable()) {
                // A "Pick" button instead of the region's state
                String label = "Pick";
                int bw = p.boldWidth(label) + 16;
                int bx1 = rx1 - 3, bx0 = bx1 - bw, by0 = y + 3, by1 = ry1 - 3;
                boolean hover = mouseInView && mx >= bx0 && mx < bx1 && contentMouseY >= by0 && contentMouseY < by1;
                g.fill(bx0, by0, bx1, by1, FRAME_DARK);
                g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? RED_HOVER : RED);
                p.fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, bw - 6, WHITE, 1, Align.CENTRE);
                pickButtons.add(new PickButton(bx0, by0, bx1, by1, new Pending(r.id(), r.title(), PickRegionPayload.EXTRA)));
                rightEdge = bx0 - 6;
                String note = "second region";
                float noteW = font.width(note) * 0.75f;
                p.text(g, Component.literal(note), rightEdge - noteW, y + 6.5f, MUTED, 0.75f, false);
                p.fit(g, title, rx0 + 6, y + 5, rightEdge - noteW - 6 - (rx0 + 6), titleColour, 1, Align.LEFT);
            } else {
                String state;
                int stateColour;
                if (locked) {
                    state = "Locked";
                    stateColour = GREY;
                } else if (r.state() == TrainerCardData.COMPLETE) {
                    state = "Complete";
                    stateColour = GOLD;
                } else {
                    state = "Tier " + r.tier() + " / " + r.maxTier() + (r.toCatch() > 0 ? "  ·  " + r.toCatch() + " to catch" : "");
                    stateColour = INK;
                }
                // The state gets up to half the row; the title the rest, with the progress bar in any space between.
                float stateW = p.fit(g, Component.literal(state), rx1 - 6, y + 5, (rx1 - rx0) / 2f, stateColour, 1, Align.RIGHT);
                int stateX0 = (int) (rx1 - 6 - stateW);
                float titleW = p.fit(g, title, rx0 + 6, y + 5, stateX0 - 8 - (rx0 + 6), titleColour, 1, Align.LEFT);
                if (!locked) {
                    float fraction = r.state() == TrainerCardData.COMPLETE ? 1 : r.maxTier() == 0 ? 0 : r.tier() / (float) r.maxTier();
                    p.bar(g, Math.max(rx0 + 80, (int) (rx0 + 6 + titleW + 8)), y + 7, stateX0 - 8, y + 12, fraction,
                            r.state() == TrainerCardData.COMPLETE ? GOLD : DARK);
                }
            }
            y += 21;
        }
        if (data.regions().stream().anyMatch(RegionEntry::startPick)) {
            p.text(g, Component.literal("★ your starting pick"), CX0 + 4, y + 2, MUTED, 0.75f, false);
            y += 10;
        }
        card.contentHeight = y - CY0 + 2;
    }
}
