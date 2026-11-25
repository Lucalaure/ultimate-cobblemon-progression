package org.ppvon.ultimateCobblemonProgression.client.card;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.Pending;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen.PickButton;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;

import java.util.ArrayList;
import java.util.List;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/**
 * The pages the card shows instead of its tabs: the region choice (where the journey starts, and where it goes next)
 * and the gym difficulty choice. Buttons are in card coordinates: these pages don't scroll.
 */
final class ChoicePages {
    private final TrainerCardScreen card;
    private final CardPainter p;

    /** "Start here" / "Travel here" buttons on the region choice. */
    private final List<PickButton> choiceButtons = new ArrayList<>();
    /** The region choice's "Not now" button (next region only), or null. */
    private PickButton laterButton;
    /** The player put off picking their next region, so the card shows its tabs for now. */
    private boolean choiceDismissed;
    /** "Choose Normal" / "Choose Hard" buttons on the difficulty choice. */
    private final List<PickButton> difficultyButtons = new ArrayList<>();

    ChoicePages(TrainerCardScreen card) {
        this.card = card;
        this.p = card.painter();
    }

    /**
     * Whether the card opens on the region choice: always until the player picks where their journey starts, and
     * when they can pick where it goes next, unless they put that off.
     */
    boolean showingRegionChoice() {
        TrainerCardData data = card.data();
        return data.needsChoice() && !(data.choosingNext() && choiceDismissed);
    }

    /** Whether the card opens on the difficulty choice: once the journey has started, until a difficulty is picked. */
    boolean showingDifficultyChoice() {
        return !showingRegionChoice() && card.data().choosingDifficulty();
    }

    /** Whether one of the pages is showing instead of the tabs. */
    boolean showing() {
        return showingRegionChoice() || showingDifficultyChoice();
    }

    void draw(GuiGraphics g, int mx, int my) {
        if (showingRegionChoice()) drawRegionChoice(g, mx, my);
        else drawDifficultyChoice(g, mx, my);
    }

    /** A click on the page showing, in card coordinates. The pages have nothing else to click. */
    void click(double x, double y) {
        if (showingRegionChoice()) {
            for (PickButton b : choiceButtons) {
                if (b.contains(x, y)) {
                    card.confirming = b.pick();
                    card.clickSound();
                }
            }
            if (laterButton != null && laterButton.contains(x, y)) {
                choiceDismissed = true;
                card.clickSound();
            }
            return;
        }
        for (PickButton b : difficultyButtons) {
            if (b.contains(x, y)) {
                // No confirmation: the difficulty can be changed again at any time.
                ClientPlayNetworking.send(new PickRegionPayload(b.pick().id(), PickRegionPayload.DIFFICULTY));
                card.clickSound();
            }
        }
    }

    /**
     * Shown instead of the card until the player picks the region their journey starts in, and again each time
     * they complete their journey so far and can pick the region it continues in.
     */
    private void drawRegionChoice(GuiGraphics g, int mx, int my) {
        TrainerCardData data = card.data();
        choiceButtons.clear();
        laterButton = null;
        boolean next = data.choosingNext();
        p.text(g, Component.literal(next ? "CHOOSE YOUR NEXT REGION" : "CHOOSE YOUR STARTING REGION").withStyle(ChatFormatting.BOLD),
                W / 2f, 22, INK, 1, true);
        p.text(g, Component.literal(next ? "Where does your journey go next?" : "Where does your journey begin?"), W / 2f, 34, MUTED, 1, true);

        List<TrainerCardData.RegionChoice> choices = data.regionChoices();
        int panelW = 96, gap = 8, panelH = 108;
        int shown = Math.min(choices.size(), 3);
        int rowW = shown * panelW + (shown - 1) * gap;
        int x0 = (W - rowW) / 2, y0 = 48;
        for (int i = 0; i < shown; i++) {
            TrainerCardData.RegionChoice c = choices.get(i);
            int px0 = x0 + i * (panelW + gap), px1 = px0 + panelW, py1 = y0 + panelH;
            g.fill(px0, y0, px1, py1, FRAME_DARK);
            g.fill(px0 + 1, y0 + 1, px1 - 1, py1 - 1, LIGHT);
            g.fill(px0 + 1, y0 + 1, px1 - 1, y0 + 19, DARK);
            p.fit(g, Component.literal(c.title()).withStyle(ChatFormatting.BOLD), (px0 + px1) / 2f, y0 + 4, panelW - 8, WHITE, 1.25f, Align.CENTRE);

            int ty = y0 + 26, textW = panelW - 12;
            p.fit(g, Component.literal(c.trainers() + " trainers to beat"), px0 + 6, ty, textW, INK, 0.75f, Align.LEFT);
            p.fit(g, Component.literal(c.species() + " species"), px0 + 6, ty + 10, textW, INK, 0.75f, Align.LEFT);
            p.fit(g, Component.literal("First leader:"), px0 + 6, ty + 24, textW, MUTED, 0.75f, Align.LEFT);
            p.paragraph(g, c.firstTrainer().copy().withStyle(ChatFormatting.BOLD), px0 + 6, ty + 33, textW, py1 - 26 - (ty + 33),
                    INK, 0.85f, Align.LEFT);

            int bx0 = px0 + 8, bx1 = px1 - 8, by0 = py1 - 22, by1 = py1 - 8;
            boolean hover = card.confirming == null && mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? RED_HOVER : RED);
            p.fit(g, Component.literal(next ? "Travel here" : "Start here").withStyle(ChatFormatting.BOLD),
                    (bx0 + bx1) / 2f, by0 + 3, bx1 - bx0 - 6, WHITE, 1, Align.CENTRE);
            choiceButtons.add(new PickButton(bx0, by0, bx1, by1,
                    new Pending(c.id(), c.title(), next ? PickRegionPayload.NEXT : PickRegionPayload.HOME)));
        }

        int fy = y0 + panelH + 8;
        if (next) {
            // "Not now": look at the card first; it asks again the next time it's opened.
            String label = "Not now";
            int bw = p.boldWidth(label) + 16;
            int bx0 = W - 22 - bw, bx1 = W - 22, by0 = fy - 2, by1 = fy + 12;
            boolean hover = card.confirming == null && mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? LIGHT : MID);
            p.fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, bw - 6, hover ? INK : WHITE, 1, Align.CENTRE);
            laterButton = new PickButton(bx0, by0, bx1, by1, null);
        }
        // Narrower beside "Not now", so it can't run into the button.
        p.paragraph(g, Component.literal(next
                        ? "You join the region's trainer series, and your level cap starts over with its first leader."
                        : "Later, you can also pick an optional second region to explore, on the card's Regions tab."),
                W / 2f, fy, (next ? W - 130 : W - 80) * 0.75f, H - 12 - fy, MUTED, 0.75f, Align.CENTRE);
    }

    private void drawDifficultyChoice(GuiGraphics g, int mx, int my) {
        difficultyButtons.clear();
        p.text(g, Component.literal("CHOOSE YOUR CHALLENGE").withStyle(ChatFormatting.BOLD), W / 2f, 22, INK, 1, true);
        p.text(g, Component.literal("How strong should gym leaders be?"), W / 2f, 34, MUTED, 1, true);

        record Option(String id, String title, String body, int colour) {}
        List<Option> options = List.of(
                new Option("normal", "Normal", "Leaders as in the original games: their usual teams, with IVs that rise through your journey and no EVs.", DARK),
                new Option("hard", "Hard", "Full-strength teams at the same levels: extra Pokémon, perfect IVs, trained EVs, held items and a sharper AI.", RED));
        int panelW = 132, gap = 10, panelH = 108;
        int x0 = (W - (2 * panelW + gap)) / 2, y0 = 48;
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            int px0 = x0 + i * (panelW + gap), px1 = px0 + panelW, py1 = y0 + panelH;
            g.fill(px0, y0, px1, py1, FRAME_DARK);
            g.fill(px0 + 1, y0 + 1, px1 - 1, py1 - 1, LIGHT);
            g.fill(px0 + 1, y0 + 1, px1 - 1, y0 + 19, o.colour());
            p.fit(g, Component.literal(o.title()).withStyle(ChatFormatting.BOLD), (px0 + px1) / 2f, y0 + 4, panelW - 8, WHITE, 1.25f, Align.CENTRE);
            p.paragraph(g, Component.literal(o.body()), px0 + 6, y0 + 25, panelW - 12, py1 - 26 - (y0 + 25), INK, 0.85f, Align.LEFT);

            int bx0 = px0 + 8, bx1 = px1 - 8, by0 = py1 - 22, by1 = py1 - 8;
            boolean hover = mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? RED_HOVER : RED);
            p.fit(g, Component.literal("Choose " + o.title()).withStyle(ChatFormatting.BOLD),
                    (bx0 + bx1) / 2f, by0 + 3, bx1 - bx0 - 6, WHITE, 1, Align.CENTRE);
            difficultyButtons.add(new PickButton(bx0, by0, bx1, by1, new Pending(o.id(), o.title(), PickRegionPayload.DIFFICULTY)));
        }
        int fy = y0 + panelH + 8;
        p.paragraph(g, Component.literal("You can switch at any time on the Road tab. It applies from the next leader a gym summons."),
                W / 2f, fy, (W - 80) * 0.75f, H - 12 - fy, MUTED, 0.75f, Align.CENTRE);
    }
}
