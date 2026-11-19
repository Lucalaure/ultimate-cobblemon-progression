package org.ppvon.ultimateCobblemonProgression.common.card;

import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RegionChoice;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RegionEntry;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.RoadEntry;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildGroup;
import org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.WildSpecies;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.gym.RctBridge;
import org.ppvon.ultimateCobblemonProgression.common.progression.dex.DexProgressionApi;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionCommands;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionSpecies;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierCatches;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.UltimateCobblemonProgressionTierExt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.ppvon.ultimateCobblemonProgression.common.card.TrainerCardData.*;

/** Works out a player's Trainer Card on the server. */
final class TrainerCardDataBuilder {
    private TrainerCardDataBuilder() {}

    static TrainerCardData build(ServerPlayer player) {
        return RegionRegistry.isActive() ? regionMode(player) : ladderMode(player);
    }

    // ------------------------------------------------------------------ region mode

    private static TrainerCardData regionMode(ServerPlayer player) {
        RegionProgress progress = RegionProgressTracker.get(player);
        List<RegionDef> regions = RegionRegistry.ordered();

        // Show the journey's current region; otherwise the first open, unfinished one; otherwise the last region
        // the journey reached (e.g. while the player picks where to go next).
        List<RegionDef> nextChoices = RegionProgress.nextChoices(player);
        int focus = progress.current();
        if (focus < 0) {
            for (int r : progress.priorityOrder()) {
                if (progress.isUnlocked(r) && !progress.isCompleted(r)) {
                    focus = r;
                    break;
                }
            }
        }
        boolean allDone = focus < 0 && nextChoices.isEmpty();
        if (focus < 0) {
            List<Integer> journey = progress.journey();
            focus = journey.isEmpty() ? regions.size() - 1 : journey.get(journey.size() - 1);
        }

        RegionDef region = regions.get(focus);
        int step = progress.step(focus);
        List<RoadEntry> road = road(player, region, step, allDone || progress.isCompleted(focus));

        Component next = road.stream().filter(r -> r.status() == NEXT).map(RoadEntry::name).findFirst().orElse(Component.empty());
        // catchWholeTier: what's left to catch in the current tier before the next leader.
        List<Species> toCatch = progress.isCompleted(focus) || step >= region.maxTier()
                ? List.of() : TierCatches.missing(player, region, step);
        Component hint;
        if (allDone) {
            hint = Component.literal("Every region is complete. Congratulations, Champion!");
        } else if (!nextChoices.isEmpty()) {
            hint = Component.literal(region.title + " is complete! Choose where your journey goes next: reopen your Trainer Card.");
        } else if (!toCatch.isEmpty()) {
            hint = Component.literal("First catch every tier " + step + " Pokémon. " + toCatch.size() + " to go ("
                    + TierCatches.targets(region, step) + " in all): " + TierCatches.names(toCatch, 6)
                    + ". The Wild Pokémon tab shows which you have.");
        } else if (!next.getString().isEmpty()) {
            hint = Component.literal("Use a Gym Challenge block in a village gym.");
        } else {
            TierDef nextTier = region.tiers.get(step + 1);
            hint = nextTier != null
                    ? Component.literal("Tier " + (step + 1) + ": " + RegionCommands.describeMissing(player, region, nextTier))
                    : Component.empty();
        }

        String startPick = RegionProgress.chosenStartRegion(player);
        List<RegionDef> choices = RegionProgress.openableStartChoices(player);
        List<RegionEntry> regionEntries = new ArrayList<>();
        List<WildGroup> wild = new ArrayList<>();
        AbstractPokedexManager dex = PlayerExtensionsKt.pokedex(player);
        for (int r = 0; r < regions.size(); r++) {
            RegionDef reg = regions.get(r);
            int state = !progress.isUnlocked(r) ? LOCKED : progress.isCompleted(r) ? COMPLETE : OPEN;
            regionEntries.add(new RegionEntry(reg.id, reg.title, state, progress.step(r), reg.maxTier(),
                    reg.id.equals(startPick), choices.contains(reg)));

            if (state == LOCKED) continue;
            int regionIndex = r;
            int regionStep = progress.step(r);
            List<WildSpecies> species = species(dex, sp -> RegionSpecies.region(sp) == regionIndex
                    && RegionSpecies.regionTier(sp) >= 1 && RegionSpecies.regionTier(sp) <= regionStep);
            int upcoming = regionStep < reg.maxTier() ? reg.realSpecies(regionStep + 1) : 0;
            wild.add(new WildGroup(reg.title, upcoming, species));
        }

        boolean choosingNext = !RegionProgress.needsHomeRegion(player);
        List<RegionChoice> regionChoices = new ArrayList<>();
        for (RegionDef reg : choosingNext ? nextChoices : RegionRegistry.homeChoices()) {
            int trainers = reg.tiers.values().stream().mapToInt(t -> t.trainers.size()).sum();
            int speciesCount = 0;
            for (int t = 1; t <= reg.maxTier(); t++) speciesCount += reg.realSpecies(t);
            Component first = reg.tiers.values().stream().filter(t -> !t.trainers.isEmpty()).findFirst()
                    .map(t -> trainerName(t.trainers.get(0).get(0))).orElse(Component.empty());
            regionChoices.add(new RegionChoice(reg.id, reg.title, trainers, speciesCount, first));
        }

        var counts = DexProgressionApi.get(player);
        return new TrainerCardData(player.getGameProfile().getName(), true, region.title, step, region.maxTier(),
                levelCap(player, region, step), counts.seen(), counts.caught(), next, hint, allDone,
                road, regionEntries, wild, regionChoices, choosingNext);
    }

    /** The trainers listed in a region's tiers, with the player's status against each. */
    private static List<RoadEntry> road(ServerPlayer player, RegionDef region, int step, boolean completed) {
        List<RoadEntry> road = new ArrayList<>();
        boolean nextFound = completed;
        for (TierDef tier : region.tiers.values()) {
            for (List<String> group : tier.trainers) {
                // Trainers of unlocked tiers count as beaten: switching RCT series (travelling to the next region)
                // can reset RCT's progress in the old one, and without RCT there's nothing to ask.
                boolean beaten = completed || tier.index <= step;
                if (!beaten && RctBridge.isLoaded()) {
                    for (String id : group) {
                        if (RctBridge.defeats(player, id) > 0) {
                            beaten = true;
                            break;
                        }
                    }
                }
                int status;
                if (beaten) status = BEATEN;
                else if (!nextFound && tier.index == step + 1) {
                    status = NEXT;
                    nextFound = true;
                } else status = LATER;
                road.add(new RoadEntry(tier.index, region.levelCap(tier.index), trainerName(group.get(0)), status));
            }
        }
        return road;
    }

    private static int levelCap(ServerPlayer player, RegionDef region, int step) {
        if (RctBridge.isLoaded()) {
            int cap = RctBridge.levelCap(player);
            if (cap > 0) return cap;
        }
        return region.levelCap(Math.max(1, step));
    }

    // ------------------------------------------------------------------ trainer-level ladder

    private static TrainerCardData ladderMode(ServerPlayer player) {
        int level = TrainerLevelComponents.KEY.get(player).getLevel();
        Component hint = TierRegistry.get(level + 1).map(next -> {
            StringBuilder sb = new StringBuilder("Level " + (level + 1) + ": ");
            if (next.requirements.dex.seen > 0) sb.append("see ").append(next.requirements.dex.seen).append(" species");
            if (next.requirements.dex.caught > 0) {
                if (next.requirements.dex.seen > 0) sb.append(", ");
                sb.append("catch ").append(next.requirements.dex.caught).append(" species");
            }
            if (!next.requirements.hasAny()) sb.append("no requirements");
            return (Component) Component.literal(sb.toString());
        }).orElse(Component.literal("Max trainer level!"));

        AbstractPokedexManager dex = PlayerExtensionsKt.pokedex(player);
        List<WildSpecies> species = species(dex, sp -> {
            int tier = ((UltimateCobblemonProgressionTierExt) (Object) sp).ucp$getTier();
            return tier >= 1 && tier <= level;
        });
        int upcoming = TierRegistry.get(level + 1).isPresent() ? TierRegistry.getRealSpecies(level + 1) : 0;

        var counts = DexProgressionApi.get(player);
        return new TrainerCardData(player.getGameProfile().getName(), false, "Trainer level", level, TierRegistry.maxTier(),
                TierRegistry.getCapForTier(level), counts.seen(), counts.caught(), Component.empty(), hint,
                TierRegistry.get(level + 1).isEmpty(), List.of(), List.of(),
                List.of(new WildGroup("Your level", upcoming, species)), List.of(), false);
    }

    // ------------------------------------------------------------------ shared

    private static List<WildSpecies> species(AbstractPokedexManager dex, Predicate<Species> allowed) {
        List<Species> list = new ArrayList<>();
        for (Species sp : PokemonSpecies.INSTANCE.getSpecies()) {
            if (sp.getImplemented() && allowed.test(sp)) list.add(sp);
        }
        list.sort(Comparator.comparingInt(Species::getNationalPokedexNumber));
        return list.stream().map(sp -> new WildSpecies(sp.getResourceIdentifier(), knowledge(dex, sp))).toList();
    }

    private static int knowledge(AbstractPokedexManager dex, Species sp) {
        if (dex == null) return UNKNOWN;
        PokedexEntryProgress p = dex.getKnowledgeForSpecies(sp.getResourceIdentifier());
        return p == PokedexEntryProgress.OWNED ? CAUGHT : p == PokedexEntryProgress.SEEN ? SEEN : UNKNOWN;
    }

    /** RCT's translated trainer name, falling back to one made from the id. */
    private static Component trainerName(String trainerId) {
        return Component.translatableWithFallback("trainer.rctmod." + trainerId + ".name", plainName(trainerId));
    }

    /** "leader_lt_surge_01a0" becomes "Leader Lt Surge". */
    static String plainName(String trainerId) {
        String base = trainerId.replaceFirst("_[0-9a-f]{4}$", "");
        StringBuilder sb = new StringBuilder();
        for (String word : base.split("_")) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
