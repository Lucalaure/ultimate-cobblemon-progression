package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager;
import com.cobblemon.mod.common.api.pokedex.FormDexRecord;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code catchWholeTier} difficulty option: in every region, reaching tier N+1 also needs every obtainable
 * Pokémon of tier N caught. In a league this holds the next gym leader back (see GymLeaderPicker); the extra starting
 * region, which has no leaders, simply waits at its tier until it's caught, without ever holding the league back.
 *
 * <p>A tier's Pokémon are its {@link TierEntry entries}: its species, and the regional forms that belong to it. Each is
 * caught on its own, with Cobblemon's per-form Pokédex records: an Alolan Rattata counts for Alola's Alolan Rattata,
 * not for Kanto's Rattata. "Obtainable" means Cobblemon itself spawns it in the wild (one of the spawns bundled in
 * Cobblemon's jar that's still in the live spawn pool; spawns other mods or datapacks add don't count) or it evolves
 * from something that does; anything else, like most legendaries, is skipped so no one gets stuck. Catching, evolving and trading all register a Pokémon as caught in the Pokédex.
 */
public final class TierCatches {
    /** Obtainable species (key "species") and regional forms (key "species/Form"), or null until spawns load. */
    private static Set<String> obtainable;

    private TierCatches() {}

    /** Whether the option applies to this region: it's on (it covers every region). */
    public static boolean applies(RegionDef region) {
        return ConfigLoader.CATCH_WHOLE_TIER.get();
    }

    /** Forgets what can be obtained, e.g. after a datapack reload changed the spawn pools. */
    public static void invalidate() {
        obtainable = null;
    }

    /** The Pokémon of this tier the player still has to catch, in Pokédex order. Empty when the option is off. */
    public static List<TierEntry> missing(ServerPlayer player, RegionDef region, int tier) {
        if (!applies(region)) return List.of();
        return missingAll(player, region, tier);
    }

    /**
     * The Pokémon of this tier the player hasn't caught, in Pokédex order, whether or not the option is on: for the
     * tier catch advancements (see CatchTierTrigger).
     */
    public static List<TierEntry> missingAll(ServerPlayer player, RegionDef region, int tier) {
        if (tier < 1) return List.of();
        AbstractPokedexManager dex = PlayerExtensionsKt.pokedex(player);
        Set<String> canGet = obtainable();
        List<TierEntry> out = new ArrayList<>();
        for (TierEntry entry : region.entries(tier)) {
            if (!counts(entry, canGet)) continue;
            if (!isCaught(dex, entry)) out.add(entry);
        }
        out.sort(Comparator.comparingInt((TierEntry e) -> e.species().getNationalPokedexNumber())
                .thenComparing(TierEntry::isForm));
        return out;
    }

    /**
     * Like {@link #missing}, but counting only the tier's species and any form of them as caught: how CJP counted
     * before regional forms were caught on their own. Used once, to keep the tiers of players from before that.
     */
    public static List<TierEntry> missingSpecies(ServerPlayer player, RegionDef region, int tier) {
        if (!applies(region) || tier < 1) return List.of();
        AbstractPokedexManager dex = PlayerExtensionsKt.pokedex(player);
        Set<String> canGet = obtainable();
        List<TierEntry> out = new ArrayList<>();
        for (TierEntry entry : region.entries(tier)) {
            if (entry.isForm() || !counts(entry, canGet)) continue;
            if (dex == null || dex.getKnowledgeForSpecies(entry.species().getResourceIdentifier()) != PokedexEntryProgress.OWNED) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Whether the spawn pools are loaded, so what's obtainable (and so what's missing) is known. */
    public static boolean isReady() {
        return !obtainable().isEmpty();
    }

    /** How many of this tier's Pokémon count for the option: the obtainable ones. */
    public static int targets(RegionDef region, int tier) {
        Set<String> canGet = obtainable();
        return (int) region.entries(tier).stream().filter(e -> counts(e, canGet)).count();
    }

    /** "Pidgey, Alolan Rattata and 3 more". */
    public static String names(List<TierEntry> entries, int shown) {
        List<String> names = entries.stream().limit(shown).map(e -> e.displayName().getString()).toList();
        String list = String.join(", ", names);
        int rest = entries.size() - names.size();
        return rest > 0 ? list + " and " + rest + " more" : list;
    }

    private static boolean counts(TierEntry entry, Set<String> canGet) {
        return entry.species().getImplemented() && canGet.contains(key(entry));
    }

    /**
     * Whether the player has caught this Pokémon. A regional form needs its own form caught; a species needs any of
     * its other forms caught (a region-bias Pikachu is still a Pikachu).
     */
    public static boolean isCaught(AbstractPokedexManager dex, TierEntry entry) {
        return knowledge(dex, entry) == PokedexEntryProgress.OWNED;
    }

    /** The player's Pokédex knowledge of this Pokémon, worked out per form as in {@link #isCaught}. */
    public static PokedexEntryProgress knowledge(AbstractPokedexManager dex, TierEntry entry) {
        if (dex == null) return PokedexEntryProgress.UNREGISTERED;
        Species sp = entry.species();
        SpeciesDexRecord record = dex.getSpeciesRecord(sp.getResourceIdentifier());
        if (record == null) return PokedexEntryProgress.UNREGISTERED;
        if (entry.isForm()) return formKnowledge(record, entry.form().getName());

        List<TierEntry> regionalForms = RegionSpecies.forms(sp);
        if (regionalForms.isEmpty() || record.isFormRecordsEmpty()) {
            return dex.getKnowledgeForSpecies(sp.getResourceIdentifier());
        }
        PokedexEntryProgress best = PokedexEntryProgress.UNREGISTERED;
        Set<String> names = new HashSet<>();
        names.add(sp.getStandardForm().getName());
        for (FormData form : sp.getForms()) names.add(form.getName());
        for (String name : names) {
            if (RegionSpecies.formByName(sp, name) != null) continue; // a regional form of its own
            PokedexEntryProgress p = formKnowledge(record, name);
            if (p.ordinal() > best.ordinal()) best = p;
        }
        return best;
    }

    private static PokedexEntryProgress formKnowledge(SpeciesDexRecord record, String formName) {
        FormDexRecord form = record.getFormRecord(formName);
        return form != null ? form.getKnowledge() : PokedexEntryProgress.UNREGISTERED;
    }

    private static String key(TierEntry entry) {
        return entry.isForm() ? key(entry.species(), entry.form()) : entry.species().getResourceIdentifier().toString();
    }

    private static String key(Species sp, FormData form) {
        TierEntry regional = form != null ? RegionSpecies.formByName(sp, form.getName()) : null;
        return regional != null
                ? sp.getResourceIdentifier() + "/" + form.getName()
                : sp.getResourceIdentifier().toString();
    }

    private static Set<String> obtainable() {
        if (obtainable == null) {
            Set<String> found = compute();
            if (found.isEmpty()) return found; // the spawn pools aren't loaded yet; try again next time
            obtainable = found;
            UltimateCobblemonProgression.LOG.info("{} species and regional forms can be obtained in the wild or by evolution",
                    found.size());
        }
        return obtainable;
    }

    /** A species and one of its forms. */
    private record Obtained(Species species, FormData form) {}

    private static Set<String> compute() {
        Deque<Obtained> queue = new ArrayDeque<>();
        Set<String> found = new HashSet<>();
        Map<String, String> own = cobblemonSpawns();
        try {
            for (SpawnDetail detail : CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails()) {
                if (detail instanceof PokemonSpawnDetail p) {
                    if (!isCobblemons(own, detail, p.getPokemon())) continue;
                    add(found, queue, p.getPokemon(), null);
                } else if (detail instanceof PokemonHerdSpawnDetail herd) {
                    if (!own.isEmpty() && !own.containsKey(detail.getId())) continue;
                    for (var member : herd.getHerdablePokemon()) add(found, queue, member.getPokemon(), null);
                }
            }
        } catch (RuntimeException e) { // Cobblemon's spawn pool isn't initialised yet
            return Set.of();
        }

        // Anything an obtainable Pokémon evolves into can be obtained too, regional forms included (an Alolan
        // Rattata's Raticate is Alolan, and a Pikachu evolved in an Alola-style biome becomes an Alolan Raichu).
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            Obtained o = queue.poll();
            if (!visited.add(o.species.getResourceIdentifier() + "/" + o.form.getName())) continue;
            Set<Evolution> evolutions = new HashSet<>(o.form.getEvolutions());
            if (evolutions.isEmpty()) evolutions.addAll(o.species.getEvolutions());
            for (Evolution evolution : evolutions) add(found, queue, evolution.getResult(), o);
        }
        return found;
    }

    /**
     * Whether this live spawn is one of Cobblemon's own: its id is in Cobblemon's bundled spawn files, for the same
     * species. Spawns other mods and datapacks add (like a Kubfu spawn) don't count, so a tier never asks for a
     * Pokémon that only another mod provides. If Cobblemon's files couldn't be read, every spawn counts.
     */
    private static boolean isCobblemons(Map<String, String> own, SpawnDetail detail, PokemonProperties props) {
        if (own.isEmpty()) return true;
        String species = own.get(detail.getId());
        if (species == null) return false;
        String live = props.getSpecies();
        if (live == null) return false;
        return path(live).equals(species);
    }

    /** "cobblemon:Rattata" → "rattata". */
    private static String path(String species) {
        int colon = species.indexOf(':');
        return (colon >= 0 ? species.substring(colon + 1) : species).toLowerCase(Locale.ROOT);
    }

    /**
     * The spawns bundled in Cobblemon's own jar ({@code data/cobblemon/spawn_pool_world}): spawn id to species name.
     * Empty if they can't be read.
     */
    private static Map<String, String> cobblemonSpawns() {
        Map<String, String> out = new HashMap<>();
        Optional<Path> root = FabricLoader.getInstance().getModContainer("cobblemon")
                .flatMap(c -> c.findPath("data/cobblemon/spawn_pool_world"));
        if (root.isEmpty()) {
            UltimateCobblemonProgression.LOG.warn("Couldn't find Cobblemon's spawn files; every wild spawn counts as catchable");
            return out;
        }
        try (Stream<Path> files = Files.walk(root.get())) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".json"))::iterator) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    if (!json.has("spawns")) continue;
                    for (JsonElement el : json.getAsJsonArray("spawns")) {
                        JsonObject spawn = el.getAsJsonObject();
                        if (!spawn.has("id")) continue;
                        String id = spawn.get("id").getAsString();
                        String pokemon = spawn.has("pokemon") ? spawn.get("pokemon").getAsString().trim() : "";
                        int space = pokemon.indexOf(' ');
                        out.put(id, path(space >= 0 ? pokemon.substring(0, space) : pokemon));
                    }
                } catch (RuntimeException | IOException e) {
                    UltimateCobblemonProgression.LOG.warn("Couldn't read Cobblemon spawn file {}", file, e);
                }
            }
        } catch (IOException e) {
            UltimateCobblemonProgression.LOG.warn("Couldn't read Cobblemon's spawn files; every wild spawn counts as catchable", e);
            return new HashMap<>();
        }
        return out;
    }

    /**
     * Records the Pokémon these properties describe as obtainable and queues it for its evolutions. Region-bias
     * Pokémon never spawn in region mode, so their spawns are skipped. An evolution keeps the regional aspect of the
     * Pokémon it evolves from unless its result names a form itself.
     */
    private static void add(Set<String> found, Deque<Obtained> queue, PokemonProperties props, Obtained from) {
        Species sp = TrainerLevelInfluence.speciesOf(props.getSpecies());
        if (sp == null) return;
        Set<String> aspects = RegionalForms.aspectsOf(props);
        if (from == null && RegionRegistry.isActive() && RegionSpecies.isRegionBias(aspects)) return;
        if (from != null && aspects.isEmpty()) {
            TierEntry regional = RegionSpecies.formByName(from.species, from.form.getName());
            if (regional != null) aspects.add(regional.aspect());
        }
        FormData form = aspects.isEmpty() ? sp.getStandardForm() : sp.getForm(aspects);
        found.add(key(sp, form));
        queue.add(new Obtained(sp, form));
    }
}
