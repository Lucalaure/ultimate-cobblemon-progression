package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.api.UCPApi;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.progression.TierStanding;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionSpecies;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierEntry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Optional rule ({@code gymLegalityCheck}): the Pokémon a player brings to a gym battle must be ones they could have
 * by now. Eggs, trades, fossils and other mods can hand a player a Pokémon of a region, or a tier, they haven't
 * unlocked; this keeps those out of gym battles.
 *
 * <p>What counts as "could have" is {@code gymLegalityRule}:
 * <ul>
 *   <li>{@code "region"} (the default): the Pokémon is from a region the player has opened, so anything they could
 *       catch there and evolve is fine. A regional form counts as its own region's: a Kanto Pikachu evolved into an
 *       Alolan Raichu needs Alola. Without regions (the tier ladder), the Pokémon or one of its pre-evolutions must be
 *       from a tier the player has unlocked.</li>
 *   <li>{@code "strict"}: the Pokémon itself must be one they could meet in the wild now: its own tier is unlocked
 *       (in region mode, in its region, see {@link TierStanding}). Evolutions of higher tiers aren't allowed yet.</li>
 * </ul>
 * Under both rules the Pokémon must also be at or below the player's level cap, when one is known. Pokémon without a
 * region or tier are always allowed, and so is the player's starter, since Cobblemon offers starters from every
 * region. Rematches aren't checked: they don't change progress.
 *
 * <ul>
 *   <li>{@code "off"} (the default): no check.</li>
 *   <li>{@code "warn"}: the leader names the Pokémon when it accepts the challenge, but the battle goes ahead.</li>
 *   <li>{@code "block"}: the same warning, and the battle doesn't start while one is in the party.</li>
 * </ul>
 */
public final class GymLegality {
    public enum Mode { OFF, WARN, BLOCK }

    public enum Rule { REGION, STRICT }

    /** Why a Pokémon can't join a gym battle, in the order a message names them. */
    enum Problem {
        REGION("from a region you've opened"),
        TIER("from a tier you've unlocked yet"),
        LEVEL("within your level cap");

        /** Completes "isn't …" / "aren't …". */
        final String phrase;

        Problem(String phrase) {
            this.phrase = phrase;
        }
    }

    /** How many Pokémon a message names before it says "and N more". */
    private static final int MAX_NAMED = 3;

    private GymLegality() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, GymLegality::onBattleStart);
    }

    public static Mode mode() {
        String value = ConfigLoader.GYM_LEGALITY_CHECK.get();
        if ("block".equalsIgnoreCase(value)) return Mode.BLOCK;
        if ("warn".equalsIgnoreCase(value)) return Mode.WARN;
        return Mode.OFF;
    }

    public static Rule rule() {
        return "strict".equalsIgnoreCase(ConfigLoader.GYM_LEGALITY_RULE.get()) ? Rule.STRICT : Rule.REGION;
    }

    /** Whether the player could have this Pokémon by now (see the class comment). */
    public static boolean isLegal(ServerPlayer player, Pokemon pokemon) {
        return problem(player, pokemon, levelCap(player)) == null;
    }

    /** Why the Pokémon can't join a gym battle, or null if it can. {@code cap} is the player's level cap, or -1. */
    static Problem problem(ServerPlayer player, Pokemon pokemon, int cap) {
        if (pokemon == null || isStarter(player, pokemon)) return null;
        Problem origin = originProblem(player, pokemon);
        if (origin != null) return origin;
        return cap > 0 && pokemon.getLevel() > cap ? Problem.LEVEL : null;
    }

    private static Problem originProblem(ServerPlayer player, Pokemon pokemon) {
        if (!TierStanding.hasTierData()) return null;
        Species species = pokemon.getSpecies();
        var aspects = pokemon.getAspects();
        if (!TierStanding.isKnown(species, aspects)) return null;

        if (rule() == Rule.STRICT) {
            return TierStanding.of(player, species, aspects).allowed() ? null : Problem.TIER;
        }
        if (RegionRegistry.isActive()) {
            TierEntry entry = RegionSpecies.entry(species, aspects);
            return RegionProgressTracker.get(player).isUnlocked(entry.region()) ? null : Problem.REGION;
        }
        // The tier ladder has no regions: allow anything the player could catch and evolve.
        int playerTier = Math.max(0, TrainerLevelComponents.KEY.get(player).getLevel());
        Set<Species> seen = new HashSet<>();
        for (Species s = species; s != null && seen.add(s); s = preEvolution(s)) {
            int tier = UCPApi.getTier(s);
            if (tier > 0 && tier <= playerTier) return null;
        }
        return Problem.TIER;
    }

    private static Species preEvolution(Species species) {
        var pre = species.getPreEvolution();
        return pre != null ? pre.getSpecies() : null;
    }

    /**
     * The player's level cap, or -1 if there isn't one: in region mode the trainer mod's (Radical Cobblemon Trainers),
     * otherwise their tier's, when the level cap is on.
     */
    static int levelCap(ServerPlayer player) {
        if (RegionRegistry.isActive()) {
            return RctBridge.isLoaded() ? RctBridge.levelCap(player) : -1;
        }
        if (!ConfigLoader.DO_LEVEL_CAP.get() || TierRegistry.maxTier() <= 0) return -1;
        try {
            return TierRegistry.getCapForTier(Math.max(0, TrainerLevelComponents.KEY.get(player).getLevel()));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    /** The Pokémon in the player's party that can't join a gym battle, by problem, in party order. */
    static Map<Problem, List<Pokemon>> problems(ServerPlayer player, Iterable<Pokemon> team) {
        int cap = levelCap(player);
        Map<Problem, List<Pokemon>> out = new LinkedHashMap<>();
        for (Problem p : Problem.values()) out.put(p, new ArrayList<>());
        for (Pokemon pokemon : team) {
            Problem p = problem(player, pokemon, cap);
            if (p != null) out.get(p).add(pokemon);
        }
        out.values().removeIf(List::isEmpty);
        return out;
    }

    /** The Pokémon in the player's party that can't join a gym battle, in party order. */
    public static List<Pokemon> outOfReach(ServerPlayer player) {
        List<Pokemon> out = new ArrayList<>();
        int cap = levelCap(player);
        for (Pokemon pokemon : PlayerExtensionsKt.party(player)) {
            if (pokemon != null && problem(player, pokemon, cap) != null) out.add(pokemon);
        }
        return out;
    }

    /** The line a challenger is told when a leader accepts, or null if the check is off or the party is fine. */
    public static Component hint(ServerPlayer player, String leaderName) {
        Mode mode = mode();
        if (mode == Mode.OFF) return null;
        List<Pokemon> party = new ArrayList<>();
        for (Pokemon pokemon : PlayerExtensionsKt.party(player)) {
            if (pokemon != null) party.add(pokemon);
        }
        Map<Problem, List<Pokemon>> problems = problems(player, party);
        if (problems.isEmpty()) return null;
        int count = problems.values().stream().mapToInt(List::size).sum();
        String text = mode == Mode.BLOCK
                ? describe(problems) + ". " + leaderName + " won't battle you until you put "
                        + (count == 1 ? "it" : "them") + " in the PC."
                : describe(problems) + ". " + leaderName + " will battle you anyway.";
        return Component.literal(text).withStyle(mode == Mode.BLOCK ? ChatFormatting.RED : ChatFormatting.YELLOW);
    }

    private static Unit onBattleStart(BattleStartedEvent.Pre event) {
        if (mode() != Mode.BLOCK || event.isCanceled()) return Unit.INSTANCE;

        for (BattleActor actor : event.getBattle().getActors()) {
            if (!(actor instanceof EntityBackedBattleActor<?> backed)) continue;
            Entity leader = backed.getEntity();
            if (leader == null || !leader.getTags().contains(GymChallengeBlockEntity.LEADER_TAG)) continue;
            if (leader.getTags().contains(GymRematch.REMATCH_TAG)) continue;

            for (BattleActor other : event.getBattle().getActors()) {
                if (!(other instanceof PlayerBattleActor playerActor) || other.getSide() == actor.getSide()) continue;
                ServerPlayer player = playerActor.getEntity();
                if (player == null) continue;

                List<Pokemon> team = new ArrayList<>();
                for (BattlePokemon battlePokemon : other.getPokemonList()) team.add(battlePokemon.getOriginalPokemon());
                Map<Problem, List<Pokemon>> problems = problems(player, team);
                if (problems.isEmpty()) continue;

                int count = problems.values().stream().mapToInt(List::size).sum();
                MutableComponent reason = Component.literal(leader.getDisplayName().getString() + " won't battle yet: "
                        + describe(problems) + ". Put " + (count == 1 ? "it" : "them") + " in the PC first.");
                event.setReason(reason);
                event.cancel();
                return Unit.INSTANCE;
            }
        }
        return Unit.INSTANCE;
    }

    private static boolean isStarter(ServerPlayer player, Pokemon pokemon) {
        try {
            UUID starter = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player).getStarterUUID();
            return starter != null && starter.equals(pokemon.getUuid());
        } catch (RuntimeException e) {
            // Player data not loaded (e.g. a fake player): don't hold the battle up over it.
            return false;
        }
    }

    /** "Raichu isn't from a region you've opened, and Snorlax isn't within your level cap". */
    private static String describe(Map<Problem, List<Pokemon>> problems) {
        Map<Problem, List<String>> named = new LinkedHashMap<>();
        problems.forEach((p, list) -> {
            List<String> names = new ArrayList<>();
            for (Pokemon pokemon : list) names.add(pokemon.getDisplayName(false).getString());
            named.put(p, names);
        });
        return describeNames(named);
    }

    /** Builds the sentence {@link #describe} gives, from names. */
    static String describeNames(Map<Problem, List<String>> problems) {
        List<String> parts = new ArrayList<>();
        problems.forEach((p, names) ->
                parts.add(joinNames(names) + " " + (names.size() == 1 ? "isn't " : "aren't ") + p.phrase));
        return String.join(", and ", parts);
    }

    /** Joins names: "Piplup", "Piplup and Eevee", "Piplup, Eevee and Riolu", or "Piplup, Eevee, Riolu and 2 more". */
    static String joinNames(List<String> names) {
        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);
        if (names.size() > MAX_NAMED) {
            return String.join(", ", names.subList(0, MAX_NAMED)) + " and " + (names.size() - MAX_NAMED) + " more";
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }
}
