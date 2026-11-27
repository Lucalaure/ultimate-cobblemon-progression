package org.ppvon.ultimateCobblemonProgression.client.gui.layout;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screenshots of the Trainer Card for the README, with a Kanto journey as a player would see it: Brock beaten on Hard,
 * Misty on Normal, Lt. Surge next. Started with {@code -Ducp.uiCheck=showcase}; draws each tab once at the largest GUI
 * scale the card fits at, saves it with a transparent background, cropped to the card, to {@code ucp-showcase/}, and
 * closes the game.
 *
 * The data mirrors the built-in leagues datapack (trainer names, ace levels, tier species, wild trainers); update it
 * here if those change.
 */
public final class UiShowcase {
    private static final Logger LOG = LogUtils.getLogger();
    /** The card's size in GUI units, with the margin the screen keeps around it (see TrainerCardScreen#init). */
    private static final int CARD_W = 345 + 8, CARD_H = 207 + 8;

    private static boolean running;
    private static int frames;

    private UiShowcase() {}

    static boolean running() {
        return running;
    }

    /**
     * Called after each title screen frame. The first sets the GUI scale; the shots are taken on a later frame, once
     * the game has set up its projection for the new scale.
     */
    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (frames++ == 0) {
            int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
            int scale = 1;
            while (w / (scale + 1) >= CARD_W && h / (scale + 1) >= CARD_H) scale++;
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
            return;
        }
        if (frames < 3) return;
        running = true;
        try {
            shoot(mc);
        } finally {
            running = false;
        }
        mc.stop();
    }

    private static void shoot(Minecraft mc) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("ucp-showcase");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOG.error("Couldn't create {}", dir, e);
            return;
        }
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();

        Screen choice = new TrainerCardScreen(choiceCard());
        choice.init(mc, w, h);
        save(mc, choice, dir.resolve("trainer-card-choose-region.png"));

        Map<String, String> tabs = Map.of(
                "Profile", "trainer-card-profile.png",
                "Road", "trainer-card-road.png",
                "Trainers", "trainer-card-trainers.png",
                "Regions", "trainer-card-regions.png",
                "Wild Pokémon", "trainer-card-wild.png");
        TrainerCardScreen card = new TrainerCardScreen(kantoCard());
        card.init(mc, w, h);
        card.sweepStates(state -> {
            if (!state.endsWith(", page 1") || state.contains("other league")) return;
            String file = tabs.get(state.substring(0, state.length() - ", page 1".length()));
            if (file != null) save(mc, card, dir.resolve(file));
        });
        LOG.info("[UCP showcase] Screenshots in {}", dir.toAbsolutePath());
    }

    /** Draws the screen on black and on white, works out each pixel's transparency from the two, and crops to the card. */
    private static void save(Minecraft mc, Screen screen, Path file) {
        try (NativeImage black = draw(mc, screen, 0); NativeImage white = draw(mc, screen, 1)) {
            int w = black.getWidth(), h = black.getHeight();
            int x0 = w, y0 = h, x1 = -1, y1 = -1;
            int[] out = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int b = black.getPixelRGBA(x, y), wh = white.getPixelRGBA(x, y);
                    int spread = 0;
                    for (int s = 0; s < 24; s += 8) spread += ((wh >> s) & 0xFF) - ((b >> s) & 0xFF);
                    int alpha = Math.max(0, Math.min(255, 255 - Math.round(spread / 3f)));
                    if (alpha == 0) continue;
                    int px = alpha << 24;
                    for (int s = 0; s < 24; s += 8) {
                        int c = Math.min(255, Math.round(((b >> s) & 0xFF) * 255f / alpha));
                        px |= c << s;
                    }
                    out[y * w + x] = px;
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
            if (x1 < 0) {
                LOG.warn("[UCP showcase] {} drew nothing", file.getFileName());
                return;
            }
            try (NativeImage cropped = new NativeImage(x1 - x0 + 1, y1 - y0 + 1, true)) {
                for (int y = y0; y <= y1; y++) {
                    for (int x = x0; x <= x1; x++) cropped.setPixelRGBA(x - x0, y - y0, out[y * w + x]);
                }
                cropped.writeToFile(file);
            }
        } catch (IOException e) {
            LOG.warn("[UCP showcase] Couldn't save {}", file, e);
        }
    }

    private static NativeImage draw(Minecraft mc, Screen screen, float background) {
        mc.getMainRenderTarget().bindWrite(true);
        RenderSystem.clearColor(background, background, background, 1);
        RenderSystem.clear(16640, Minecraft.ON_OSX);
        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        screen.renderWithTooltip(g, -1000, -1000, 0);
        g.flush();
        return Screenshot.takeScreenshot(mc.getMainRenderTarget());
    }

    // ------------------------------------------------------------------ the journey shown

    private static TrainerCardData choiceCard() {
        List<RegionChoice> choices = List.of(
                new RegionChoice("kanto", "Kanto", 13, 151, Component.literal("Leader Brock")),
                new RegionChoice("sinnoh", "Sinnoh", 13, 130, Component.literal("Gym Leader Roark")),
                new RegionChoice("galar", "Galar", 14, 108, Component.literal("Gym Leader Milo")));
        return new TrainerCardData("Red", true, "Kanto", 1, 10, 15, 0, 0, Component.empty(), Component.empty(), false,
                List.of(), List.of(), List.of(), choices, false, "", false, false, List.of(), List.of(), false);
    }

    private static TrainerCardData kantoCard() {
        // Kanto's league: the tier each trainer unlocks and the level of their ace.
        Object[][] league = {
                {2, 14, "Leader Brock"}, {3, 21, "Leader Misty"}, {4, 24, "Leader Lt. Surge"}, {5, 29, "Leader Erika"},
                {6, 36, "Leader Koga"}, {7, 43, "Leader Sabrina"}, {8, 47, "Leader Blaine"}, {9, 50, "Leader Giovanni"},
                {10, 54, "Elite Four Lorelei"}, {10, 56, "Elite Four Bruno"}, {10, 58, "Elite Four Agatha"},
                {10, 60, "Elite Four Lance"}, {10, 63, "Champion Blue"}};
        List<RoadEntry> road = new ArrayList<>();
        for (int i = 0; i < league.length; i++) {
            int status = i < 2 ? TrainerCardData.BEATEN : i == 2 ? TrainerCardData.NEXT : TrainerCardData.LATER;
            road.add(new RoadEntry((int) league[i][0], (int) league[i][1], Component.literal((String) league[i][2]), status, i == 0));
        }

        List<RegionEntry> regions = new ArrayList<>();
        regions.add(new RegionEntry("kanto", "Kanto", TrainerCardData.OPEN, 3, 10, false, false, 0));
        // Every other region can still be picked as the second region (Sinnoh and Galar as a second league).
        for (String r : new String[]{"Johto", "Hoenn", "Sinnoh", "Unova", "Kalos", "Alola", "Galar", "Paldea"}) {
            regions.add(new RegionEntry(r.toLowerCase(), r, TrainerCardData.LOCKED, 0, 10, false, true, 0));
        }

        // Kanto's tiers 1 to 3, as the Wild Pokémon tab lists them: caught, seen, or not yet seen.
        String[] caught = {"caterpie", "metapod", "weedle", "kakuna", "pidgey", "rattata", "spearow", "ekans", "nidoranf",
                "nidoranm", "zubat", "magikarp", "geodude", "abra", "charmander", "mankey", "machop", "sandshrew", "vulpix",
                "squirtle", "pikachu", "oddish"};
        String[] seen = {"jigglypuff", "paras", "diglett", "doduo", "bellsprout", "poliwag", "meowth", "venonat", "horsea",
                "psyduck", "gastly", "krabby", "goldeen", "magnemite"};
        String[][] tiers = {
                {"caterpie", "metapod", "weedle", "kakuna", "pidgey", "rattata", "spearow", "ekans", "nidoranf", "nidoranm",
                        "jigglypuff", "zubat", "paras", "diglett", "ditto", "magikarp"},
                {"charmander", "vulpix", "abra", "bellsprout", "doduo", "dratini", "geodude", "horsea", "machop", "mankey",
                        "meowth", "poliwag", "sandshrew", "shellder", "venonat"},
                {"bulbasaur", "squirtle", "pikachu", "clefairy", "oddish", "psyduck", "slowpoke", "magnemite", "grimer",
                        "gastly", "krabby", "exeggcute", "cubone", "goldeen", "eevee"}};
        List<String> caughtList = List.of(caught), seenList = List.of(seen);
        List<WildSpecies> species = new ArrayList<>();
        for (String[] tier : tiers) {
            for (String id : tier) {
                int knowledge = caughtList.contains(id) ? TrainerCardData.CAUGHT : seenList.contains(id) ? TrainerCardData.SEEN
                        : TrainerCardData.UNKNOWN;
                species.add(new WildSpecies(ResourceLocation.fromNamespaceAndPath("cobblemon", id), knowledge));
            }
        }
        List<WildGroup> wild = List.of(new WildGroup("Kanto", 15, species));

        // Kanto's weakest wild trainers (FireRed's), with their level range across rematches.
        Object[][] wildTrainers = {
                {"Rival Terry", 5, 25, true}, {"Bug Catcher Rick", 6, 6, true}, {"Bug Catcher Charlie", 7, 7, true},
                {"Bug Catcher Doug", 7, 7, true}, {"Bug Catcher Anthony", 8, 8, true}, {"Bug Catcher Greg", 9, 9, false},
                {"Bug Catcher Sammy", 9, 9, true}, {"Lass Janice", 9, 28, true}, {"Bug Catcher Cale", 10, 10, false},
                {"Bug Catcher Robby", 10, 10, true}, {"Hiker Marcos", 10, 10, true}, {"Lass Sally", 10, 10, false},
                {"Youngster Josh", 10, 10, true}, {"Bug Catcher Colton", 10, 54, false}, {"Bug Catcher James", 11, 11, false},
                {"Bug Catcher Kent", 11, 11, true}, {"Camper Liam", 11, 11, false}, {"Lass Miriam", 11, 11, false},
                {"Super Nerd Jovan", 11, 11, false}, {"Youngster Ben", 11, 48, true}, {"Lass Ali", 12, 12, false},
                {"Super Nerd Miguel", 12, 12, false}, {"Hiker Nob", 13, 13, false}, {"Lass Haley", 13, 13, false},
                {"Camper Flint", 14, 14, false}, {"Camper Shane", 14, 14, false}};
        List<WildTrainer> trainers = new ArrayList<>();
        for (Object[] t : wildTrainers) trainers.add(new WildTrainer((String) t[0], (int) t[1], (int) t[2], (boolean) t[3]));
        // Kanto has 447 wild trainers; the rest aren't beaten yet.
        for (int i = trainers.size(); i < 447; i++) trainers.add(new WildTrainer("Trainer", 15 + i / 10, 15 + i / 10, false));

        int seenCount = caught.length + seen.length;
        return new TrainerCardData("Red", true, "Kanto", 3, 10, 24, seenCount, caught.length,
                Component.literal("Leader Lt. Surge"), Component.literal("Use a Gym Challenge block in a village gym."), false,
                road, regions, wild, List.of(), false, "Normal", true, false,
                List.of(new TrainerGroup("Kanto", trainers)), List.of(), true);
    }
}
