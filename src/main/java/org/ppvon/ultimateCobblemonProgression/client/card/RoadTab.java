package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.Pending;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.PickButton;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RoadEntry;

import java.util.List;
import java.util.Locale;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** The region's trainers in order, with the gym difficulty and, with two leagues, a switch to the other league. */
final class RoadTab extends CardTab {
    /** Two leagues at once: whether the tab shows the other league. */
    boolean showOtherRoad;
    /** The switch between the two leagues' roads, in content coordinates, or null. */
    private PickButton roadToggle;
    /** The "Switch to ..." difficulty button, in content coordinates, or null. */
    private PickButton difficultyToggle;

    RoadTab(TrainerCardScreen card) {
        super(card);
    }

    @Override
    boolean click(double x, double cy) {
        if (roadToggle != null && roadToggle.contains(x, cy)) {
            showOtherRoad = !showOtherRoad;
            card.clickSound();
            return true;
        }
        if (difficultyToggle != null && difficultyToggle.contains(x, cy)) {
            card.confirming = difficultyToggle.pick();
            card.clickSound();
            return true;
        }
        return false;
    }

    @Override
    void draw(GuiGraphics g, int mx, int my) {
        TrainerCardData data = data();
        Font font = p.font();
        difficultyToggle = null;
        roadToggle = null;
        int y = CY0 + 2;
        // Two leagues at once: a switch between the focused league's road and the other league's.
        TrainerCardData.LeagueView otherLeague = data.otherLeagues().isEmpty() ? null : data.otherLeagues().get(0);
        if (otherLeague == null) showOtherRoad = false;
        String roadTitle = showOtherRoad ? otherLeague.title() : data.regionTitle();
        List<RoadEntry> road = showOtherRoad ? otherLeague.road() : data.road();
        long beaten = showOtherRoad ? otherLeague.beaten() : data.beaten();
        if (road.isEmpty()) {
            p.paragraph(g, Component.literal("No trainers are listed for " + roadTitle + "."), (CX0 + CX1) / 2f, y + 20,
                    CX1 - CX0 - 12, 30, MUTED, 1, Align.CENTRE);
            card.contentHeight = 0;
            return;
        }
        String summary = beaten + " / " + road.size() + " beaten";
        int titleEnd = CX1 - 14 - font.width(summary);
        if (otherLeague != null) {
            String label = "Show " + (showOtherRoad ? data.regionTitle() : otherLeague.title());
            int bw = Math.min(font.width(label) + 10, 110);
            int bx1 = CX1 - 6, bx0 = bx1 - bw, by0 = y - 2, by1 = y + 10;
            int contentMouseY = contentMouseY(my);
            boolean hover = card.confirming == null && my >= CY0 && my < CY1 && mx >= bx0 && mx < bx1
                    && contentMouseY >= by0 && contentMouseY < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? MID : DARK);
            p.fit(g, Component.literal(label), (bx0 + bx1) / 2f, by0 + 2, bw - 6, WHITE, 1, Align.CENTRE);
            roadToggle = new PickButton(bx0, by0, bx1, by1, null);
            g.drawString(font, summary, bx0 - 6 - font.width(summary), y, MUTED, false);
            titleEnd = bx0 - 12 - font.width(summary);
        } else {
            g.drawString(font, summary, CX1 - 8 - font.width(summary), y, MUTED, false);
        }
        p.fit(g, Component.literal(roadTitle + " Road").withStyle(ChatFormatting.BOLD), CX0 + 4, y,
                titleEnd - (CX0 + 4), INK, 1, Align.LEFT);
        y += 13;

        if (!data.difficulty().isEmpty()) {
            // Gym difficulty, with a button to switch it when players choose their own.
            int rx0 = CX0 + 2, rx1 = CX1 - 6, ry1 = y + 15;
            p.panel(g, rx0, y, rx1, ry1);
            // A region without Hard leaders always summons Normal ones, so there's nothing to switch.
            boolean hardHere = showOtherRoad ? otherLeague.hardAvailable() : data.hardAvailable();
            boolean hard = hardHere && data.difficulty().equals("Hard");
            int labelEnd = rx1 - 6;
            if (data.difficultyChangeable() && hardHere) {
                String other = hard ? "Normal" : "Hard";
                String label = "Switch to " + other;
                int bw = p.boldWidth(label) + 12;
                int bx1 = rx1 - 2, bx0 = bx1 - bw, by0 = y + 2, by1 = ry1 - 2;
                int contentMouseY = contentMouseY(my);
                boolean hover = card.confirming == null && my >= CY0 && my < CY1 && mx >= bx0 && mx < bx1
                        && contentMouseY >= by0 && contentMouseY < by1;
                g.fill(bx0, by0, bx1, by1, FRAME_DARK);
                g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? RED_HOVER : RED);
                p.fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 2, bw - 6, WHITE, 1, Align.CENTRE);
                difficultyToggle = new PickButton(bx0, by0, bx1, by1,
                        new Pending(other.toLowerCase(Locale.ROOT), other, PickRegionPayload.DIFFICULTY));
                labelEnd = bx0 - 6;
            }
            MutableComponent line = Component.literal("Gym leaders: ").append(Component.literal(hardHere ? data.difficulty() : "Normal")
                    .withStyle(s -> s.withBold(true).withColor(hard ? RED : INK)));
            if (!hardHere) line.append(Component.literal("  ·  no Hard leaders in " + roadTitle + " yet"));
            p.fit(g, line, rx0 + 6, y + 4, labelEnd - (rx0 + 6), MUTED, 1, Align.LEFT);
            y += 18;
        }

        int i = 0;
        for (RoadEntry r : road) {
            int rx0 = CX0 + 2, rx1 = CX1 - 6, ry1 = y + 13;
            if (r.status() == TrainerCardData.NEXT) {
                g.fill(rx0, y, rx1, ry1, RED);
                g.fill(rx0 + 1, y + 1, rx1 - 1, ry1 - 1, LIGHT);
            } else {
                g.fill(rx0, y, rx1, ry1, i % 2 == 0 ? LIGHT : SCREEN);
            }
            int chipColour = switch (r.status()) {
                case TrainerCardData.BEATEN -> GREEN;
                case TrainerCardData.NEXT -> RED;
                default -> GREY;
            };
            String chip = switch (r.status()) {
                case TrainerCardData.BEATEN -> "✔";
                case TrainerCardData.NEXT -> "▶";
                default -> "•";
            };
            g.fill(rx0 + 3, y + 2, rx0 + 12, y + 11, chipColour);
            p.text(g, Component.literal(chip), rx0 + 7.5f, y + 3, WHITE, 1, true);

            String meta = r.level() > 0 ? "Tier " + r.tier() + "  ·  Lv " + r.level() : "Tier " + r.tier();
            float metaW = font.width(meta) * 0.75f;
            p.text(g, Component.literal(meta), rx1 - 4 - metaW, y + 4.5f, MUTED, 0.75f, false);
            if (r.hard()) {
                // Beaten on Hard
                float tagW = font.width("HARD") * 0.75f + 6;
                float tx1 = rx1 - 8 - metaW, tx0 = tx1 - tagW;
                g.fill((int) tx0, y + 2, (int) tx1, y + 11, RED);
                p.text(g, Component.literal("HARD").withStyle(ChatFormatting.BOLD), (tx0 + tx1) / 2f, y + 4, WHITE, 0.75f, true);
                metaW += tagW + 4;
            }

            MutableComponent name = r.name().copy();
            if (r.status() == TrainerCardData.NEXT) name.withStyle(ChatFormatting.BOLD);
            p.fit(g, name, rx0 + 17, y + 3, rx1 - 10 - metaW - (rx0 + 17), r.status() == TrainerCardData.LATER ? MUTED : INK, 1, Align.LEFT);
            y += 14;
            i++;
        }
        card.contentHeight = y - CY0 + 2;
    }
}
