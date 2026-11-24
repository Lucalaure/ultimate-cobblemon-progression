package org.ppvon.ultimateCobblemonProgression.client.gui.layout;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds text that runs into other text, out of its box or off the screen on UCP's screens.
 *
 * While a UCP screen draws, the GuiGraphics mixin reports every line of text, every filled box and every scissor
 * here, and the widget mixin says which widget is drawing. Afterwards each line of text is checked against:
 * <ul>
 *   <li>other text on the same layer: the two mustn't overlap,</li>
 *   <li>filled boxes (panels, rows, bars, buttons) on the same layer: text inside a box must stay inside it,</li>
 *   <li>widgets: a widget's own text must stay inside it, and no other text may touch it,</li>
 *   <li>its scissor: a line cut off at the side doesn't fit (scrolling lists clip at the top and bottom, which is fine),</li>
 *   <li>the screen's edges.</li>
 * </ul>
 * Widgets mustn't overlap each other either.
 *
 * In the dev client this runs on every UCP screen: problems are outlined in red and logged once. Elsewhere it's off
 * unless the game is started with {@code -Ducp.uiCheck=live}. {@link UiSweep} runs it over every screen at every
 * size ({@code gradlew runUiCheck}).
 */
public final class UiCheck {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String PROPERTY = System.getProperty("ucp.uiCheck", "");
    /** Outlines and logs problems on UCP screens as they're drawn. */
    public static final boolean LIVE = FabricLoader.getInstance().isDevelopmentEnvironment()
            || PROPERTY.equals("live") || PROPERTY.equals("run") || PROPERTY.equals("exit");

    /** Slack for rounding, in GUI pixels: boxes may touch or overlap by this much. */
    private static final float SLACK = 1f;
    /** Height of a line of text's glyphs (the font's line height less its spacing). */
    private static final int GLYPH_HEIGHT = 8;

    record Rect(float x0, float y0, float x1, float y1) {
        float w() { return x1 - x0; }
        float h() { return y1 - y0; }
        boolean empty() { return w() <= 0 || h() <= 0; }

        Rect intersect(Rect o) {
            return new Rect(Math.max(x0, o.x0), Math.max(y0, o.y0), Math.min(x1, o.x1), Math.min(y1, o.y1));
        }

        /** Whether the two overlap by more than the slack each way. */
        boolean overlaps(Rect o) {
            Rect i = intersect(o);
            return i.w() > SLACK && i.h() > SLACK;
        }

        boolean contains(Rect o) {
            return o.x0 >= x0 - SLACK && o.y0 >= y0 - SLACK && o.x1 <= x1 + SLACK && o.y1 <= y1 + SLACK;
        }

        @Override
        public String toString() {
            return String.format("(%.0f,%.0f %.0fx%.0f)", x0, y0, w(), h());
        }
    }

    /** A line of text as drawn: its box on screen, the part the scissor leaves, its layer and the widgets drawing it. */
    record Text(String text, Rect rect, Rect visible, Rect scissor, int layer, List<AbstractWidget> owners) {}

    /** A widget as drawn (widgets in a scrolling list are drawn by the list): its box, the part the scissor leaves, and the widgets around it. */
    record Drawn(AbstractWidget widget, Rect rect, Rect visible, List<AbstractWidget> parents) {}

    record Fill(Rect rect, int layer) {}

    private static boolean recording;
    private static Screen screen;
    private static final List<Text> texts = new ArrayList<>();
    private static final List<Fill> fills = new ArrayList<>();
    private static final Deque<Rect> scissors = new ArrayDeque<>();
    private static final Deque<AbstractWidget> widgets = new ArrayDeque<>();
    private static final List<Drawn> drawn = new ArrayList<>();
    /** Problems already logged by the live check, so each is logged once. */
    private static final Set<String> logged = new HashSet<>();

    private UiCheck() {}

    /** Whether a screen is one of UCP's. */
    public static boolean isOurs(Screen s) {
        return s != null && s.getClass().getName().startsWith("org.ppvon.ultimateCobblemonProgression.");
    }

    // ------------------------------------------------------------------ recording

    public static void begin(Screen s) {
        recording = true;
        screen = s;
        texts.clear();
        fills.clear();
        scissors.clear();
        widgets.clear();
        drawn.clear();
    }

    public static boolean recording() {
        return recording;
    }

    public static void text(GuiGraphics g, Font font, String text, int x, int y) {
        if (!recording || text == null || text.isBlank()) return;
        addText(g, text, x, y, font.width(text));
    }

    public static void text(GuiGraphics g, Font font, FormattedCharSequence text, int x, int y) {
        if (!recording || text == null) return;
        StringBuilder sb = new StringBuilder();
        text.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        if (sb.toString().isBlank()) return;
        addText(g, sb.toString(), x, y, font.width(text));
    }

    private static void addText(GuiGraphics g, String text, int x, int y, int width) {
        if (width <= 0) return;
        // Leading and trailing spaces take up no ink.
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        String trimmed = text.stripLeading();
        int lead = font.width(text.substring(0, text.length() - trimmed.length()));
        int trail = font.width(trimmed) - font.width(trimmed.stripTrailing());
        Matrix4f m = g.pose().last().pose();
        Rect rect = transform(m, x + lead, y, x + width - trail, y + GLYPH_HEIGHT);
        if (rect.empty()) return;
        Rect scissor = scissors.peek();
        Rect visible = scissor == null ? rect : rect.intersect(scissor);
        if (visible.empty()) return; // scrolled out of view
        texts.add(new Text(text.strip(), rect, visible, scissor, layer(m, 0), List.copyOf(widgets)));
    }

    public static void fill(GuiGraphics g, int x0, int y0, int x1, int y1, int z, int colour) {
        if (!recording || (colour >>> 24) < 0x20) return; // see-through fills aren't boxes
        Matrix4f m = g.pose().last().pose();
        Rect rect = transform(m, Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));
        Rect scissor = scissors.peek();
        Rect visible = scissor == null ? rect : rect.intersect(scissor);
        // Thin lines (dividers, outlines) aren't boxes text sits in.
        if (visible.w() <= 2 || visible.h() <= 2) return;
        fills.add(new Fill(visible, layer(m, z)));
    }

    public static void pushScissor(int x0, int y0, int x1, int y1) {
        if (!recording) return;
        Rect r = new Rect(x0, y0, x1, y1);
        Rect top = scissors.peek();
        scissors.push(top == null ? r : top.intersect(r));
    }

    public static void popScissor() {
        if (recording && !scissors.isEmpty()) scissors.pop();
    }

    public static void pushWidget(AbstractWidget w) {
        if (!recording) return;
        if (w.visible && w.getWidth() > 0 && w.getHeight() > 0) {
            Rect r = rect(w);
            Rect scissor = scissors.peek();
            Rect visible = scissor == null ? r : r.intersect(scissor);
            if (!visible.empty()) drawn.add(new Drawn(w, r, visible, List.copyOf(widgets)));
        }
        widgets.push(w);
    }

    public static void popWidget() {
        if (recording && !widgets.isEmpty()) widgets.pop();
    }

    private static Rect transform(Matrix4f m, float x0, float y0, float x1, float y1) {
        Vector3f a = m.transformPosition(new Vector3f(x0, y0, 0));
        Vector3f b = m.transformPosition(new Vector3f(x1, y1, 0));
        return new Rect(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.max(a.x, b.x), Math.max(a.y, b.y));
    }

    /** Things drawn at different depths (a dialog over the screen, say) don't collide. */
    private static int layer(Matrix4f m, int z) {
        return Math.round(m.m32() + z);
    }

    // ------------------------------------------------------------------ checking

    /** A problem: what's wrong, and the boxes to outline. */
    public record Problem(String message, List<Rect> rects) {}

    /** Problems handed back by {@link #restore}, returned by the next {@link #end} instead of checking again. */
    private static List<Problem> restored;

    /** Makes the next {@link #end} return these problems (the sweep checks, then draws outlines, then finishes). */
    static void restore(List<Problem> problems) {
        restored = problems;
    }

    /** Stops recording and checks what was drawn. */
    public static List<Problem> end() {
        if (restored != null) {
            List<Problem> r = restored;
            restored = null;
            return r;
        }
        recording = false;
        Screen s = screen;
        screen = null;
        List<Problem> problems = new ArrayList<>();
        if (s == null) return problems;

        Rect screenRect = new Rect(0, 0, s.width, s.height);

        for (int i = 0; i < texts.size(); i++) {
            Text t = texts.get(i);
            if (!screenRect.contains(t.visible)) {
                problems.add(new Problem("\"" + t.text + "\" runs off the screen " + t.rect, List.of(t.visible)));
            }
            if (t.scissor != null && (t.rect.x0 < t.scissor.x0 - SLACK || t.rect.x1 > t.scissor.x1 + SLACK)) {
                problems.add(new Problem("\"" + t.text + "\" is cut off at the side: it needs " + Math.round(t.rect.w())
                        + "px, its area has " + Math.round(t.scissor.w()) + "px", List.of(t.visible)));
            }
            for (int j = i + 1; j < texts.size(); j++) {
                Text o = texts.get(j);
                if (o.layer == t.layer && t.visible.overlaps(o.visible) && !t.text.equals(o.text)) {
                    problems.add(new Problem("\"" + t.text + "\" overlaps \"" + o.text + "\"", List.of(t.visible, o.visible)));
                }
            }
            for (Fill f : fills) {
                if (f.layer == t.layer && t.visible.overlaps(f.rect) && !f.rect.contains(t.visible)
                        // A box inside the text's line (a chip behind one letter, say) isn't what the text sits in.
                        && !(t.visible.contains(f.rect))) {
                    problems.add(new Problem("\"" + t.text + "\" runs over the edge of a box " + f.rect, List.of(t.visible, f.rect)));
                }
            }
            for (Drawn d : drawn) {
                if (t.owners.contains(d.widget)) {
                    if (!d.visible.contains(t.visible)) {
                        problems.add(new Problem("\"" + t.text + "\" doesn't fit inside its own " + name(d.widget), List.of(t.visible, d.visible)));
                    }
                } else if (t.layer == 0 && t.visible.overlaps(d.visible)) {
                    problems.add(new Problem("\"" + t.text + "\" overlaps " + name(d.widget), List.of(t.visible, d.visible)));
                }
            }
        }
        for (int i = 0; i < drawn.size(); i++) {
            Drawn a = drawn.get(i);
            if (!screenRect.contains(a.visible)) {
                problems.add(new Problem(name(a.widget) + " runs off the screen " + a.rect, List.of(a.visible)));
            }
            for (int j = i + 1; j < drawn.size(); j++) {
                Drawn b = drawn.get(j);
                // A list and the widgets in it are meant to overlap.
                if (a.parents.contains(b.widget) || b.parents.contains(a.widget)) continue;
                if (a.visible.overlaps(b.visible)) {
                    problems.add(new Problem(name(a.widget) + " overlaps " + name(b.widget), List.of(a.visible, b.visible)));
                }
            }
        }
        // The same problem found twice (two fills of one panel, say) is reported once.
        Set<String> seen = new LinkedHashSet<>();
        problems.removeIf(p -> !seen.add(p.message));
        return problems;
    }

    private static Rect rect(AbstractWidget w) {
        return new Rect(w.getX(), w.getY(), w.getX() + w.getWidth(), w.getY() + w.getHeight());
    }

    private static String name(AbstractWidget w) {
        String type = w.getClass().getSimpleName();
        if (type.isEmpty()) type = "widget";
        return type + " \"" + w.getMessage().getString() + "\"";
    }

    // ------------------------------------------------------------------ live check

    /** Outlines problems in red. */
    static void outline(GuiGraphics g, List<Problem> problems) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 1000);
        for (Problem p : problems) {
            for (Rect r : p.rects) {
                int x0 = (int) Math.floor(r.x0), y0 = (int) Math.floor(r.y0);
                g.renderOutline(x0, y0, Math.max(1, (int) Math.ceil(r.x1) - x0), Math.max(1, (int) Math.ceil(r.y1) - y0), 0xFFFF2020);
            }
        }
        g.pose().popPose();
    }

    /** Outlines this frame's problems in red, and logs any not logged before. */
    public static void showLive(Screen s, GuiGraphics g, List<Problem> problems) {
        if (problems.isEmpty()) return;
        outline(g, problems);
        g.pose().pushPose();
        g.pose().translate(0, 0, 1000);
        for (Problem p : problems) {
            if (logged.add(s.getClass().getName() + p.message)) {
                LOG.warn("[UCP UI check] {} at {}x{}: {}", s.getClass().getSimpleName(), s.width, s.height, p.message);
            }
        }
        String banner = problems.size() + " layout problem" + (problems.size() == 1 ? "" : "s") + " (see log)";
        Font font = net.minecraft.client.Minecraft.getInstance().font;
        g.fill(0, 0, font.width(banner) + 4, 11, 0xC0000000);
        g.drawString(font, banner, 2, 2, 0xFFFF5050, false);
        g.pose().popPose();
    }
}
