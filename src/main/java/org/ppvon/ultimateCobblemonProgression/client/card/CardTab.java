package org.ppvon.ultimateCobblemonProgression.client.card;

import net.minecraft.client.gui.GuiGraphics;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/**
 * One tab of the Trainer Card. Scrolling tabs draw in content coordinates: card coordinates, shifted up by the
 * screen's scroll. They record their buttons as they draw, and the screen passes clicks inside the content area on.
 */
abstract class CardTab {
    final TrainerCardScreen card;
    final CardPainter p;

    CardTab(TrainerCardScreen card) {
        this.card = card;
        this.p = card.painter();
    }

    TrainerCardData data() {
        return card.data();
    }

    /** Draws the tab; mx and my are the mouse in card coordinates. A scrolling tab sets the card's content height. */
    abstract void draw(GuiGraphics g, int mx, int my);

    /** Whether the tab scrolls inside the content area (and is clipped to it). */
    boolean scrolls() {
        return true;
    }

    /** How far one turn of the mouse wheel scrolls. */
    int scrollStep() {
        return 14;
    }

    /** A click at x (card coordinates) and cy (content coordinates) inside the content area; returns whether it was used. */
    boolean click(double x, double cy) {
        return false;
    }

    /** The mouse's y in content coordinates. */
    int contentMouseY(int my) {
        return my + (int) card.scroll;
    }

    /** Whether the mouse is inside the content area (and no confirmation box is in the way). */
    boolean mouseInView(int mx, int my) {
        return card.confirming == null && my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;
    }
}
