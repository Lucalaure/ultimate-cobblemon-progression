package org.ppvon.ultimateCobblemonProgression.client.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.ppvon.ultimateCobblemonProgression.client.gui.UiText;

import java.util.List;

/**
 * A config screen's settings, one per row, scrolling when the window is too short to show them all. Each row has its
 * own space, so settings can't run into each other or into the Save and Cancel buttons at any window size or GUI scale.
 */
public final class ConfigList extends ContainerObjectSelectionList<ConfigList.Row> {
    public static final int ROW_HEIGHT = 24;
    /** Width of the number field on a labelled row. */
    public static final int FIELD_WIDTH = 100;
    private static final int GREY = 0xFFA0A0A0;

    private final int rowWidth;

    public ConfigList(Minecraft mc, int width, int height, int y) {
        super(mc, width, height, y, ROW_HEIGHT);
        this.rowWidth = Math.min(310, width - 24);
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    protected int getScrollbarPosition() {
        return getRowRight() + 6;
    }

    /** A checkbox, button or slider on its own row. Buttons and sliders are stretched to the row's width. */
    public <W extends AbstractWidget> W add(W widget) {
        if (!(widget instanceof Checkbox)) widget.setWidth(rowWidth);
        addEntry(new Row(widget, null, List.of()));
        return widget;
    }

    /** A label on the left and a field on the right, like "Tier Cap Scaling   [0.25]". */
    public <W extends AbstractWidget> W addLabelled(String label, W field) {
        field.setWidth(FIELD_WIDTH);
        addEntry(new Row(field, Component.literal(label), List.of()));
        return field;
    }

    /** A grey note explaining the setting above it, wrapped onto as many rows as it needs (two lines a row). */
    public void addNote(String text) {
        List<FormattedCharSequence> lines = UiText.wrap(minecraft.font, Component.literal(text), rowWidth - 4, 4);
        for (int i = 0; i < lines.size(); i += 2) {
            addEntry(new Row(null, null, lines.subList(i, Math.min(i + 2, lines.size()))));
        }
    }

    public final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final AbstractWidget widget;
        private final Component label;
        private final List<FormattedCharSequence> note;

        Row(AbstractWidget widget, Component label, List<FormattedCharSequence> note) {
            this.widget = widget;
            this.label = label;
            this.note = note;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY,
                           boolean hovering, float partialTick) {
            var font = minecraft.font;
            if (widget != null) {
                widget.setX(label == null ? left : left + width - widget.getWidth());
                widget.setY(top + (height - widget.getHeight()) / 2);
                widget.render(g, mouseX, mouseY, partialTick);
            }
            if (label != null) {
                UiText.drawFitted(g, font, label, left + 2, top + (height - 8) / 2, width - FIELD_WIDTH - 10, GREY, false);
            }
            int y = top + (height - note.size() * 10) / 2 + 1;
            for (FormattedCharSequence line : note) {
                g.drawString(font, line, left + 2, y, GREY, false);
                y += 10;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widget == null ? List.of() : List.of(widget);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widget == null ? List.of() : List.of(widget);
        }
    }
}
