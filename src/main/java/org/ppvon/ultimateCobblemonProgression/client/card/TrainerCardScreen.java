package org.ppvon.ultimateCobblemonProgression.client.card;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.ppvon.ultimateCobblemonProgression.common.card.PickRegionPayload;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RegionEntry;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RoadEntry;
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
        PROFILE("Profile"), ROAD("Road"), REGIONS("Regions"), WILD("Wild Pokémon");
        final String label;
        Tab(String label) { this.label = label; }
    }

    /** The tab last looked at, so the card reopens there (e.g. after picking a region). */
    private static Tab lastTab = Tab.PROFILE;

    private final TrainerCardData data;
    private Tab tab;
    private double scroll;
    private int contentHeight;
    private float scale = 1;
    private int left, top;
    /** Species under the mouse on the Wild tab, for the tooltip. */
    private Species hovered;
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

    /** A region pick to confirm, with its {@link PickRegionPayload} kind (home, next or extra region). */
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
        return data.regionMode() ? List.of(Tab.values()) : List.of(Tab.PROFILE, Tab.WILD);
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
        scroll = Mth.clamp(scroll - scrollY * 14, 0, Math.max(0, contentHeight - (CY1 - CY0)));
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
            case ROAD -> scrolled(g, () -> drawRoad(g));
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
        text(g, Component.literal(next ? "CHOOSE YOUR NEXT REGION" : "CHOOSE YOUR STARTING REGION").withStyle(ChatFormatting.BOLD),
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
            text(g, Component.literal(c.title()).withStyle(ChatFormatting.BOLD), (px0 + px1) / 2f, y0 + 4, WHITE, 1.25f, true);

            int ty = y0 + 26;
            text(g, Component.literal(c.trainers() + " trainers to beat"), px0 + 6, ty, INK, 0.75f, false);
            text(g, Component.literal(c.species() + " species"), px0 + 6, ty + 10, INK, 0.75f, false);
            text(g, Component.literal("First leader:"), px0 + 6, ty + 24, MUTED, 0.75f, false);
            String first = font.plainSubstrByWidth(c.firstTrainer().getString(), (int) ((panelW - 12) / 0.85f));
            text(g, Component.literal(first).withStyle(ChatFormatting.BOLD), px0 + 6, ty + 33, INK, 0.85f, false);

            int bx0 = px0 + 8, bx1 = px1 - 8, by0 = py1 - 22, by1 = py1 - 8;
            boolean hover = confirming == null && mx >= bx0 && mx < bx1 && my >= by0 && my < by1;
            g.fill(bx0, by0, bx1, by1, FRAME_DARK);
            g.fill(bx0 + 1, by0 + 1, bx1 - 1, by1 - 1, hover ? 0xFFE8574F : RED);
            text(g, Component.literal(next ? "Travel here" : "Start here").withStyle(ChatFormatting.BOLD),
                    (bx0 + bx1) / 2f, by0 + 3, WHITE, 1, true);
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
        List<FormattedCharSequence> foot = font.split(Component.literal(next
                ? "You join the region's trainer series, and your level cap starts over with its first leader."
                : "Later, you can also pick an optional second region to explore, on the card's Regions tab."),
                next ? W - 130 : W - 80);
        for (FormattedCharSequence line : foot) {
            g.pose().pushPose();
            g.pose().translate(W / 2f - font.width(line) * 0.75f / 2, fy, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, line, 0, 0, MUTED, false);
            g.pose().popPose();
            fy += 8;
        }
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
            text(g, label, x + w / 2f, 23, selected ? WHITE : INK, 1, true);
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
        text(g, Component.literal(data.player()).withStyle(ChatFormatting.BOLD), 57, 110, INK, 1, true);
        text(g, Component.literal(data.regionMode() ? data.regionTitle() : "Trainer"), 57, 121, MUTED, 1, true);

        int x0 = 98, x1 = CX1;
        int y = 40;
        if (data.regionMode()) {
            row(g, x0, x1, y, "Region", Component.literal(data.regionTitle()).withStyle(ChatFormatting.BOLD), INK);
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
        int hintY = by0 + 15;
        if (hasNext) {
            text(g, Component.literal("▶ ").append(data.next()).withStyle(ChatFormatting.BOLD), 31, by0 + 14, WHITE, 1.25f, false);
            hintY = by0 + 29;
        }
        for (FormattedCharSequence line : font.split(data.hint(), CX1 - 34)) {
            if (hintY > by1 - 9) break;
            g.drawString(font, line, 31, hintY, LIGHT, false);
            hintY += 10;
        }
    }

    private void row(GuiGraphics g, int x0, int x1, int y, String label, Component value, int valueColour) {
        panel(g, x0, y, x1, y + 15);
        text(g, Component.literal(label), x0 + 5, y + 4, MUTED, 1, false);
        int w = font.width(value);
        g.drawString(font, value, x1 - 5 - w, y + 4, valueColour, false);
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

    private void drawRoad(GuiGraphics g) {
        int y = CY0 + 2;
        if (data.road().isEmpty()) {
            text(g, Component.literal("No trainers are listed for " + data.regionTitle() + "."), (CX0 + CX1) / 2f, y + 20, MUTED, 1, true);
            contentHeight = 0;
            return;
        }
        text(g, Component.literal(data.regionTitle() + " Road").withStyle(ChatFormatting.BOLD), CX0 + 4, y, INK, 1, false);
        String summary = data.beaten() + " / " + data.road().size() + " beaten";
        g.drawString(font, summary, CX1 - 8 - font.width(summary), y, MUTED, false);
        y += 13;

        int i = 0;
        for (RoadEntry r : data.road()) {
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

            MutableComponent name = r.name().copy();
            if (r.status() == TrainerCardData.NEXT) name.withStyle(ChatFormatting.BOLD);
            g.drawString(font, name, rx0 + 17, y + 3, r.status() == TrainerCardData.LATER ? MUTED : INK, false);

            String meta = "Tier " + r.tier() + "  ·  cap " + r.levelCap();
            text(g, Component.literal(meta), rx1 - 4 - font.width(meta) * 0.75f, y + 4.5f, MUTED, 0.75f, false);
            y += 14;
            i++;
        }
        contentHeight = y - CY0 + 2;
    }

    // Regions ---------------------------------------------------------

    private void drawRegions(GuiGraphics g, int mx, int my) {
        pickButtons.clear();
        int y = CY0 + 2;
        int contentMouseY = my + (int) scroll;
        boolean mouseInView = my >= CY0 && my < CY1 && mx >= CX0 && mx < CX1;

        if (data.canPickStart()) {
            String first = data.regions().isEmpty() ? "" : data.regions().get(0).title();
            int bx0 = CX0 + 2, bx1 = CX1 - 6;
            List<FormattedCharSequence> lines = font.split(Component.literal("Pick one more region to explore from the start, alongside "
                    + first + ". Its Pokémon can spawn for you right away. You only get one pick."), bx1 - bx0 - 12);
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
            g.drawString(font, title, rx0 + 6, y + 5, locked && !r.choosable() ? GREY : INK, false);

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
                String note = "starting choice";
                text(g, Component.literal(note), rightEdge - font.width(note) * 0.75f, y + 6.5f, MUTED, 0.75f, false);
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
                int stateW = font.width(state);
                g.drawString(font, state, rx1 - 6 - stateW, y + 5, stateColour, false);
                if (!locked) {
                    float fraction = r.state() == TrainerCardData.COMPLETE ? 1 : r.maxTier() == 0 ? 0 : r.tier() / (float) r.maxTier();
                    bar(g, rx0 + 80, y + 7, rx1 - 14 - stateW, y + 12, fraction,
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
            default -> "Explore ";
        };
        String title = verb + confirming.title() + "?";
        String body = switch (confirming.kind()) {
            case PickRegionPayload.HOME -> "Your journey begins in " + confirming.title() + " and you join its trainer series. This choice is permanent.";
            case PickRegionPayload.NEXT -> "Your journey continues in " + confirming.title() + " and you join its trainer series. This choice is permanent.";
            default -> confirming.title() + " opens now, alongside your first region. This choice is permanent.";
        };
        text(g, Component.literal(title).withStyle(ChatFormatting.BOLD), (BOX_X0 + BOX_X1) / 2f, BOX_Y0 + 4, WHITE, 1, true);
        int ty = BOX_Y0 + 21;
        for (FormattedCharSequence line : font.split(Component.literal(body), BOX_X1 - BOX_X0 - 20)) {
            // Centred by hand: drawCenteredString always adds a drop shadow, which smears dark text on the light box.
            g.drawString(font, line, (BOX_X0 + BOX_X1) / 2 - font.width(line) / 2, ty, INK, false);
            ty += 10;
        }
        button(g, YES_X0, yesX1(), verb + confirming.title(), RED, mx, my);
        button(g, noX0(), NO_X1, "Cancel", MID, mx, my);
        g.pose().popPose();
    }

    private void button(GuiGraphics g, int x0, int x1, String label, int colour, int mx, int my) {
        boolean hover = mx >= x0 && mx < x1 && my >= BTN_Y0 && my < BTN_Y1;
        g.fill(x0, BTN_Y0, x1, BTN_Y1, FRAME_DARK);
        g.fill(x0 + 1, BTN_Y0 + 1, x1 - 1, BTN_Y1 - 1, hover ? LIGHT : colour);
        Component text = Component.literal(font.plainSubstrByWidth(label, x1 - x0 - 6)).withStyle(ChatFormatting.BOLD);
        this.text(g, text, (x0 + x1) / 2f, BTN_Y0 + 3, hover ? INK : WHITE, 1, true);
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
            g.drawString(font, Component.literal(group.title()).withStyle(ChatFormatting.BOLD), hx0 + 5, y + 3, WHITE, false);
            String count = group.species().size() + " species" + (group.upcoming() > 0 ? "  ·  +" + group.upcoming() + " next tier" : "");
            text(g, Component.literal(count), hx1 - 5 - font.width(count) * 0.75f, y + 4.5f, LIGHT, 0.75f, false);
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
                    hovered = sp;
                }
                dexIcon(g, cx + 2, y + 2, s.knowledge());
                Component name = sp != null ? sp.getTranslatedName() : Component.literal(s.id().getPath());
                int colour = s.knowledge() == TrainerCardData.UNKNOWN ? MUTED : INK;
                g.drawString(font, font.plainSubstrByWidth(name.getString(), cellW - 16), cx + 13, y + 2, colour, false);
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

    /** Cobblemon's Pokédex caught/seen ball, drawn at 8x8. */
    private void dexIcon(GuiGraphics g, int x, int y, int knowledge) {
        RenderSystem.enableBlend();
        if (knowledge == TrainerCardData.UNKNOWN) g.setColor(1, 1, 1, 0.3f);
        int v = knowledge == TrainerCardData.SEEN ? 14 : 0;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(8 / 14f, 8 / 14f, 1);
        g.blit(DEX_ICON, 0, 0, 0, v, 14, 14, 14, 28);
        g.pose().popPose();
        g.setColor(1, 1, 1, 1);
    }

    private List<Component> speciesTooltip(Species sp) {
        List<Component> lines = new ArrayList<>();
        lines.add(sp.getTranslatedName().copy().withStyle(ChatFormatting.BOLD)
                .append(Component.literal("  #" + String.format("%04d", sp.getNationalPokedexNumber())).withStyle(ChatFormatting.GRAY)));
        MutableComponent types = Component.empty();
        types.append(typeName(sp.getPrimaryType()));
        ElementalType second = sp.getSecondaryType();
        if (second != null) types.append(Component.literal(" / ").withStyle(ChatFormatting.GRAY)).append(typeName(second));
        lines.add(types);
        return lines;
    }

    private static Component typeName(ElementalType type) {
        return type.getDisplayName().copy().withStyle(s -> s.withColor(type.getHue()));
    }

    // Text --------------------------------------------------------------

    /** Draws text at a scale, optionally centred on x. */
    private void text(GuiGraphics g, Component text, float x, float y, int colour, float scale, boolean centred) {
        g.pose().pushPose();
        float w = font.width(text) * scale;
        g.pose().translate(centred ? x - w / 2 : x, y, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, text, 0, 0, colour, false);
        g.pose().popPose();
    }
}
