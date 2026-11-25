package org.ppvon.ultimateCobblemonProgression.common.starter;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.starter.StarterHandler;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.config.starter.StarterCategory;
import com.cobblemon.mod.common.starter.CobblemonStarterHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Cobblemon's starter choice, following the player's journey.
 *
 * <ul>
 *   <li>With {@code regionStarters} on, in region mode: a player picks their starter only once they've chosen where
 *       their journey starts, and only from that region's starters, plus their second region's if they picked it
 *       first. Picking where the journey starts opens the starter choice (see {@link #offerStarter}).</li>
 *   <li>With {@code starterLevel} above 0: every starter comes at that level (5 by default, as in the original
 *       games), with the moves it knows at that level, instead of the level in Cobblemon's starter list (10).</li>
 * </ul>
 *
 * It extends Cobblemon's own handler, whose screen and choice both read {@link #getStarterList}, so a player can't
 * choose a starter the list doesn't offer them. It's only installed in place of Cobblemon's own handler: when another
 * mod already handles starters, CJP leaves it alone.
 */
public final class RegionStarterHandler extends CobblemonStarterHandler {
    private static boolean warnedNoCategory;

    /** Puts CJP's handler in place of Cobblemon's own, unless another mod replaced it. */
    public static void install() {
        StarterHandler current = Cobblemon.INSTANCE.getStarterHandler();
        if (current instanceof RegionStarterHandler) return;
        if (current.getClass() != CobblemonStarterHandler.class) {
            UltimateCobblemonProgression.LOG.info("Starters are handled by another mod ({}), so CJP's starter rules are off",
                    current.getClass().getName());
            return;
        }
        Cobblemon.INSTANCE.setStarterHandler(new RegionStarterHandler());
    }

    @NotNull
    @Override
    public List<StarterCategory> getStarterList(@NotNull ServerPlayer player) {
        List<StarterCategory> all = super.getStarterList(player);
        List<StarterCategory> shown = all;
        Set<String> regions = journeyRegionNames(player);
        if (!regions.isEmpty()) {
            List<StarterCategory> mine = new ArrayList<>();
            for (StarterCategory category : all) {
                if (matches(category.getName(), category.getDisplayName(), regions)) mine.add(category);
            }
            if (!mine.isEmpty()) {
                shown = mine;
            } else if (!warnedNoCategory) {
                // A region without starters of its own (e.g. from a datapack): offer every starter rather than none.
                warnedNoCategory = true;
                UltimateCobblemonProgression.LOG.warn("No starter category matches the regions {}, so every starter is offered", regions);
            }
        }
        int level = ConfigLoader.STARTER_LEVEL.get();
        return level > 0 ? atLevel(shown, level) : shown;
    }

    @Override
    public void requestStarterChoice(@NotNull ServerPlayer player) {
        if (mustPickRegionFirst(player)) {
            tellPickRegionFirst(player);
            return;
        }
        super.requestStarterChoice(player);
    }

    @Override
    public void chooseStarter(@NotNull ServerPlayer player, @NotNull String categoryName, int selected) {
        if (mustPickRegionFirst(player)) {
            tellPickRegionFirst(player);
            return;
        }
        super.chooseStarter(player, categoryName, selected);
    }

    /**
     * Opens the starter choice for a player who just picked where their journey starts, if they don't have a
     * starter yet. Called after their Trainer Card is sent again, so the starter choice shows on top of it.
     */
    public static void offerStarter(ServerPlayer player) {
        if (!waitsForStarter(player)) return;
        Cobblemon.INSTANCE.getStarterHandler().requestStarterChoice(player);
    }

    /** Whether the player's starter choice waits on their journey: the option is on and they have no starter yet. */
    public static boolean waitsForStarter(ServerPlayer player) {
        if (!ConfigLoader.REGION_STARTERS.get()) return false;
        GeneralPlayerData data = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player);
        return !data.getStarterSelected() && !data.getStarterLocked();
    }

    /** Whether the player has to choose where their journey starts before they can pick a starter. */
    public static boolean mustPickRegionFirst(ServerPlayer player) {
        return ConfigLoader.REGION_STARTERS.get() && RegionProgress.needsHomeRegion(player);
    }

    private static void tellPickRegionFirst(ServerPlayer player) {
        player.displayClientMessage(Component.literal(
                "Choose where your journey starts first: open your Trainer Card. Your starter comes from that region.")
                .withStyle(ChatFormatting.YELLOW), false);
    }

    /**
     * The ids and titles (lowercase) of the regions the player's starter may come from: where their journey starts,
     * and their second region if they picked it. Empty when starters aren't limited (the option is off, region mode
     * isn't on, or no region is picked yet).
     */
    private static Set<String> journeyRegionNames(ServerPlayer player) {
        Set<String> names = new LinkedHashSet<>();
        if (!ConfigLoader.REGION_STARTERS.get() || !RegionRegistry.isActive()) return names;
        for (String id : new String[]{RegionProgress.chosenHomeRegion(player), RegionProgress.chosenStartRegion(player)}) {
            RegionDef region = id != null ? RegionRegistry.byId(id) : null;
            if (region == null) continue;
            names.add(region.id.toLowerCase(Locale.ROOT));
            names.add(region.title.toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /**
     * Whether a starter category belongs to one of these regions (lowercase ids or titles): by its name ("Kanto"),
     * or by the last part of its display name's translation key ("cobblemon.starterselection.category.kanto").
     */
    static boolean matches(String categoryName, String displayName, Set<String> regions) {
        if (categoryName != null && regions.contains(categoryName.toLowerCase(Locale.ROOT))) return true;
        if (displayName == null) return false;
        String key = displayName.toLowerCase(Locale.ROOT);
        return regions.contains(key.substring(key.lastIndexOf('.') + 1));
    }

    /** The categories with every starter at this level. */
    private static List<StarterCategory> atLevel(List<StarterCategory> categories, int level) {
        List<StarterCategory> out = new ArrayList<>(categories.size());
        for (StarterCategory category : categories) {
            List<PokemonProperties> pokemon = new ArrayList<>(category.getPokemon().size());
            for (PokemonProperties properties : category.getPokemon()) {
                PokemonProperties copy = properties.copy();
                copy.setLevel(level);
                pokemon.add(copy);
            }
            out.add(category.copy(category.getName(), category.getOrder(), category.getDisplayName(), pokemon,
                    category.getRandomStarter()));
        }
        return out;
    }
}
