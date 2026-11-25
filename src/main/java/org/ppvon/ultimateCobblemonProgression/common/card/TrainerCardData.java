package org.ppvon.ultimateCobblemonProgression.common.card;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything a Trainer Card shows, worked out on the server and drawn by the client's TrainerCardScreen.
 *
 * @param regionMode false when UCP runs its single trainer-level ladder; then "region" is the ladder
 * @param next       the next trainer to beat, or empty
 * @param hint       a line under the next challenge (how to unlock the next tier, or a reminder)
 * @param regionChoices regions the card asks the player to pick from before showing anything else: where their
 *                   journey starts, or (with {@code choosingNext}) where it goes next. Empty when there's nothing to pick
 * @param difficulty  the gym difficulty the player's next leader is summoned at ("Normal" or "Hard"), or "" when there are
 *                   no Hard leaders
 * @param difficultyChangeable whether the player can change it (players choose, rather than the server config)
 * @param choosingDifficulty   whether the card first asks the player to pick a difficulty
 * @param wildTrainers  per open region, the Radical Cobblemon Trainers wild trainers it lists and which the player has
 *                   beaten. Empty without RCT or outside region mode
 * @param otherLeagues  with two leagues in progress, the one the card isn't focused on (at most one). Empty otherwise
 * @param hardAvailable whether the card's region has Hard leaders; without them its leaders are always Normal
 */
public record TrainerCardData(
        String player,
        boolean regionMode,
        String regionTitle,
        int tier,
        int maxTier,
        int levelCap,
        int seen,
        int caught,
        Component next,
        Component hint,
        boolean allDone,
        List<RoadEntry> road,
        List<RegionEntry> regions,
        List<WildGroup> wild,
        List<RegionChoice> regionChoices,
        boolean choosingNext,
        String difficulty,
        boolean difficultyChangeable,
        boolean choosingDifficulty,
        List<TrainerGroup> wildTrainers,
        List<LeagueView> otherLeagues,
        boolean hardAvailable
) {
    public static final int BEATEN = 0, NEXT = 1, LATER = 2;
    public static final int LOCKED = 0, OPEN = 1, COMPLETE = 2;
    public static final int UNKNOWN = 0, SEEN = 1, CAUGHT = 2;

    /** Another league in progress: its name, tier, next trainer and road, for the card's Profile and Road tabs. */
    public record LeagueView(String title, int tier, int maxTier, Component next, List<RoadEntry> road, boolean hardAvailable) {
        public long beaten() {
            return road.stream().filter(r -> r.status == BEATEN).count();
        }
    }

    /**
     * A trainer in the region's order.
     *
     * @param tier  the tier beating this trainer unlocks
     * @param level the level of its strongest Pokémon, in the version a gym summons for the player; 0 if unknown
     */
    public record RoadEntry(int tier, int level, Component name, int status, boolean hard) {}

    /**
     * A region and the player's standing in it.
     *
     * @param startPick  the region the player picked to explore from the start
     * @param choosable  the player can still pick this region as their starting region
     * @param toCatch    with catchWholeTier: Pokémon of the current tier still to catch before the next tier opens
     */
    public record RegionEntry(String id, String title, int state, int tier, int maxTier, boolean startPick, boolean choosable,
                              int toCatch) {}

    /** The species allowed to spawn from one region, and how many the next tier adds. */
    public record WildGroup(String title, int upcoming, List<WildSpecies> species) {}

    /**
     * A wild Pokémon on the card: a species, or one of its regional forms.
     *
     * @param form   the regional form's Cobblemon form name ("Alola"), or "" for the species
     * @param prefix shown before the species' name for a regional form ("Alolan"), or ""
     */
    public record WildSpecies(ResourceLocation id, String form, String prefix, int knowledge) {
        public WildSpecies(ResourceLocation id, int knowledge) {
            this(id, "", "", knowledge);
        }
    }

    /** A league region the player can start their journey in, or travel to next, shown on the card's choice screen. */
    public record RegionChoice(String id, String title, int trainers, int species, Component firstTrainer) {}

    /** A region's wild trainers, weakest first. */
    public record TrainerGroup(String title, List<WildTrainer> trainers) {
        public long beaten() {
            return trainers.stream().filter(WildTrainer::beaten).count();
        }
    }

    /**
     * A wild trainer, counted once for all its versions: its name, the level of its strongest Pokémon in its weakest and
     * strongest version (equal with a single version), and whether the player has beaten any version.
     */
    public record WildTrainer(String name, int minLevel, int maxLevel, boolean beaten) {}

    /** Whether the card should first ask the player where their journey starts, or where it goes next. */
    public boolean needsChoice() {
        return !regionChoices.isEmpty();
    }

    /** Whether the player still has a starting region to pick. */
    public boolean canPickStart() {
        return regions.stream().anyMatch(RegionEntry::choosable);
    }

    public long beaten() {
        return road.stream().filter(r -> r.status == BEATEN).count();
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TrainerCardData> CODEC = StreamCodec.of(
            TrainerCardData::write, TrainerCardData::read);

    private static void write(RegistryFriendlyByteBuf buf, TrainerCardData d) {
        buf.writeUtf(d.player);
        buf.writeBoolean(d.regionMode);
        buf.writeUtf(d.regionTitle);
        buf.writeVarInt(d.tier);
        buf.writeVarInt(d.maxTier);
        buf.writeVarInt(d.levelCap);
        buf.writeVarInt(d.seen);
        buf.writeVarInt(d.caught);
        ComponentSerialization.STREAM_CODEC.encode(buf, d.next);
        ComponentSerialization.STREAM_CODEC.encode(buf, d.hint);
        buf.writeBoolean(d.allDone);

        writeRoad(buf, d.road);
        buf.writeVarInt(d.regions.size());
        for (RegionEntry r : d.regions) {
            buf.writeUtf(r.id);
            buf.writeUtf(r.title);
            buf.writeVarInt(r.state);
            buf.writeVarInt(r.tier);
            buf.writeVarInt(r.maxTier);
            buf.writeBoolean(r.startPick);
            buf.writeBoolean(r.choosable);
            buf.writeVarInt(r.toCatch);
        }
        buf.writeVarInt(d.wild.size());
        for (WildGroup g : d.wild) {
            buf.writeUtf(g.title);
            buf.writeVarInt(g.upcoming);
            buf.writeVarInt(g.species.size());
            for (WildSpecies s : g.species) {
                buf.writeResourceLocation(s.id);
                buf.writeUtf(s.form);
                buf.writeUtf(s.prefix);
                buf.writeVarInt(s.knowledge);
            }
        }
        buf.writeBoolean(d.choosingNext);
        buf.writeVarInt(d.regionChoices.size());
        for (RegionChoice h : d.regionChoices) {
            buf.writeUtf(h.id);
            buf.writeUtf(h.title);
            buf.writeVarInt(h.trainers);
            buf.writeVarInt(h.species);
            ComponentSerialization.STREAM_CODEC.encode(buf, h.firstTrainer);
        }
        buf.writeUtf(d.difficulty);
        buf.writeBoolean(d.difficultyChangeable);
        buf.writeBoolean(d.choosingDifficulty);
        buf.writeVarInt(d.wildTrainers.size());
        for (TrainerGroup g : d.wildTrainers) {
            buf.writeUtf(g.title);
            buf.writeVarInt(g.trainers.size());
            for (WildTrainer t : g.trainers) {
                buf.writeUtf(t.name);
                buf.writeVarInt(t.minLevel);
                buf.writeVarInt(t.maxLevel);
                buf.writeBoolean(t.beaten);
            }
        }
        buf.writeVarInt(d.otherLeagues.size());
        for (LeagueView l : d.otherLeagues) {
            buf.writeUtf(l.title);
            buf.writeVarInt(l.tier);
            buf.writeVarInt(l.maxTier);
            ComponentSerialization.STREAM_CODEC.encode(buf, l.next);
            writeRoad(buf, l.road);
            buf.writeBoolean(l.hardAvailable);
        }
        buf.writeBoolean(d.hardAvailable);
    }

    private static void writeRoad(RegistryFriendlyByteBuf buf, List<RoadEntry> road) {
        buf.writeVarInt(road.size());
        for (RoadEntry r : road) {
            buf.writeVarInt(r.tier);
            buf.writeVarInt(r.level);
            ComponentSerialization.STREAM_CODEC.encode(buf, r.name);
            buf.writeVarInt(r.status);
            buf.writeBoolean(r.hard);
        }
    }

    private static List<RoadEntry> readRoad(RegistryFriendlyByteBuf buf) {
        List<RoadEntry> road = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            road.add(new RoadEntry(buf.readVarInt(), buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readVarInt(),
                    buf.readBoolean()));
        }
        return road;
    }

    private static TrainerCardData read(RegistryFriendlyByteBuf buf) {
        String player = buf.readUtf();
        boolean regionMode = buf.readBoolean();
        String regionTitle = buf.readUtf();
        int tier = buf.readVarInt();
        int maxTier = buf.readVarInt();
        int levelCap = buf.readVarInt();
        int seen = buf.readVarInt();
        int caught = buf.readVarInt();
        Component next = ComponentSerialization.STREAM_CODEC.decode(buf);
        Component hint = ComponentSerialization.STREAM_CODEC.decode(buf);
        boolean allDone = buf.readBoolean();

        List<RoadEntry> road = readRoad(buf);
        List<RegionEntry> regions = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            regions.add(new RegionEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        }
        List<WildGroup> wild = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            String title = buf.readUtf();
            int upcoming = buf.readVarInt();
            List<WildSpecies> species = new ArrayList<>();
            for (int j = buf.readVarInt(); j > 0; j--) species.add(new WildSpecies(buf.readResourceLocation(), buf.readUtf(), buf.readUtf(), buf.readVarInt()));
            wild.add(new WildGroup(title, upcoming, species));
        }
        boolean choosingNext = buf.readBoolean();
        List<RegionChoice> regionChoices = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            regionChoices.add(new RegionChoice(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    ComponentSerialization.STREAM_CODEC.decode(buf)));
        }
        String difficulty = buf.readUtf();
        boolean difficultyChangeable = buf.readBoolean();
        boolean choosingDifficulty = buf.readBoolean();
        List<TrainerGroup> wildTrainers = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            String title = buf.readUtf();
            List<WildTrainer> trainers = new ArrayList<>();
            for (int j = buf.readVarInt(); j > 0; j--) {
                trainers.add(new WildTrainer(buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));
            }
            wildTrainers.add(new TrainerGroup(title, trainers));
        }
        List<LeagueView> otherLeagues = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            otherLeagues.add(new LeagueView(buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    ComponentSerialization.STREAM_CODEC.decode(buf), readRoad(buf), buf.readBoolean()));
        }
        boolean hardAvailable = buf.readBoolean();
        return new TrainerCardData(player, regionMode, regionTitle, tier, maxTier, levelCap, seen, caught,
                next, hint, allDone, road, regions, wild, regionChoices, choosingNext, difficulty, difficultyChangeable,
                choosingDifficulty, wildTrainers, otherLeagues, hardAvailable);
    }
}
