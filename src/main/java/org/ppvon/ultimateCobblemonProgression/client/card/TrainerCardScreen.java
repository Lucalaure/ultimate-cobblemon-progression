package org.ppvon.ultimateCobblemonProgression.client.card;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RegionEntry;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RoadEntry;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.TrainerGroup;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildTrainer;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildGroup;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildSpecies;

import java.util.ArrayList;
import java.util.List;

/**
 * The Trainer Card, drawn in Cobblemon's Pokédex style: its red frame and cyan screen, with tabs for the
 * player's profile, the region's trainer road, every region, and the wild Pokémon they can meet.
 * Cobblemon's textures are loaded from the installed Cobblemon jar, not copied into UCP.
 */
public class TrainerCardScreen extends Screen {
    private static final int W = 345, H = 207;

    private static final ResourceLocation FRAME = cobblemon("textures/gui/pokedex/pokedex_base_red.png");
    private static final ResourceLocation PORTRAIT = cobblemon("textures/gui/summary/portrait_background.png");
    private static final ResourceLocation DEX_ICON = cobblemon("textures/gui/pokedex/caught_seen_icon.png");

    // Cobblemon's Pokédex palette
    private static final int SCREEN = 0xFF5BD1DA, LIGHT = 0xFF7EE7E5, MID = 0xFF44B3C8, DARK = 0xFF3A96B6;
    private static final int INK = 0xFF1B4652, MUTED = 0xFF3F7584, WHITE = 0xFFFFFFFF, FRAME_DARK = 0xFF2F2F2F;
    private static final int RED = 0xFFD74845, GREEN = 0xFF3E9E4F, GOLD = 0xFFE2A82C, GREY = 0xFF8AA9B3;

    // content area, in card coordinates
    private static final int CX0 = 20, CY0 = 38, CX1 = 325, CY1 = 189;

    private enum Tab {
        PROFILE("Profile"), ROAD("Road"), TRAINERS("Trainers"), REGIONS("Regions"), WILD("Wild Pokémon");
        final String label;
        Tab(String label) { this.label = label; }
    }

    /** The tab last looked at, so the card reopens there (e.g. after picking a region). */
    private static Tab lastTab = Tab.PROFILE;
    /** Trainers tab: whether beaten trainers are hidden, and which regions' lists are folded away. Kept between openings. */
    private static boolean hideBeaten;
    private static final java.util.Set<String> collapsedTrainerGroups = new java.util.HashSet<>();

    private final TrainerCardData data;
    private Tab tab;
    private double scroll;
    private int contentHeight;
    private float scale = 1;
    private int left, top;
    /** Species under the mouse on the Wild tab, for the tooltip. */
    private WildSpecies hovered;
    /** Two leagues at once: whether the Road tab shows the other league, and where its switch is drawn. */
    private boolean showOtherRoad;
    private int[] roadToggle;
    /** "Pick" buttons on the Regions tab, in content coordinates (before scrolling). */
    private final List<PickButton> pickButtons = new ArrayList<>();
    /** The pick waiting for the player to confirm it, or null. */
    private Pending confirming;
    /** "Start here" / "Travel here" buttons on the region choice screen, in card coordinates. */
    private final List<PickButton> choiceButtons = new ArrayList<>();
    /** The choice screen's "Not now" button (next region only), in card coordinates, or null. */
    private PickButton laterButton;
    /** The player put off picking their next region, so the card shows its tabs for now. */
    private boolean choiceDismissed;
    /** "Choose Normal" / "Choose Hard" buttons on the difficulty choice screen, in card coordinates. */
    private final List<PickButton> difficultyButtons = new ArrayList<>();
    /** The Road tab's "Switch to ..." button, in content coordinates (before scrolling), or null. */
    private PickButton difficultyToggle;
    /** The Trainers tab's "Hide beaten" button, in content coordinates (before scrolling), or null. */
    private PickButton hideBeatenToggle;
    /** The Trainers tab's region headers, which fold their list, in content coordinates; the pick's id is the region title. */
    private final List<PickButton> trainerHeaders = new ArrayList<>();

    /** A pick to confirm, with its {@link PickRegionPayload} kind (home, next or extra region, or difficulty). */
    private record Pending(String id, String title, int kind) {}

    private record PickButton(int x0, int y0, int x1, int y1, Pending pick) {}

    // confirmation box, in card coordinates
    private static final int BOX_X0 = 62, BOX_Y0 = 64, BOX_X1 = W - 62, BOX_Y1 = 146;
    private static final int YES_X0 = BOX_X0 + 14, NO_X1 = BOX_X1 - 14, BTN_Y0 = BOX_Y1 - 24, BTN_Y1 = BOX_Y1 - 10;

    public TrainerCardScreen(TrainerCardData data) {
        super(Component.literal("Trainer Card"));
        this.data = data;
        this.tab = tabs().contains(lastTab) ? lastTab : Tab.PROFILE;
    }

    private static ResourceLocation cobblemon(String path) {
        return ResourceLocation.fromNamespaceAndPath("cobblemon", path);
    }

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

    // ------------------------------------------------------------------ input

    private double lx(double mouseX) { return (mouseX - left) / scale; }
    private double ly(double mouseY) { return (mouseY - top) / scale; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double x = lx(mouseX), y = ly(mouseY);
        if (confirming != null) {
            if (y >= BTN_Y0 && y < BTN_Y1 && x >= YES_X0 && x < yesX1()) {
                ClientPlayNetworking.send(new PickRegionPayload(confirming.id(), confirming.kind()));
                click();
                confirming = null;
            } else if (y >= BTN_Y0 && y < BTN_Y1 && x >= noX0() && x < NO_X1) {
                click();
                confirming = null;
            }
            return true; // the box is modal
        }
        if (showingChoice()) {
            for (PickButton b : choiceButtons) {
                if (x >= b.x0 && x < b.x1 && y >= b.y0 && y < b.y1) {
                    confirming = b.pick;
                    click();
                }
            }
            PickButton later = laterButton;
            if (later != null && x >= later.x0 && x < later.x1 && y >= later.y0 && y < later.y1) {
                choiceDismissed = true;
                click();
            }
            return true; // the choice screen has nothing else to click
        }
        if (showingDifficultyChoice()) {
            for (PickButton b : difficultyButtons) {
                if (x >= b.x0 && x < b.x1 && y >= b.y0 && y < b.y1) {
                    // No confirmation: the difficulty can be changed again at any time.
                    ClientPlayNetworking.send(new PickRegionPayload(b.pick.id(), PickRegionPayload.DIFFICULTY));
                    click();
                }
            }
            return true;
        }
        if (tab == Tab.ROAD && y >= CY0 && y < CY1 && roadToggle != null) {
            double cy = y + scroll;
            int[] b = roadToggle;
            if (x >= b[0] && x < b[2] && cy >= b[1] && cy < b[3]) {
                showOtherRoad = !showOtherRoad;
                click();
                return true;
            }
        }
        if (tab == Tab.ROAD && y >= CY0 && y < CY1 && difficultyToggle != null) {
            double cy = y + scroll;
            PickButton b = difficultyToggle;
            if (x >= b.x0 && x < b.x1 && cy >= b.y0 && cy < b.y1) {
                confirming = b.pick;
                click();
                return true;
            }
        }
        if (tab == Tab.TRAINERS && y >= CY0 && y < CY1) {
            double cy = y + scroll;
            PickButton toggle = hideBeatenToggle;
            if (toggle != null && x >= toggle.x0 && x < toggle.x1 && cy >= toggle.y0 && cy < toggle.y1) {
                hideBeaten = !hideBeaten;
                click();
                return true;
            }
            for (PickButton b : trainerHeaders) {
                if (x >= b.x0 && x < b.x1 && cy >= b.y0 && cy < b.y1) {
                    if (!collapsedTrainerGroups.remove(b.pick.id())) collapsedTrainerGroups.add(b.pick.id());
                    click();
                    return true;
                }
            }
        }
        if (tab == Tab.REGIONS && y >= CY0 && y < CY1) {
            double cy = y + scroll;
            for (PickButton b : pickButtons) {
                if (x >= b.x0 && x < b.x1 && cy >= b.y0 && cy < b.y1) {
                    confirming = b.pick;
                    click();
                    return true;
                }
            }
        }
        List<Tab> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            int tx = tabX(i);
            if (x >= tx && x < tx + tabWidth() && y >= 20 && y < 33) {
                if (tab != tabs.get(i)) {
                    tab = tabs.get(i);
                    lastTab = tab;
                    scroll = 0;
                    click();
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

    private void click() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                    .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // The Trainers tab's lists run to hundreds of rows, so it scrolls three rows at a time.
        int step = tab == Tab.TRAINERS ? TRAINER_ROW * 3 : 14;
        scroll = Mth.clamp(scroll - scrollY * step, 0, Math.max(0, contentHeight - (CY1 - CY0)));
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        hovered = null;
        int mx = (int) lx(mouseX), my = (int) ly(mouseY);

        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);

        g.fill(12, 10, W - 12, H - 10, SCREEN);
        if (showingChoice()) {
            drawChoice(g, mx, my);
        } else if (showingDifficultyChoice()) {
            drawDifficultyChoice(g, mx, my);
        } else {
            drawTabs(g, mx, my);
            drawTab(g, mx, my);
        }

        RenderSystem.enableBlend();
        g.blit(FRAME, 0, 0, 0, 0, W, H, W, H);
        text(g, Component.literal("TRAINER CARD").withStyle(ChatFormatting.BOLD), W / 2f, 10.5f, WHITE, 0.75f, true);
        if (confirming != null) drawConfirm(g, mx, my);
        g.pose().popPose();

        if (hovered != null) g.renderComponentTooltip(font, speciesTooltip(hovered), mouseX, mouseY);
    }

    private void drawTab(GuiGraphics g, int mx, int my) {
        switch (tab) {
            case PROFILE -> drawProfile(g);
            case ROAD -> scrolled(g, () -> drawRoad(g, mx, my));
            case TRAINERS -> scrolled(g, () -> drawTrainers(g, mx, my));
            case REGIONS -> scrolled(g, () -> drawRegions(g, mx, my));
            case WILD -> scrolled(g, () -> drawWild(g, mx, my));
        }
    }

    // Region choice screen -------------------------------------------

    /**
     * Whether the card opens on the region choice screen: always until the player picks where their journey starts,
     * and when they can pick where it goes next, unless they put that off.
     */
    private boolean showingChoice() {
        return data.needsChoice() && !(data.choosingNext() && choiceDismissed);
    }

    /**
     * Shown instead of the card until the player picks the region their journey starts in, and again each time
     * they complete their journey so far and can pick the region it continues in.
     */
    private void drawChoice(GuiGraphics g, int mx, int my) {
        choiceButtons.clear();
        laterButton = null;
        boolean next = data.choosingNext();
        text(g, Component.literal(next ? "CHOOSE YOUR NEXT REGION" : "CHOOSE YOUR SECOND REGION").withStyle(ChatFormatting.BOLD),
                W / 2f, 22, INK, 1, true);
        text(g, Component.literal(next ? "Where does your journey go next?" : "Where does your journey begin?"), W / 2f, 34, MUTED, 1, true);

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
            fit(g, Component.literal(c.title()).withStyle(ChatFormatting.BOLD), (px0 + px1) / 2f, y0 + 4, panelW - 8, WHITE, 1.25f, Align.CENTRE);

            int ty = y0 + 26, textW = panelW - 12;
            fit(g, Component.literal(c.trainers() + " trainers to beat"), px0 + 6, ty, textW, INK, 0.75f, Align.LEFT);
            fit(g, Component.literal(c.species() + " species"), px0 + 6, ty + 10, textW, INK, 0.75f, Align.LEFT);
            fit(g, Component.literal("First leader:"), px0 + 6, ty + 24, textW, MUTED, 0.75f, Align.LEFT);
            paragraph(g, c.firstTrainer().copy().withStyle(ChatFormatting.BOLD), px0 + 6, ty + 33, textW, py1 - 26 - (ty + 33),
                    INK, 0.85f, Align.LEFT);

            int bx0 = px0 + 8, bx1 = px1 - 8, by0 = py1 - 22, by1 = py1 - 8;
            boolean hover = confirming == null && mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
            fit(g, Component.literal(next ? "Travel here" : "Start here").withStyle(ChatFormatting.BOLD),
                    (bx0 + bx1) / 2f, by0 + 3, bx1 - bx0 - 6, WHITE, 1, Align.CENTRE);
            choiceButtons.add(new PickButton(bx0, by0, bx1, by1,
                    new Pending(c.id(), c.title(), next ? PickRegionPayload.NEXT : PickRegionPayload.HOME)));
        }

        int fy = y0 + panelH + 8;
        if (next) {
            // "Not now": look at the card first; it asks again the next time it's opened.
            String label = "Not now";
            int bw = font.width(label) + 16;
            int bx0 = W - 22 - bw, bx1 = W - 22, by0 = fy - 2, by1 = fy + 12;
            boolean hover = confirming == null && mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? LIGHT : MID);
            text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, hover ? INK : WHITE, 1, true);
            laterButton = new PickButton(bx0, by0, bx1, by1, null);
        }
        // Narrower beside "Not now", so it can't run into the button.
        paragraph(g, Component.literal(next
                        ? "You join the region's trainer series, and your level cap starts over with its first leader."
                        : "Later, you can also pick an optional second region to explore, on the card's Regions tab."),
                W / 2f, fy, (next ? W - 130 : W - 80) * 0.75f, H - 12 - fy, MUTED, 0.75f, Align.CENTRE);
    }

    // Difficulty choice screen ---------------------------------------

    /** Whether the card opens on the difficulty choice screen: once the journey has started, until a difficulty is picked. */
    private boolean showingDifficultyChoice() {
        return !showingChoice() && data.choosingDifficulty();
    }

    private void drawDifficultyChoice(GuiGraphics g, int mx, int my) {
        difficultyButtons.clear();
        text(g, Component.literal("CHOOSE YOUR CHALLENGE").withStyle(ChatFormatting.BOLD), W / 2f, 22, INK, 1, true);
        text(g, Component.literal("How strong should gym leaders be?"), W / 2f, 34, MUTED, 1, true);

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
            fit(g, Component.literal(o.title()).withStyle(ChatFormatting.BOLD), (px0 + px1) / 2f, y0 + 4, panelW - 8, WHITE, 1.25f, Align.CENTRE);
            paragraph(g, Component.literal(o.body()), px0 + 6, y0 + 25, panelW - 12, py1 - 26 - (y0 + 25), INK, 0.85f, Align.LEFT);

            int bx0 = px0 + 8, bx1 = px1 - 8, by0 = py1 - 22, by1 = py1 - 8;
            boolean hover = mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
            fit(g, Component.literal("Choose " + o.title()).withStyle(ChatFormatting.BOLD),
                    (bx0 + bx1) / 2f, by0 + 3, bx1 - bx0 - 6, WHITE, 1, Align.CENTRE);
            difficultyButtons.add(new PickButton(bx0, by0, bx1, by1, new Pending(o.id(), o.title(), PickRegionPayload.DIFFICULTY)));
        }
        int fy = y0 + panelH + 8;
        paragraph(g, Component.literal("You can switch at any time on the Road tab. It applies from the next leader a gym summons."),
                W / 2f, fy, (W - 80) * 0.75f, H - 12 - fy, MUTED, 0.75f, Align.CENTRE);
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
            fit(g, label, x + w / 2f, 23, w - 4, selected ? WHITE : INK, 1, Align.CENTRE);
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

    // Profile ---------------------------------------------------------

    private void drawProfile(GuiGraphics g) {
        // Portrait with the player's face, like a Pokémon's summary portrait
        g.blit(PORTRAIT, 24, 40, 0, 0, 66, 66, 66, 66);
        if (minecraft != null && minecraft.player != null) {
            PlayerFaceRenderer.draw(g, minecraft.player.getSkin(), 35, 51, 44);
        }
        fit(g, Component.literal(data.player()).withStyle(ChatFormatting.BOLD), 57, 110, 70, INK, 1, Align.CENTRE);
        fit(g, Component.literal(data.regionMode() ? data.regionTitle() : "Trainer"), 57, 121, 70, MUTED, 1, Align.CENTRE);

        int x0 = 98, x1 = CX1;
        int y = 40;
        if (data.regionMode()) {
            String regions = data.regionTitle();
            for (TrainerCardData.LeagueView other : data.otherLeagues()) regions += " + " + other.title();
            row(g, x0, x1, y, "Region", Component.literal(regions).withStyle(ChatFormatting.BOLD), INK);
            y += 18;
            barRow(g, x0, x1, y, "Tier", data.tier(), data.maxTier(), DARK);
        } else {
            barRow(g, x0, x1, y, "Trainer level", data.tier(), data.maxTier(), DARK);
        }
        y += 18;
        row(g, x0, x1, y, "Level cap", Component.literal("Lv. " + data.levelCap()).withStyle(ChatFormatting.BOLD), RED);
        y += 18;
        if (data.regionMode() && !data.road().isEmpty()) {
            barRow(g, x0, x1, y, "Trainers beaten", (int) data.beaten(), data.road().size(), GREEN);
            y += 18;
        }
        row(g, x0, x1, y, "Pokédex", Component.literal(data.seen() + " seen  ·  " + data.caught() + " caught"), INK);

        // Next challenge
        int by0 = 136, by1 = CY1 - 2;
        g.fill(24, by0, CX1, by1, FRAME_DARK);
        g.fill(25, by0 + 1, CX1 - 1, by1 - 1, DARK);
        boolean hasNext = !data.next().getString().isEmpty();
        String label = data.allDone() ? "CHAMPION" : hasNext ? "NEXT CHALLENGE" : "NEXT STEP";
        text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), 31, by0 + 5, LIGHT, 0.75f, false);
        int textW = CX1 - 34;
        int hintY = by0 + 15;
        if (hasNext) {
            fit(g, Component.literal("▶ ").append(data.next()).withStyle(ChatFormatting.BOLD), 31, by0 + 14, textW, WHITE, 1.25f, Align.LEFT);
            hintY = by0 + 28;
        }
        // Two leagues at once: the other league's next leader too.
        for (TrainerCardData.LeagueView other : data.otherLeagues()) {
            if (other.next().getString().isEmpty()) continue;
            fit(g, Component.literal("▶ " + other.title() + ": ").append(other.next()), 31, hintY, textW, WHITE, 1, Align.LEFT);
            hintY += 12;
        }
        // The hint can be long (e.g. the Pokémon still to catch): it shrinks to fit the box rather than being cut off.
        paragraph(g, data.hint(), 31, hintY, textW, by1 - 2 - hintY, LIGHT, 1, Align.LEFT);
    }

    private void row(GuiGraphics g, int x0, int x1, int y, String label, Component value, int valueColour) {
        panel(g, x0, y, x1, y + 15);
        text(g, Component.literal(label), x0 + 5, y + 4, MUTED, 1, false);
        fit(g, value, x1 - 5, y + 4, x1 - x0 - 18 - font.width(label), valueColour, 1, Align.RIGHT);
    }

    private void barRow(GuiGraphics g, int x0, int x1, int y, String label, int value, int max, int colour) {
        panel(g, x0, y, x1, y + 15);
        text(g, Component.literal(label), x0 + 5, y + 4, MUTED, 1, false);
        String count = value + " / " + max;
        int countW = font.width(count);
        g.drawString(font, count, x1 - 5 - countW, y + 4, INK, false);
        int bx0 = x0 + 5 + font.width(label) + 8, bx1 = x1 - 10 - countW;
        bar(g, bx0, y + 5, bx1, y + 10, max == 0 ? 0 : value / (float) max, colour);
    }

    private void bar(GuiGraphics g, int x0, int y0, int x1, int y1, float fraction, int colour) {
        if (x1 - x0 < 6) return;
        g.fill(x0, y0, x1, y1, FRAME_DARK);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, SCREEN);
        int fill = x0 + 1 + Math.round((x1 - x0 - 2) * Mth.clamp(fraction, 0, 1));
        if (fill > x0 + 1) g.fill(x0 + 1, y0 + 1, fill, y1 - 1, colour);
    }

    private void panel(GuiGraphics g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, MID);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, LIGHT);
    }

    // Road ------------------------------------------------------------

    private void drawRoad(GuiGraphics g, int mx, int my) {
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
            paragraph(g, Component.literal("No trainers are listed for " + roadTitle + "."), (CX0 + CX1) / 2f, y + 20,
                    CX1 - CX0 - 12, 30, MUTED, 1, Align.CENTRE);
            contentHeight = 0;
            return;
        }
        String summary = beaten + " / " + road.size() + " beaten";
        int titleEnd = CX1 - 14 - font.width(summary);
        if (otherLeague != null) {
            String label = "Show " + (showOtherRoad ? data.regionTitle() : otherLeague.title());
            int bw = font.width(label) + 10;
            int bx1 = CX1 - 6, bx0 = bx1 - bw, by0 = y - 2, by1 = y + 10;
            int contentMouseY = my + (int) scroll;
            boolean hover = confirming == null && my >= CY0 && my < CY1 && mx >= bx0 && mx < bx1
                    && contentMouseY >= by0 && contentMouseY < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? MID : DARK);
            text(g, Component.literal(label), (bx0 + bx1) / 2f, by0 + 2, WHITE, 1, true);
            roadToggle = new int[]{bx0, by0, bx1, by1};
            g.drawString(font, summary, bx0 - 6 - font.width(summary), y, MUTED, false);
            titleEnd = bx0 - 12 - font.width(summary);
        } else {
            g.drawString(font, summary, CX1 - 8 - font.width(summary), y, MUTED, false);
        }
        fit(g, Component.literal(roadTitle + " Road").withStyle(ChatFormatting.BOLD), CX0 + 4, y,
                titleEnd - (CX0 + 4), INK, 1, Align.LEFT);
        y += 13;

        if (!data.difficulty().isEmpty()) {
            // Gym difficulty, with a button to switch it when players choose their own.
            int rx0 = CX0 + 2, rx1 = CX1 - 6, ry1 = y + 15;
            panel(g, rx0, y, rx1, ry1);
            // A region without Hard leaders always summons Normal ones, so there's nothing to switch.
            boolean hardHere = showOtherRoad ? otherLeague.hardAvailable() : data.hardAvailable();
            boolean hard = hardHere && data.difficulty().equals("Hard");
            int labelEnd = rx1 - 6;
            if (data.difficultyChangeable() && hardHere) {
                String other = hard ? "Normal" : "Hard";
                String label = "Switch to " + other;
                int bw = font.width(label) + 12;
                int bx1 = rx1 - 2, bx0 = bx1 - bw, by0 = y + 2, by1 = ry1 - 2;
                int contentMouseY = my + (int) scroll;
                boolean hover = confirming == null && my >= CY0 && my < CY1 && mx >= bx0 && mx < bx1
                        && contentMouseY >= by0 && contentMouseY < by1;
                g.fill(bx0, by0, bx1, by1, FRAME_DARK);
                g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
                text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 2, WHITE, 1, true);
                difficultyToggle = new PickButton(bx0, by0, bx1, by1,
                        new Pending(other.toLowerCase(java.util.Locale.ROOT), other, PickRegionPayload.DIFFICULTY));
                labelEnd = bx0 - 6;
            }
            MutableComponent line = Component.literal("Gym leaders: ").append(Component.literal(hardHere ? data.difficulty() : "Normal")
                    .withStyle(s -> s.withBold(true).withColor(hard ? RED : INK)));
            if (!hardHere) line.append(Component.literal("  ·  no Hard leaders in " + roadTitle + " yet"));
            fit(g, line, rx0 + 6, y + 4, labelEnd - (rx0 + 6), MUTED, 1, Align.LEFT);
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
            text(g, Component.literal(chip), rx0 + 7.5f, y + 3, WHITE, 1, true);

            String meta = "Tier " + r.tier() + "  ·  cap " + r.levelCap();
            float metaW = font.width(meta) * 0.75f;
            text(g, Component.literal(meta), rx1 - 4 - metaW, y + 4.5f, MUTED, 0.75f, false);
            if (r.hard()) {
                // Beaten on Hard
                float tagW = font.width("HARD") * 0.75f + 6;
                float tx1 = rx1 - 8 - metaW, tx0 = tx1 - tagW;
                g.fill((int) tx0, y + 2, (int) tx1, y + 11, RED);
                text(g, Component.literal("HARD").withStyle(ChatFormatting.BOLD), (tx0 + tx1) / 2f, y + 4, WHITE, 0.75f, true);
                metaW += tagW + 4;
            }

            MutableComponent name = r.name().copy();
            if (r.status() == TrainerCardData.NEXT) name.withStyle(ChatFormatting.BOLD);
            fit(g, name, rx0 + 17, y + 3, rx1 - 10 - metaW - (rx0 + 17), r.status() == TrainerCardData.LATER ? MUTED : INK, 1, Align.LEFT);
            y += 14;
            i++;
        }
        contentHeight = y - CY0 + 2;
    }

    // Trainers --------------------------------------------------------

    private static final int TRAINER_ROW = 12;

    private void drawTrainers(GuiGraphics g, int mx, int my) {
        trainerHeaders.clear();
        int y = CY0 + 2;
        int contentMouseY = my + (int) scroll;
        boolean mouseInView = confirming == null && my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;
        int x0 = CX0 + 2, x1 = CX1 - 6;

        // Title, with the button that hides beaten trainers
        String label = hideBeaten ? "Show beaten" : "Hide beaten";
        int bw = font.width(label) + 12;
        int bx1 = x1, bx0 = bx1 - bw, by0 = y, by1 = y + 13;
        boolean hover = mouseInView && mx >= bx0 && mx < bx1 && contentMouseY >= by0 && contentMouseY < by1;
        g.fill(bx0, by0, bx1, by1, FRAME_DARK);
        g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
        text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, WHITE, 1, true);
        hideBeatenToggle = new PickButton(bx0, by0, bx1, by1, null);
        fit(g, Component.literal("Wild Trainers").withStyle(ChatFormatting.BOLD), x0 + 2, y + 3, bx0 - 8 - (x0 + 2), INK, 1, Align.LEFT);
        y += 17;

        int columns = 2;
        int cellW = (x1 - x0) / columns;
        for (TrainerGroup group : data.wildTrainers()) {
            // Region header: click to fold its list away
            boolean collapsed = collapsedTrainerGroups.contains(group.title());
            boolean headerHover = mouseInView && mx >= x0 && mx < x1 && contentMouseY >= y && contentMouseY < y + 13;
            g.fill(x0, y, x1, y + 13, headerHover ? MID : DARK);
            String count = group.beaten() + " / " + group.trainers().size() + " beaten";
            float countW = font.width(count) * 0.75f;
            text(g, Component.literal(count), x1 - 5 - countW, y + 4.5f, LIGHT, 0.75f, false);
            float titleW = fit(g, Component.literal((collapsed ? "▶ " : "▼ ") + group.title()).withStyle(ChatFormatting.BOLD),
                    x0 + 5, y + 3, x1 - 11 - countW - (x0 + 5), WHITE, 1, Align.LEFT);
            int barX0 = Math.max(x0 + 90, (int) (x0 + 5 + titleW + 8)), barX1 = (int) (x1 - 11 - countW);
            bar(g, barX0, y + 4, barX1, y + 9, group.trainers().isEmpty() ? 0 : group.beaten() / (float) group.trainers().size(), GREEN);
            trainerHeaders.add(new PickButton(x0, y, x1, y + 13, new Pending(group.title(), group.title(), 0)));
            y += 15;
            if (collapsed) continue;

            List<WildTrainer> shown = hideBeaten
                    ? group.trainers().stream().filter(t -> !t.beaten()).toList()
                    : group.trainers();
            if (shown.isEmpty()) {
                String none = group.trainers().isEmpty() ? "None listed" : "Every trainer here is beaten!";
                text(g, Component.literal(none), x0 + 5, y + 2, MUTED, 1, false);
                y += 15;
                continue;
            }
            int rows = (shown.size() + columns - 1) / columns;
            for (int i = 0; i < shown.size(); i++) {
                int row = i / columns, col = i % columns;
                int cy = y + row * TRAINER_ROW;
                // Only rows inside the view are drawn: a region lists hundreds of trainers.
                if (cy + TRAINER_ROW < CY0 + scroll || cy > CY1 + scroll) continue;
                WildTrainer t = shown.get(i);
                int cx = x0 + col * cellW, cx1 = cx + cellW - (col == columns - 1 ? 0 : 2);
                g.fill(cx, cy, cx1, cy + TRAINER_ROW - 1, row % 2 == 0 ? LIGHT : SCREEN);
                g.fill(cx + 2, cy + 1, cx + 11, cy + 10, t.beaten() ? GREEN : GREY);
                text(g, Component.literal(t.beaten() ? "✔" : "•"), cx + 6.5f, cy + 2, WHITE, 1, true);
                String lv = t.minLevel() == t.maxLevel() ? "Lv " + t.minLevel() : "Lv " + t.minLevel() + "–" + t.maxLevel();
                float lvW = font.width(lv) * 0.75f;
                text(g, Component.literal(lv), cx1 - 3 - lvW, cy + 3.5f, MUTED, 0.75f, false);
                fit(g, Component.literal(t.name()), cx + 14, cy + 2, cx1 - 6 - lvW - (cx + 14), t.beaten() ? INK : MUTED, 1, Align.LEFT);
            }
            y += rows * TRAINER_ROW + 4;
        }
        y += paragraph(g, Component.literal("Wild trainers you beat count here, weakest first. A trainer you can meet again "
                        + "at higher levels counts once, whichever time you beat them. Wins from before this list was added "
                        + "don't count: beat those trainers again when you meet them."),
                CX0 + 4, y + 1, CX1 - CX0 - 14, 40, MUTED, 0.75f, Align.LEFT) + 3;
        contentHeight = y - CY0 + 2;
    }

    // Regions ---------------------------------------------------------

    private void drawRegions(GuiGraphics g, int mx, int my) {
        pickButtons.clear();
        int y = CY0 + 2;
        int contentMouseY = my + (int) scroll;
        boolean mouseInView = my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;

        if (data.canPickStart()) {
            String first = data.regionTitle();
            int bx0 = CX0 + 2, bx1 = CX1 - 6;
            List<FormattedCharSequence> lines = font.split(Component.literal("Pick a second region to work through alongside "
                    + first + ": another league with its own gym leaders, or a region without gyms that grows with your badges. "
                    + "Its Pokémon can spawn for you right away. You only get one pick."), bx1 - bx0 - 12);
            int by1 = y + 16 + lines.size() * 10;
            g.fill(bx0, y, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, y + 1, bx1 - 1, by1 - 1, DARK);
            text(g, Component.literal("CHOOSE YOUR STARTING REGION").withStyle(ChatFormatting.BOLD), bx0 + 6, y + 5, GOLD, 0.75f, false);
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
            panel(g, rx0, y, rx1, ry1);
            if (locked && !r.choosable()) g.fill(rx0 + 1, y + 1, rx1 - 1, ry1 - 1, SCREEN);

            MutableComponent title = Component.literal(r.title()).withStyle(ChatFormatting.BOLD);
            if (r.startPick()) title.append(Component.literal(" ★").withStyle(s -> s.withColor(GOLD)));
            int titleColour = locked && !r.choosable() ? GREY : INK;

            int rightEdge;
            if (r.choosable()) {
                // A "Pick" button instead of the region's state
                String label = "Pick";
                int bw = font.width(label) + 16;
                int bx1 = rx1 - 3, bx0 = bx1 - bw, by0 = y + 3, by1 = ry1 - 3;
                boolean hover = mouseInView && confirming == null
                        && mx >= bx0 && mx < bx1 && contentMouseY >= by0 && contentMouseY < by1;
                g.fill(bx0, by0, bx1, by1, FRAME_DARK);
                g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
                text(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (bx0 + bx1) / 2f, by0 + 3, WHITE, 1, true);
                pickButtons.add(new PickButton(bx0, by0, bx1, by1, new Pending(r.id(), r.title(), PickRegionPayload.EXTRA)));
                rightEdge = bx0 - 6;
                String note = "second region";
                float noteW = font.width(note) * 0.75f;
                text(g, Component.literal(note), rightEdge - noteW, y + 6.5f, MUTED, 0.75f, false);
                fit(g, title, rx0 + 6, y + 5, rightEdge - noteW - 6 - (rx0 + 6), titleColour, 1, Align.LEFT);
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
                float stateW = fit(g, Component.literal(state), rx1 - 6, y + 5, (rx1 - rx0) / 2f, stateColour, 1, Align.RIGHT);
                int stateX0 = (int) (rx1 - 6 - stateW);
                float titleW = fit(g, title, rx0 + 6, y + 5, stateX0 - 8 - (rx0 + 6), titleColour, 1, Align.LEFT);
                if (!locked) {
                    float fraction = r.state() == TrainerCardData.COMPLETE ? 1 : r.maxTier() == 0 ? 0 : r.tier() / (float) r.maxTier();
                    bar(g, Math.max(rx0 + 80, (int) (rx0 + 6 + titleW + 8)), y + 7, stateX0 - 8, y + 12, fraction,
                            r.state() == TrainerCardData.COMPLETE ? GOLD : DARK);
                }
            }
            y += 21;
        }
        if (data.regions().stream().anyMatch(RegionEntry::startPick)) {
            text(g, Component.literal("★ your starting pick"), CX0 + 4, y + 2, MUTED, 0.75f, false);
            y += 10;
        }
        contentHeight = y - CY0 + 2;
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
        fit(g, Component.literal(title).withStyle(ChatFormatting.BOLD), (BOX_X0 + BOX_X1) / 2f, BOX_Y0 + 4,
                BOX_X1 - BOX_X0 - 10, WHITE, 1, Align.CENTRE);
        int ty = BOX_Y0 + 21;
        paragraph(g, Component.literal(body), (BOX_X0 + BOX_X1) / 2f, ty, BOX_X1 - BOX_X0 - 20, BTN_Y0 - 4 - ty, INK, 1, Align.CENTRE);
        button(g, YES_X0, yesX1(), verb + confirming.title(), RED, mx, my);
        button(g, noX0(), NO_X1, "Cancel", MID, mx, my);
        g.pose().popPose();
    }

    private void button(GuiGraphics g, int x0, int x1, String label, int colour, int mx, int my) {
        boolean hover = mx >= x0 && mx < x1 && my >= BTN_Y0 && my < BTN_Y1;
        g.fill(x0, BTN_Y0, x1, BTN_Y1, FRAME_DARK);
        g.fill(x0 + 1, BTN_Y0 + 1, x1 - 1, BTN_Y1 - 1, hover ? LIGHT : colour);
        fit(g, Component.literal(label).withStyle(ChatFormatting.BOLD), (x0 + x1) / 2f, BTN_Y0 + 3, x1 - x0 - 6,
                hover ? INK : WHITE, 1, Align.CENTRE);
    }

    // Wild Pokémon ----------------------------------------------------

    private void drawWild(GuiGraphics g, int mx, int my) {
        int columns = 3;
        int cellW = (CX1 - CX0 - 8) / columns;
        int y = CY0 + 2;
        boolean mouseInView = my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;
        int contentMouseY = my + (int) scroll;

        for (WildGroup group : data.wild()) {
            int hx0 = CX0 + 2, hx1 = CX1 - 6;
            g.fill(hx0, y, hx1, y + 13, DARK);
            String count = group.species().size() + " species" + (group.upcoming() > 0 ? "  ·  +" + group.upcoming() + " next tier" : "");
            float countW = font.width(count) * 0.75f;
            text(g, Component.literal(count), hx1 - 5 - countW, y + 4.5f, LIGHT, 0.75f, false);
            fit(g, Component.literal(group.title()).withStyle(ChatFormatting.BOLD), hx0 + 5, y + 3,
                    hx1 - 11 - countW - (hx0 + 5), WHITE, 1, Align.LEFT);
            y += 15;

            if (group.species().isEmpty()) {
                text(g, Component.literal("None yet"), hx0 + 5, y + 2, MUTED, 1, false);
                y += 13;
                continue;
            }
            int col = 0;
            for (WildSpecies s : group.species()) {
                Species sp = PokemonSpecies.getByIdentifier(s.id());
                int cx = hx0 + col * cellW;
                boolean hover = mouseInView && mx >= cx && mx < cx + cellW - 2 && contentMouseY >= y && contentMouseY < y + 12;
                if (hover) {
                    g.fill(cx, y, cx + cellW - 2, y + 12, LIGHT);
                    hovered = sp != null ? s : null;
                }
                dexIcon(g, cx + 2, y + 2, s.knowledge());
                Component name = sp != null ? sp.getTranslatedName() : Component.literal(s.id().getPath());
                if (!s.prefix().isEmpty()) name = Component.literal(s.prefix() + " ").append(name);
                int colour = s.knowledge() == TrainerCardData.UNKNOWN ? MUTED : INK;
                fit(g, name, cx + 13, y + 2, cellW - 16, colour, 1, Align.LEFT);
                col++;
                if (col == columns) {
                    col = 0;
                    y += 12;
                }
            }
            if (col != 0) y += 12;
            y += 4;
        }
        text(g, Component.literal("Ball: caught   ·   Outline: seen   ·   Faded: not yet seen"), CX0 + 4, y + 1, MUTED, 0.75f, false);
        y += 10;
        contentHeight = y - CY0 + 2;
    }

    /** Cobblemon's Pokédex caught/seen ball, drawn at 8x8. Its texture has the seen outline on top, the caught ball below. */
    private void dexIcon(GuiGraphics g, int x, int y, int knowledge) {
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

    private List<Component> speciesTooltip(WildSpecies s) {
        Species sp = PokemonSpecies.getByIdentifier(s.id());
        List<Component> lines = new ArrayList<>();
        if (sp == null) return lines;
        // A regional form has its own types (Alolan Rattata is Dark/Normal).
        FormData form = s.form().isEmpty() ? null : sp.getFormByName(s.form());
        MutableComponent name = s.prefix().isEmpty() ? Component.empty() : Component.literal(s.prefix() + " ");
        lines.add(name.append(sp.getTranslatedName()).withStyle(ChatFormatting.BOLD)
                .append(Component.literal("  #" + String.format("%04d", sp.getNationalPokedexNumber())).withStyle(ChatFormatting.GRAY)));
        MutableComponent types = Component.empty();
        types.append(typeName(form != null ? form.getPrimaryType() : sp.getPrimaryType()));
        ElementalType second = form != null ? form.getSecondaryType() : sp.getSecondaryType();
        if (second != null) types.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY)).append(typeName(second));
        lines.add(types);
        return lines;
    }

    private static Component typeName(ElementalType type) {
        return type.getDisplayName().copy().withStyle(s -> s.withColor(type.getHue()));
    }

    // Text --------------------------------------------------------------

    /** The smallest text is shrunk to before it's cut short with "…". */
    private static final float MIN_SCALE = 0.65f;
    private static final int LINE_HEIGHT = 10;

    private enum Align { LEFT, CENTRE, RIGHT }

    /** Draws text at a scale, optionally centred on x. */
    private void text(GuiGraphics g, Component text, float x, float y, int colour, float scale, boolean centred) {
        draw(g, text.getVisualOrderText(), x, y, colour, scale, centred ? Align.CENTRE : Align.LEFT);
    }

    /** Draws text at a scale, with x as its left edge, centre or right edge. */
    private void draw(GuiGraphics g, FormattedCharSequence text, float x, float y, int colour, float scale, Align align) {
        float w = font.width(text) * scale;
        float dx = switch (align) {
            case LEFT -> 0;
            case CENTRE -> -w / 2;
            case RIGHT -> -w;
        };
        g.pose().pushPose();
        g.pose().translate(x + dx, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }

    /**
     * Draws one line in at most maxWidth: at maxScale if it fits, otherwise shrunk until it does (down to
     * MIN_SCALE, staying centred on the full-size line), and only then cut short with "…". Returns the drawn width.
     */
    private float fit(GuiGraphics g, Component text, float x, float y, float maxWidth, int colour, float maxScale, Align align) {
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
    private float paragraph(GuiGraphics g, Component text, float x, float y, float maxWidth, float maxHeight,
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
        return font.getSplitter().splitLines(text, Math.max(1, (int) width), Style.EMPTY);
    }

    /** The text cut to fit width (in unscaled font units), ending in "…". */
    private FormattedCharSequence ellipsize(FormattedText text, float width) {
        FormattedText cut = font.substrByWidth(text, Math.max(0, (int) width - font.width("…")));
        return Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("…")));
    }
}
