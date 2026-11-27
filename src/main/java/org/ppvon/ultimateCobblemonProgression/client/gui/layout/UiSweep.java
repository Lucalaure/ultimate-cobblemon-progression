package org.ppvon.ultimateCobblemonProgression.client.gui.layout;

import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.client.card.TrainerCardScreen;
import org.ppvon.ultimateCobblemonProgression.client.gui.CommonConfigScreen;
import org.ppvon.ultimateCobblemonProgression.client.gui.ConfigHubScreen;
import org.ppvon.ultimateCobblemonProgression.client.gui.SpawnConfigScreen;
import org.ppvon.ultimateCobblemonProgression.client.gym.GymChoiceScreen;
import org.ppvon.ultimateCobblemonProgression.client.gym.RematchScreen;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.*;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymChoice;
import org.ppvon.ultimateCobblemonProgression.common.gym.RematchPayloads;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Draws every UCP screen at every common window size and GUI scale, with ordinary and deliberately long text, and
 * runs the {@link UiCheck layout check} on each. Started with {@code -Ducp.uiCheck=run} (or {@code =exit} to close
 * the game afterwards, as {@code gradlew runUiCheck} does) once the title screen shows; the report goes to
 * {@code ucp-ui-check.txt} in the game folder.
 *
 * A new screen is checked by adding it to {@link #cases()}; a screen with tabs or pages implements {@link UiSweepable}
 * so each of them is drawn.
 */
public final class UiSweep {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String MODE = System.getProperty("ucp.uiCheck", "");
    private static boolean started, running;

    private UiSweep() {}

    public static boolean running() {
        return running || UiShowcase.running();
    }

    /**
     * Called after each screen draws: runs the sweep the first time the title screen has drawn, or with
     * {@code -Ducp.uiCheck=showcase}, takes the README's {@link UiShowcase screenshots}.
     */
    public static void maybeStart(Screen screen) {
        if (MODE.equals("showcase") && screen instanceof TitleScreen) {
            UiShowcase.tick();
            return;
        }
        if (started || !(screen instanceof TitleScreen) || !(MODE.equals("run") || MODE.equals("exit"))) return;
        started = true;
        run();
    }

    /** A screen to check, made fresh for each size. */
    private record Case(String name, Supplier<Screen> screen) {}

    private static void run() {
        Minecraft mc = Minecraft.getInstance();
        running = true;
        // problem -> where it was seen
        Map<String, Set<String>> found = new LinkedHashMap<>();
        int draws = 0;
        // Screenshots at the window's own GUI size, the one size the framebuffer can show.
        int[] real = {mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()};
        List<int[]> sizes = sizes();
        if (sizes.stream().noneMatch(sz -> sz[0] == real[0] && sz[1] == real[1])) sizes.add(0, real);
        Path shots = FabricLoader.getInstance().getGameDir().resolve("ucp-ui-check");
        try {
            Files.createDirectories(shots);
            try (var old = Files.list(shots)) {
                for (Path f : old.toList()) Files.deleteIfExists(f);
            }
        } catch (IOException e) {
            LOG.warn("Couldn't clear the UI check screenshots", e);
        }
        try {
            for (Case c : cases()) {
                for (int[] size : sizes) {
                    String where = size[0] + "x" + size[1];
                    Path shotDir = size[0] == real[0] && size[1] == real[1] ? shots : null;
                    Screen screen;
                    try {
                        screen = c.screen.get();
                        screen.init(mc, size[0], size[1]);
                    } catch (RuntimeException e) {
                        found.computeIfAbsent(c.name + ": couldn't open: " + e, k -> new LinkedHashSet<>()).add(where);
                        continue;
                    }
                    List<String> states = new ArrayList<>();
                    final Screen s = screen;
                    if (s instanceof UiSweepable sweepable) {
                        sweepable.sweepStates(state -> {
                            states.add(state);
                            record(c.name + " [" + state + "]", where, s, found, shotDir);
                        });
                    } else {
                        record(c.name, where, s, found, shotDir);
                        states.add("");
                        // Scrolling lists (the config screens): every page, not only the first.
                        for (var child : s.children()) {
                            if (!(child instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> list)) continue;
                            for (int page = 2; list.getScrollAmount() < list.getMaxScroll() && page < 40; page++) {
                                list.setScrollAmount(list.getScrollAmount() + Math.max(20, list.getHeight() - 20));
                                record(c.name + " [scrolled, page " + page + "]", where, s, found, shotDir);
                                states.add("page " + page);
                            }
                        }
                    }
                    draws += states.size();
                }
            }
        } finally {
            running = false;
        }

        StringBuilder report = new StringBuilder();
        report.append("UCP UI check: ").append(found.size()).append(" problem").append(found.size() == 1 ? "" : "s")
                .append(" in ").append(draws).append(" drawn screens.\n");
        report.append("Sizes are the GUI size (window size divided by GUI scale).\n\n");
        for (var e : found.entrySet()) {
            report.append("- ").append(e.getKey()).append("\n    at ").append(String.join(", ", e.getValue())).append("\n");
            String example = examples.get(e.getKey());
            if (example != null) report.append("    e.g. ").append(example).append("\n");
        }
        report.append("\nScreenshots at ").append(real[0]).append("x").append(real[1]).append(", problems outlined in red: ")
                .append(shots.toAbsolutePath()).append("\n");
        Path file = FabricLoader.getInstance().getGameDir().resolve("ucp-ui-check.txt");
        try {
            Files.writeString(file, report);
        } catch (IOException e) {
            LOG.error("Couldn't write the UI check report", e);
        }
        LOG.info("[UCP UI check] {} problems in {} drawn screens; report: {}", found.size(), draws, file.toAbsolutePath());
        if (MODE.equals("exit")) mc.stop();
    }

    /** The first full message (with positions) of each problem, for the report. */
    private static final Map<String, String> examples = new LinkedHashMap<>();

    private static void record(String name, String where, Screen screen, Map<String, Set<String>> found, Path shotDir) {
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        if (shotDir != null) {
            mc.getMainRenderTarget().bindWrite(true);
            com.mojang.blaze3d.systems.RenderSystem.clearColor(0.12f, 0.12f, 0.14f, 1);
            com.mojang.blaze3d.systems.RenderSystem.clear(16640, Minecraft.ON_OSX);
        }
        UiCheck.begin(screen);
        List<UiCheck.Problem> problems;
        try {
            screen.renderWithTooltip(g, -1000, -1000, 0);
            g.flush();
            if (shotDir != null) {
                // Outline what the check found, so the screenshot shows it.
                List<UiCheck.Problem> shown = UiCheck.end();
                UiCheck.outline(g, shown);
                g.flush();
                String file = name.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_|_$", "") + ".png";
                try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    image.writeToFile(shotDir.resolve(file));
                } catch (IOException e) {
                    LOG.warn("Couldn't save the UI check screenshot {}", file, e);
                }
                UiCheck.restore(shown);
            }
        } catch (RuntimeException e) {
            found.computeIfAbsent(name + ": drawing failed: " + e, k -> new LinkedHashSet<>()).add(where);
            LOG.error("[UCP UI check] {} failed to draw at {}", name, where, e);
        } finally {
            problems = UiCheck.end();
        }
        for (UiCheck.Problem p : problems) {
            // Positions differ per size; leave them out so one problem is listed once.
            String message = p.message().replaceAll(" \\(-?\\d+,-?\\d+ \\d+x\\d+\\)", "");
            found.computeIfAbsent(name + ": " + message, k -> new LinkedHashSet<>()).add(where);
            examples.putIfAbsent(name + ": " + message, where + ": " + p.message());
        }
    }

    /** GUI sizes for common window sizes at every GUI scale the game allows for them, smallest first. */
    static List<int[]> sizes() {
        int[][] windows = {{854, 480}, {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}};
        Set<String> seen = new LinkedHashSet<>();
        List<int[]> sizes = new ArrayList<>();
        for (int[] w : windows) {
            for (int scale = 1; w[0] / scale >= 320 && w[1] / scale >= 240; scale++) {
                int gw = (int) Math.ceil(w[0] / (double) scale), gh = (int) Math.ceil(w[1] / (double) scale);
                if (seen.add(gw + "x" + gh)) sizes.add(new int[]{gw, gh});
            }
        }
        sizes.sort((a, b) -> a[0] * a[1] - b[0] * b[1]);
        return sizes;
    }

    // ------------------------------------------------------------------ the screens

    private static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("Config hub", () -> new ConfigHubScreen(null)));
        cases.add(new Case("General config", () -> new CommonConfigScreen(new ConfigHubScreen(null))));
        cases.add(new Case("Spawn config", () -> new SpawnConfigScreen(new ConfigHubScreen(null))));

        cases.add(new Case("Trainer card (typical)", () -> new TrainerCardScreen(card(false))));
        cases.add(new Case("Trainer card (long names)", () -> new TrainerCardScreen(card(true))));
        cases.add(new Case("Trainer card (trainer levels)", () -> new TrainerCardScreen(levelCard())));
        cases.add(new Case("Trainer card (first region choice)", () -> new TrainerCardScreen(choiceCard(false, false))));
        cases.add(new Case("Trainer card (next region choice, long)", () -> new TrainerCardScreen(choiceCard(true, true))));
        cases.add(new Case("Trainer card (difficulty choice)", () -> new TrainerCardScreen(difficultyCard())));

        cases.add(new Case("Gym choice", () -> new GymChoiceScreen(BlockPos.ZERO, List.of(
                new GymChoice.Option("Kanto", Component.literal("Leader Brock"), "a", 14, false),
                new GymChoice.Option("Pokémon Unbound Borrius", Component.literal("Gym Leader Mirskle the Grass Specialist"), "b", 100, true)))));
        cases.add(new Case("Rematch", () -> {
            List<RematchPayloads.Entry> entries = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                entries.add(new RematchPayloads.Entry(i % 2 == 0 ? "Kanto" : "Pokémon Unbound Borrius",
                        Component.literal(i % 3 == 0 ? "Gym Leader Mirskle the Grass Specialist of Frozen Heights" : "Leader Misty"),
                        "n" + i, i % 2 == 0 ? "h" + i : ""));
            }
            return new RematchScreen(BlockPos.ZERO, entries);
        }));
        return cases;
    }

    private static String pick(boolean longText, String normal, String lengthy) {
        return longText ? lengthy : normal;
    }

    private static List<RoadEntry> road(boolean longText, int beaten) {
        String[] names = longText
                ? new String[]{"Gym Leader Mirskle the Grass Specialist", "Gym Leader Galavan of Dresco Town", "Elite Four Moleman the Digger",
                "Champion Jax of the Borrius League", "Title Defense Rival (chose Beldum)", "Leader Big Mo of Fallshore City"}
                : new String[]{"Leader Brock", "Leader Misty", "Leader Lt. Surge", "Leader Erika", "Leader Koga", "Leader Sabrina",
                "Leader Blaine", "Leader Giovanni", "Elite Four Lorelei", "Elite Four Bruno", "Elite Four Agatha", "Elite Four Lance",
                "Champion Terry"};
        List<RoadEntry> road = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            int status = i < beaten ? TrainerCardData.BEATEN : i == beaten ? TrainerCardData.NEXT : TrainerCardData.LATER;
            road.add(new RoadEntry(i + 1, 12 + i * 7, Component.literal(names[i]), status, i % 2 == 0 && i < beaten));
        }
        return road;
    }

    private static List<WildSpecies> species(boolean longText) {
        String[] ids = {"bulbasaur", "charmander", "squirtle", "pidgey", "rattata", "crabominable", "fletchinder", "corviknight",
                "stonjourner", "polteageist", "barraskewda", "dracovish"};
        List<WildSpecies> list = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            String prefix = longText && i % 3 == 0 ? "Paldean" : i == 4 ? "Alolan" : "";
            list.add(new WildSpecies(ResourceLocation.fromNamespaceAndPath("cobblemon", ids[i]), prefix.isEmpty() ? "" : "Alola",
                    prefix, i % 3));
        }
        return list;
    }

    private static TrainerCardData card(boolean longText) {
        String region = pick(longText, "Kanto", "Pokémon Unbound Borrius");
        String player = pick(longText, "Dev", "AVeryLongPlayerName_");
        List<RoadEntry> road = road(longText, 3);
        List<RegionEntry> regions = List.of(
                new RegionEntry("kanto", region, TrainerCardData.OPEN, 4, 13, false, false, longText ? 128 : 3),
                new RegionEntry("johto", pick(longText, "Johto", "Northern Galar Wild Area"), TrainerCardData.COMPLETE, 13, 13, true, false, 0),
                new RegionEntry("hoenn", pick(longText, "Hoenn", "Sevii Islands and Beyond"), TrainerCardData.LOCKED, 0, 13, false, true, 0),
                new RegionEntry("sinnoh", "Sinnoh", TrainerCardData.LOCKED, 0, 13, false, false, 0));
        List<WildGroup> wild = List.of(
                new WildGroup(region, longText ? 120 : 4, species(longText)),
                new WildGroup(pick(longText, "Johto", "Northern Galar Wild Area"), 0, List.of()));
        List<WildTrainer> trainers = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            trainers.add(new WildTrainer(longText ? "Ace Trainer Alexandrina-Montgomery " + i : "Youngster Joey", 5 + i, longText ? 95 + i % 5 : 5 + i, i % 2 == 0));
        }
        List<TrainerGroup> wildTrainers = List.of(new TrainerGroup(region, trainers), new TrainerGroup("Johto", List.of()));
        List<LeagueView> others = List.of(new LeagueView(pick(longText, "Galar", "Crown Tundra and Isle of Armor"), 2, 9,
                Component.literal(pick(longText, "Gym Leader Nessa", "Gym Leader Melony of Circhester's Ice Stadium")), road(longText, 1), true));
        Component next = Component.literal(pick(longText, "Leader Erika", "Gym Leader Mirskle the Grass Specialist of Frozen Heights"));
        Component hint = Component.literal(longText
                ? "Catch every Pokémon of tier 4 before facing the next leader: Bulbasaur, Charmander, Squirtle, Pidgey, Rattata, Crabominable, Fletchinder, Corviknight, Stonjourner, Polteageist, Barraskewda, Dracovish and 116 more."
                : "Beat Erika in Celadon City to unlock tier 5.");
        return new TrainerCardData(player, true, region, 4, 13, 36, longText ? 1025 : 120, longText ? 1000 : 64, next, hint, false,
                road, regions, wild, List.of(), false, "Hard", true, false, wildTrainers, others, true);
    }

    private static TrainerCardData levelCard() {
        return new TrainerCardData("Dev", false, "Trainer level", 3, 10, 30, 50, 20, Component.empty(),
                Component.literal("Battle trainers to raise your trainer level."), false, List.of(), List.of(),
                List.of(new WildGroup("Tier 3", 5, species(false))), List.of(), false, "", false, false, List.of(), List.of(), false);
    }

    private static TrainerCardData choiceCard(boolean next, boolean longText) {
        List<RegionChoice> choices = List.of(
                new RegionChoice("kanto", "Kanto", 13, 151, Component.literal("Leader Brock")),
                new RegionChoice("unbound", pick(longText, "Johto", "Pokémon Unbound Borrius"), 16, 400,
                        Component.literal(pick(longText, "Leader Falkner", "Gym Leader Mirskle the Grass Specialist of Frozen Heights"))),
                new RegionChoice("galar", pick(longText, "Galar", "Crown Tundra"), 18, 1025, Component.literal("Gym Leader Milo")));
        return new TrainerCardData("Dev", true, "Kanto", 1, 13, 15, 0, 0, Component.empty(), Component.empty(), false,
                List.of(), List.of(), List.of(), choices, next, "", false, false, List.of(), List.of(), false);
    }

    private static TrainerCardData difficultyCard() {
        return new TrainerCardData("Dev", true, "Kanto", 1, 13, 15, 0, 0, Component.literal("Leader Brock"), Component.empty(), false,
                road(false, 0), List.of(), List.of(), List.of(), false, "Normal", true, true, List.of(), List.of(), true);
    }
}
