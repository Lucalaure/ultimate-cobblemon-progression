package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.Pending;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.PickButton;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.TrainerGroup;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildTrainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** The wild trainers of each region, beaten or not, in lists that fold away. */
final class TrainersTab extends CardTab {
    private static final int TRAINER_ROW = 12;

    /** Whether beaten trainers are hidden, and which regions' lists are folded away. Kept between openings. */
    private static boolean hideBeaten;
    private static final Set<String> collapsedTrainerGroups = new HashSet<>();

    /** The "Hide beaten" button, in content coordinates, or null. */
    private PickButton hideBeatenToggle;
    /** The region headers, which fold their list, in content coordinates; the pick's id is the region title. */
    private final List<PickButton> trainerHeaders = new ArrayList<>();

    TrainersTab(TrainerCardScreen card) {
        super(card);
    }

    /** The lists run to hundreds of rows, so the tab scrolls three rows at a time. */
    @Override
    int scrollStep() {
        return TRAINER_ROW * 3;
    }

    @Override
    boolean click(double x, double cy) {
        if (hideBeatenToggle != null && hideBeatenToggle.contains(x, cy)) {
            hideBeaten = !hideBeaten;
            card.clickSound();
            return true;
        }
        for (PickButton b : trainerHeaders) {
            if (b.contains(x, cy)) {
                if (!collapsedTrainerGroups.remove(b.pick().id())) collapsedTrainerGroups.add(b.pick().id());
                card.clickSound();
                return true;
            }
        }
        return false;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my) {
        Font font = p.font();
        trainerHeaders.clear();
        int y = CY0 + 2;
        int contentMouseY = contentMouseY(my);
        boolean mouseInView = mouseInView(mx, my);
        int x0 = CX0 + 2, x1 = CX1 - 6;

        // Title, with the button that hides beaten trainers
        String label = hideBeaten ? "Show beaten" : "Hide beaten";
        int bw = p.boldWidth(label) + 12;
        int bx1 = x1, bx0 = bx1 - bw, by0 = y, by1 = y + 13;
        boolean hover = mouseInView && mx >= bx0 && mx < bx1 && contentMouseY >= by0 && contentMouseY < by1;
        g.fill(bx0, by0, bx1, by1, FRAME_DARK);
        g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? RED_HOVER : RED);
        p.fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, bw - 6, WHITE, 1, Align.CENTRE);
        hideBeatenToggle = new PickButton(bx0, by0, bx1, by1, null);
        p.fit(g, Component.literal("Wild Trainers").withStyle(ChatFormatting.BOLD), x0 + 2, y + 3, bx0 - 8 - (x0 + 2), INK, 1, Align.LEFT);
        y += 17;

        int columns = 2;
        int cellW = (x1 - x0) / columns;
        for (TrainerGroup group : data().wildTrainers()) {
            // Region header: click to fold its list away
            boolean collapsed = collapsedTrainerGroups.contains(group.title());
            boolean headerHover = mouseInView && mx >= x0 && mx < x1 && contentMouseY >= y && contentMouseY < y + 13;
            g.fill(x0, y, x1, y + 13, headerHover ? MID : DARK);
            String count = group.beaten() + " / " + group.trainers().size() + " beaten";
            float countW = font.width(count) * 0.75f;
            p.text(g, Component.literal(count), x1 - 5 - countW, y + 4.5f, LIGHT, 0.75f, false);
            float titleW = p.fit(g, Component.literal((collapsed ? "▶ " : "▼ ") + group.title()).withStyle(ChatFormatting.BOLD),
                    x0 + 5, y + 3, x1 - 11 - countW - (x0 + 5), WHITE, 1, Align.LEFT);
            int barX0 = Math.max(x0 + 90, (int) (x0 + 5 + titleW + 8)), barX1 = (int) (x1 - 11 - countW);
            p.bar(g, barX0, y + 4, barX1, y + 9, group.trainers().isEmpty() ? 0 : group.beaten() / (float) group.trainers().size(), GREEN);
            trainerHeaders.add(new PickButton(x0, y, x1, y + 13, new Pending(group.title(), group.title(), 0)));
            y += 15;
            if (collapsed) continue;

            List<WildTrainer> shown = hideBeaten
                    ? group.trainers().stream().filter(t -> !t.beaten()).toList()
                    : group.trainers();
            if (shown.isEmpty()) {
                String none = group.trainers().isEmpty() ? "None listed" : "Every trainer here is beaten!";
                p.text(g, Component.literal(none), x0 + 5, y + 2, MUTED, 1, false);
                y += 15;
                continue;
            }
            int rows = (shown.size() + columns - 1) / columns;
            for (int i = 0; i < shown.size(); i++) {
                int row = i / columns, col = i % columns;
                int cy = y + row * TRAINER_ROW;
                // Only rows inside the view are drawn: a region lists hundreds of trainers.
                if (cy + TRAINER_ROW < CY0 + card.scroll || cy > CY1 + card.scroll) continue;
                WildTrainer t = shown.get(i);
                int cx = x0 + col * cellW, cx1 = cx + cellW - (col == columns - 1 ? 0 : 2);
                g.fill(cx, cy, cx1, cy + TRAINER_ROW - 1, row % 2 == 0 ? LIGHT : SCREEN);
                g.fill(cx + 2, cy + 1, cx + 11, cy + 10, t.beaten() ? GREEN : GREY);
                p.text(g, Component.literal(t.beaten() ? "✔" : "•"), cx + 6.5f, cy + 2, WHITE, 1, true);
                String lv = t.minLevel() == t.maxLevel() ? "Lv " + t.minLevel() : "Lv " + t.minLevel() + "–" + t.maxLevel();
                float lvW = font.width(lv) * 0.75f;
                p.text(g, Component.literal(lv), cx1 - 3 - lvW, cy + 3.5f, MUTED, 0.75f, false);
                p.fit(g, Component.literal(t.name()), cx + 14, cy + 2, cx1 - 6 - lvW - (cx + 14), t.beaten() ? INK : MUTED, 1, Align.LEFT);
            }
            y += rows * TRAINER_ROW + 4;
        }
        y += p.paragraph(g, Component.literal("Wild trainers you beat count here, weakest first. A trainer you can meet again "
                        + "at higher levels counts once, whichever time you beat them. Wins from before this list was added "
                        + "don't count: beat those trainers again when you meet them."),
                CX0 + 4, y + 1, CX1 - CX0 - 14, 40, MUTED, 0.75f, Align.LEFT) + 3;
        card.contentHeight = y - CY0 + 2;
    }
}
