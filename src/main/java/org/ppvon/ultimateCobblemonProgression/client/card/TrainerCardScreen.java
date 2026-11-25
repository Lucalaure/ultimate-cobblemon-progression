package org.ppvon.ultimateCobblemonProgression.client.card;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.ppvon.ultimateCobblemonProgression.client.card.CardPainter.Align;
import org.ppvon.ultimateCobblemonProgression.client.gui.layout.UiSweepable;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.ppvon.ultimateCobblemonProgression.client.card.CardStyle.*;

/**
 * The Trainer Card, drawn in Cobblemon's Pokédex style: its red frame and cyan screen, with tabs for the
 * player's profile, the region's trainer road, every region, and the wild Pokémon they can meet.
 * Cobblemon's textures are loaded from the installed Cobblemon jar, not copied into UCP.
 *
 * <p>This class lays the card out, routes input and draws the tab bar and the confirmation box; each tab is its own
 * {@link CardTab}, the region and difficulty choices are {@link ChoicePages}, and {@link CardPainter} draws the text.
 */
public class TrainerCardScreen extends Screen implements UiSweepable {
    private enum Tab {
        PROFILE("Profile"), ROAD("Road"), TRAINERS("Trainers"), REGIONS("Regions"), WILD("Wild Pokémon");
        final String label;
        Tab(String label) { this.label = label; }
    }

    /** The tab last looked at, so the card reopens there (e.g. after picking a region). */
    private static Tab lastTab = Tab.PROFILE;

    /** A pick to confirm, with its {@link PickRegionPayload} kind (home, next or extra region, or difficulty). */
    record Pending(String id, String title, int kind) {}

    /** A button's area, and the pick it makes (or null for buttons that don't pick anything). */
    record PickButton(int x0, int y0, int x1, int y1, Pending pick) {
        boolean contains(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    // confirmation box, in card coordinates
    private static final int BOX_X0 = 62, BOX_Y0 = 64, BOX_X1 = W - 62, BOX_Y1 = 146;
    private static final int YES_X0 = BOX_X0 + 14, NO_X1 = BOX_X1 - 14, BTN_Y0 = BOX_Y1 - 24, BTN_Y1 = BOX_Y1 - 10;

    private final TrainerCardData data;
    private final CardPainter painter = new CardPainter(() -> font);
    private final Map<Tab, CardTab> tabViews = new EnumMap<>(Tab.class);
    private final RoadTab road;
    private final WildTab wild;
    private final ChoicePages choices;
    private Tab tab;
    private float scale = 1;
    private int left, top;

    /** How far the current tab is scrolled, in card units. */
    double scroll;
    /** The current tab's height, set as it draws. */
    int contentHeight;
    /** The pick waiting for the player to confirm it, or null. */
    Pending confirming;

    public TrainerCardScreen(TrainerCardData data) {
        super(Component.literal("Trainer Card"));
        this.data = data;
        road = new RoadTab(this);
        wild = new WildTab(this);
        tabViews.put(Tab.PROFILE, new ProfileTab(this));
        tabViews.put(Tab.ROAD, road);
        tabViews.put(Tab.TRAINERS, new TrainersTab(this));
        tabViews.put(Tab.REGIONS, new RegionsTab(this));
        tabViews.put(Tab.WILD, wild);
        choices = new ChoicePages(this);
        this.tab = tabs().contains(lastTab) ? lastTab : Tab.PROFILE;
    }

    TrainerCardData data() { return data; }
    CardPainter painter() { return painter; }
    Minecraft minecraft() { return minecraft; }

    private List<Tab> tabs() {
        if (!data.regionMode()) return List.of(Tab.PROFILE, Tab.WILD);
        return data.wildTrainers().isEmpty()
                ? List.of(Tab.PROFILE, Tab.ROAD, Tab.REGIONS, Tab.WILD)
                : List.of(Tab.values());
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 8) / (float) W, (height - 8) / (float) H));
        left = Math.round((width - W * scale) / 2);
        top = Math.round((height - H * scale) / 2);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For the layout check: every tab a page at a time, the other league's road, and each confirmation box. */
    @Override
    public void sweepStates(java.util.function.Consumer<String> draw) {
        Tab before = lastTab;
        if (choices.showing()) {
            draw.accept(choices.showingRegionChoice() ? "region choice" : "difficulty choice");
        } else {
            for (Tab t : tabs()) {
                tab = t;
                for (boolean other : t == Tab.ROAD && !data.otherLeagues().isEmpty() ? new boolean[]{false, true} : new boolean[]{false}) {
                    road.showOtherRoad = other;
                    scroll = 0;
                    contentHeight = 0;
                    int page = 1;
                    draw.accept(t.label + (other ? ", other league" : "") + ", page 1");
                    int view = CY1 - CY0;
                    while (scroll + view < contentHeight && page < 40) {
                        scroll = Math.min(scroll + view - 20, contentHeight - view);
                        draw.accept(t.label + (other ? ", other league" : "") + ", page " + ++page);
                    }
                }
            }
            road.showOtherRoad = false;
            tab = tabs().get(0);
            scroll = 0;
        }
        String title = data.regionChoices().isEmpty() ? data.regionTitle() : data.regionChoices().get(0).title();
        for (int kind : new int[]{PickRegionPayload.HOME, PickRegionPayload.NEXT, PickRegionPayload.EXTRA, PickRegionPayload.DIFFICULTY}) {
            confirming = new Pending("x", kind == PickRegionPayload.DIFFICULTY ? "Normal" : title, kind);
            draw.accept("confirm box " + kind);
        }
        confirming = null;
        lastTab = before;
    }

    // ------------------------------------------------------------------ input

    private double lx(double mouseX) { return (mouseX - left) / scale; }
    private double ly(double mouseY) { return (mouseY - top) / scale; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = lx(mouseX), y = ly(mouseY);
        if (confirming != null) {
            if (y >= BTN_Y0 && y < BTN_Y1 && x >= YES_X0 && x < yesX1()) {
                ClientPlayNetworking.send(new PickRegionPayload(confirming.id(), confirming.kind()));
                clickSound();
                confirming = null;
            } else if (y >= BTN_Y0 && y < BTN_Y1 && x >= noX0() && x < NO_X1) {
                clickSound();
                confirming = null;
            }
            return true; // the box is modal
        }
        if (choices.showing()) {
            choices.click(x, y);
            return true; // the choice pages have nothing else to click
        }
        CardTab view = tabViews.get(tab);
        if (view.scrolls() && y >= CY0 && y < CY1 && view.click(x, y + scroll)) return true;

        List<Tab> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int tx = tabX(i);
            if (x >= tx && x < tx + tabWidth() && y >= 20 && y < 33) {
                if (tab != tabs.get(i)) {
                    tab = tabs.get(i);
                    lastTab = tab;
                    scroll = 0;
                    clickSound();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirming != null && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            confirming = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    void clickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                    .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = tabViews.get(tab).scrollStep();
        scroll = Mth.clamp(scroll - scrollY * step, 0, Math.max(0, contentHeight - (CY1 - CY0)));
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        wild.hovered = null;
        int mx = (int) lx(mouseX), my = (int) ly(mouseY);

        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);

        g.fill(12, 10, W - 12, H - 10, SCREEN);
        if (choices.showing()) {
            choices.draw(g, mx, my);
        } else {
            drawTabs(g, mx, my);
            CardTab view = tabViews.get(tab);
            if (view.scrolls()) scrolled(g, () -> view.draw(g, mx, my));
            else view.draw(g, mx, my);
        }

        RenderSystem.enableBlend();
        g.blit(FRAME, 0, 0, 0, 0, W, H, W, H);
        painter.text(g, Component.literal("TRAINER CARD").withStyle(ChatFormatting.BOLD), W / 2f, 10.5f, WHITE, 0.75f, true);
        if (confirming != null) drawConfirm(g, mx, my);
        g.pose().popPose();

        if (wild.hovered != null) g.renderComponentTooltip(font, WildTab.tooltip(wild.hovered), mouseX, mouseY);
    }

    private int tabWidth() {
        return (CX1 - CX0 - 4 * (tabs().size() - 1)) / tabs().size();
    }

    private int tabX(int i) {
        return CX0 + i * (tabWidth() + 4);
    }

    private void drawTabs(GuiGraphics g, int mx, int my) {
        List<Tab> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            int x = tabX(i), w = tabWidth();
            boolean selected = t == tab;
            boolean hover = mx >= x && mx < x + w && my >= 20 && my < 33;
            g.fill(x, 20, x + w, 33, selected ? DARK : MID);
            g.fill(x + 1, 21, x + w - 1, 32, selected ? DARK : hover ? LIGHT : SCREEN);
            MutableComponent label = Component.literal(t.label).withStyle(selected ? ChatFormatting.BOLD : ChatFormatting.RESET);
            if (t == Tab.REGIONS && data.canPickStart()) {
                label.append(Component.literal(" !").withStyle(s -> s.withColor(selected ? GOLD : RED).withBold(true)));
            }
            painter.fit(g, label, x + w / 2f, 23, w - 4, selected ? WHITE : INK, 1, Align.CENTRE);
        }
    }

    /** Draws a scrolling tab inside the content area, clipped to it. */
    private void scrolled(GuiGraphics g, Runnable body) {
        int sx0 = Math.round(left + CX0 * scale), sy0 = Math.round(top + CY0 * scale);
        int sx1 = Math.round(left + CX1 * scale), sy1 = Math.round(top + CY1 * scale);
        g.enableScissor(sx0, sy0, sx1, sy1);
        g.pose().pushPose();
        g.pose().translate(0, -scroll, 0);
        body.run();
        g.pose().popPose();
        g.disableScissor();

        int view = CY1 - CY0;
        if (contentHeight > view) {
            int barH = Math.max(12, view * view / contentHeight);
            int barY = CY0 + (int) ((view - barH) * (scroll / (contentHeight - view)));
            g.fill(CX1 - 3, CY0, CX1, CY1, MID);
            g.fill(CX1 - 3, barY, CX1, barY + barH, DARK);
        }
        scroll = Mth.clamp(scroll, 0, Math.max(0, contentHeight - view));
    }

    // Confirmation ----------------------------------------------------

    private int yesX1() { return (BOX_X0 + BOX_X1) / 2 - 4; }
    private int noX0() { return (BOX_X0 + BOX_X1) / 2 + 4; }

    private void drawConfirm(GuiGraphics g, int mx, int my) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        g.fill(12, 10, W - 12, H - 10, 0x99000000);
        g.fill(BOX_X0, BOX_Y0, BOX_X1, BOX_Y1, FRAME_DARK);
        g.fill(BOX_X0 + 1, BOX_Y0 + 1, BOX_X1 - 1, BOX_Y1 - 1, SCREEN);
        g.fill(BOX_X0 + 1, BOX_Y0 + 1, BOX_X1 - 1, BOX_Y0 + 15, DARK);
        String verb = switch (confirming.kind()) {
            case PickRegionPayload.HOME -> "Start in ";
            case PickRegionPayload.NEXT -> "Travel to ";
            case PickRegionPayload.DIFFICULTY -> "Switch to ";
            default -> "Explore ";
        };
        String title = verb + confirming.title() + "?";
        String body = switch (confirming.kind()) {
            case PickRegionPayload.HOME -> "Your journey begins in " + confirming.title() + " and you join its trainer series. This choice is permanent.";
            case PickRegionPayload.NEXT -> "Your journey continues in " + confirming.title() + " and you join its trainer series. This choice is permanent.";
            case PickRegionPayload.DIFFICULTY -> "From the next leader a gym summons, leaders use their " + confirming.title()
                    + " teams. Badges you already have stay as they are. You can switch back any time.";
            default -> confirming.title() + " opens now, alongside your first region. This choice is permanent.";
        };
        painter.fit(g, Component.literal(title).withStyle(ChatFormatting.BOLD), (BOX_X0 + BOX_X1) / 2f, BOX_Y0 + 4,
                BOX_X1 - BOX_X0 - 10, WHITE, 1, Align.CENTRE);
        int ty = BOX_Y0 + 21;
        painter.paragraph(g, Component.literal(body), (BOX_X0 + BOX_X1) / 2f, ty, BOX_X1 - BOX_X0 - 20, BTN_Y0 - 4 - ty, INK, 1, Align.CENTRE);
        button(g, YES_X0, yesX1(), verb + confirming.title(), RED, mx, my);
        button(g, noX0(), NO_X1, "Cancel", MID, mx, my);
        g.pose().popPose();
    }

    private void button(GuiGraphics g, int x0, int x1, String label, int colour, int mx, int my) {
        boolean hover = mx >= x0 && mx < x1 && my >= BTN_Y0 && my < BTN_Y1;
        g.fill(x0, BTN_Y0, x1, BTN_Y1, FRAME_DARK);
        g.fill(x0 + 1, BTN_Y0 + 1, x1 - 1, BTN_Y1 - 1, hover ? LIGHT : colour);
        painter.fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (x0 + x1) / 2f, BTN_Y0 + 3, x1 - x0 - 6,
                hover ? INK : WHITE, 1, Align.CENTRE);
    }
}
