package org.ppvon.ultimateCobblemonProgression.client.card;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.ppvon.ultimateCobblemonProgression.client.gui.UiText;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;

import java.util.List;
import java.util.function.Supplier;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/** Draws the Trainer Card's text, panels and bars: text shrinks to fit before it's cut short. */
final class CardPainter {
    /** The smallest text is shrunk to before it's cut short with "…". */
    private static final float MIN_SCALE = 0.65f;
    private static final int LINE_HEIGHT = 10;

    enum Align { LEFT, CENTRE, RIGHT }

    /** The screen's font, read when drawing: a screen only gets its font once it's shown. */
    private final Supplier<Font> font;

    CardPainter(Supplier<Font> font) {
        this.font = font;
    }

    Font font() {
        return font.get();
    }

    // Text --------------------------------------------------------------

    /** A button label's width: labels are bold, which is wider than plain text. */
    int boldWidth(String label) {
        return font().width(Component.literal(label).withStyle(ChatFormatting.BOLD));
    }

    /** Draws text at a scale, optionally centred on x. */
    void text(GuiGraphics g, Component text, float x, float y, int colour, float scale, boolean centred) {
        draw(g, text.getVisualOrderText(), x, y, colour, scale, centred ? Align.CENTRE : Align.LEFT);
    }

    /** Draws text at a scale, with x as its left edge, centre or right edge. */
    void draw(GuiGraphics g, FormattedCharSequence text, float x, float y, int colour, float scale, Align align) {
        float w = font().width(text) * scale;
        float dx = switch (align) {
            case LEFT -> 0;
            case CENTRE -> -w / 2;
            case RIGHT -> -w;
        };
        g.pose().pushPose();
        g.pose().translate(x + dx, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font(), text, 0, 0, colour, false);
        g.pose().popPose();
    }

    /**
     * Draws one line in at most maxWidth: at maxScale if it fits, otherwise shrunk until it does (down to
     * MIN_SCALE, staying centred on the full-size line), and only then cut short with "…". Returns the drawn width.
     */
    float fit(GuiGraphics g, Component text, float x, float y, float maxWidth, int colour, float maxScale, Align align) {
        Font font = font();
        int w = font.width(text);
        if (maxWidth <= 0 || w == 0) return 0;
        float scale = w * maxScale <= maxWidth ? maxScale : Math.max(Math.min(maxScale, MIN_SCALE), maxWidth / w);
        FormattedCharSequence line = w * scale <= maxWidth ? text.getVisualOrderText() : ellipsize(text, maxWidth / scale);
        draw(g, line, x, y + (maxScale - scale) * font.lineHeight / 2, colour, scale, align);
        return font.width(line) * scale;
    }

    /**
     * Draws wrapped text in a box maxWidth wide and maxHeight tall: at maxScale if every line fits, otherwise
     * shrunk until they do (down to MIN_SCALE), and only then with the last line that fits cut short with "…".
     * Returns the height used.
     */
    float paragraph(GuiGraphics g, Component text, float x, float y, float maxWidth, float maxHeight,
                    int colour, float maxScale, Align align) {
        float scale = maxScale;
        List<FormattedText> lines = wrap(text, maxWidth / scale);
        while (lines.size() * LINE_HEIGHT * scale > maxHeight && scale > MIN_SCALE) {
            scale = Math.max(MIN_SCALE, scale - 0.05f);
            lines = wrap(text, maxWidth / scale);
        }
        int fits = Math.max(1, (int) (maxHeight / (LINE_HEIGHT * scale)));
        float ly = y;
        for (int i = 0; i < Math.min(fits, lines.size()); i++) {
            boolean cut = i == fits - 1 && lines.size() > fits;
            FormattedCharSequence line = cut ? ellipsize(lines.get(i), maxWidth / scale)
                    : Language.getInstance().getVisualOrder(lines.get(i));
            draw(g, line, x, ly, colour, scale, align);
            ly += LINE_HEIGHT * scale;
        }
        return ly - y;
    }

    private List<FormattedText> wrap(Component text, float width) {
        return font().getSplitter().splitLines(text, Math.max(1, (int) width), Style.EMPTY);
    }

    /** The text cut to fit width (in unscaled font units), ending in "…". */
    private FormattedCharSequence ellipsize(FormattedText text, float width) {
        return UiText.ellipsize(font(), text, (int) width);
    }

    // Panels and bars ---------------------------------------------------

    /** A labelled row with a value on its right. */
    void row(GuiGraphics g, int x0, int x1, int y, String label, Component value, int valueColour) {
        panel(g, x0, y, x1, y + 15);
        text(g, Component.literal(label), x0 + 5, y + 4, MUTED, 1, false);
        fit(g, value, x1 - 5, y + 4, x1 - x0 - 18 - font().width(label), valueColour, 1, Align.RIGHT);
    }

    /** A labelled row with a progress bar and "value / max". */
    void barRow(GuiGraphics g, int x0, int x1, int y, String label, int value, int max, int colour) {
        Font font = font();
        panel(g, x0, y, x1, y + 15);
        text(g, Component.literal(label), x0 + 5, y + 4, MUTED, 1, false);
        String count = value + " / " + max;
        int countW = font.width(count);
        g.drawString(font, count, x1 - 5 - countW, y + 4, INK, false);
        int bx0 = x0 + 5 + font.width(label) + 8, bx1 = x1 - 10 - countW;
        bar(g, bx0, y + 5, bx1, y + 10, max == 0 ? 0 : value / (float) max, colour);
    }

    void bar(GuiGraphics g, int x0, int y0, int x1, int y1, float fraction, int colour) {
        if (x1 - x0 < 6) return;
        g.fill(x0, y0, x1, y1, FRAME_DARK);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SCREEN);
        int fill = x0 + 1 + Math.round((x1 - x0 - 2) * Mth.clamp(fraction, 0, 1));
        if (fill > x0 + 1) g.fill(x0 + 1, y0 + 1, fill, y1 - 1, colour);
    }

    void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, MID);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, LIGHT);
    }

    /** Cobblemon's Pokédex caught/seen ball, drawn at 8x8. Its texture has the seen outline on top, the caught ball below. */
    void dexIcon(GuiGraphics g, int x, int y, int knowledge) {
        RenderSystem.enableBlend();
        if (knowledge == TrainerCardData.UNKNOWN) g.setColor(1, 1, 1, 0.3f);
        int v = knowledge == TrainerCardData.CAUGHT ? 14 : 0;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(8 / 14f, 8 / 14f, 1);
        g.blit(DEX_ICON, 0, 0, 0, v, 14, 14, 14, 28);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }
}
