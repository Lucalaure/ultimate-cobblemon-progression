package org.ppvon.ultimateCobblemonProgression.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Text that stays inside the space it's given. Measure with the styled text (bold is wider than plain), and draw
 * with {@link #drawFitted} or {@link #wrap}, so a long name is cut short with "…" instead of running into its
 * neighbours. The layout check (gradlew runUiCheck, or the red outlines in the dev client) finds anything that doesn't.
 */
public final class UiText {
    private UiText() {}

    /** The text cut to fit maxWidth, ending in "…", keeping its colours and styles. Unchanged if it fits. */
    public static FormattedCharSequence ellipsize(Font font, FormattedText text, int maxWidth) {
        if (font.width(text) <= maxWidth) return Language.getInstance().getVisualOrder(text);
        FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width("…")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
    }

    /** Draws one line, left-aligned, cut short with "…" if wider than maxWidth. Returns the drawn width. */
    public static int drawFitted(GuiGraphics g, Font font, Component text, int x, int y, int maxWidth, int colour, boolean shadow) {
        if (maxWidth <= 0) return 0;
        FormattedCharSequence line = ellipsize(font, text, maxWidth);
        g.drawString(font, line, x, y, colour, shadow);
        return font.width(line);
    }

    /** Draws one line centred on x, cut short with "…" if wider than maxWidth. */
    public static void drawCentredFitted(GuiGraphics g, Font font, Component text, int x, int y, int maxWidth, int colour) {
        if (maxWidth <= 0) return;
        FormattedCharSequence line = ellipsize(font, text, maxWidth);
        g.drawString(font, line, x - font.width(line) / 2, y, colour, true);
    }

    /** The text wrapped to maxWidth, at most maxLines lines; the last line is cut short with "…" if there's more. */
    public static List<FormattedCharSequence> wrap(Font font, Component text, int maxWidth, int maxLines) {
        List<FormattedText> lines = font.getSplitter().splitLines(text, Math.max(1, maxWidth), Style.EMPTY);
        if (lines.size() <= maxLines) return lines.stream().map(Language.getInstance()::getVisualOrder).toList();
        List<FormattedCharSequence> out = new java.util.ArrayList<>();
        for (int i = 0; i < maxLines - 1; i++) out.add(Language.getInstance().getVisualOrder(lines.get(i)));
        // The rest of the text on the last line, cut short.
        FormattedText rest = FormattedText.composite(lines.subList(maxLines - 1, lines.size()).toArray(FormattedText[]::new));
        out.add(ellipsize(font, rest, maxWidth));
        return out;
    }
}
