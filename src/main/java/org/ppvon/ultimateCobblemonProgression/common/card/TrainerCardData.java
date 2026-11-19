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
        boolean choosingNext
) {
    public static final int BEATEN = 0, NEXT = 1, LATER = 2;
    public static final int LOCKED = 0, OPEN = 1, COMPLETE = 2;
    public static final int UNKNOWN = 0, SEEN = 1, CAUGHT = 2;

    /** A trainer in the region's order. */
    public record RoadEntry(int tier, int levelCap, Component name, int status) {}

    /**
     * A region and the player's standing in it.
     *
     * @param startPick  the region the player picked to explore from the start
     * @param choosable  the player can still pick this region as their starting region
     */
    public record RegionEntry(String id, String title, int state, int tier, int maxTier, boolean startPick, boolean choosable) {}

    /** The species allowed to spawn from one region, and how many the next tier adds. */
    public record WildGroup(String title, int upcoming, List<WildSpecies> species) {}

    public record WildSpecies(ResourceLocation id, int knowledge) {}

    /** A league region the player can start their journey in, or travel to next, shown on the card's choice screen. */
    public record RegionChoice(String id, String title, int trainers, int species, Component firstTrainer) {}

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

        buf.writeVarInt(d.road.size());
        for (RoadEntry r : d.road) {
            buf.writeVarInt(r.tier);
            buf.writeVarInt(r.levelCap);
            ComponentSerialization.STREAM_CODEC.encode(buf, r.name);
            buf.writeVarInt(r.status);
        }
        buf.writeVarInt(d.regions.size());
        for (RegionEntry r : d.regions) {
            buf.writeUtf(r.id);
            buf.writeUtf(r.title);
            buf.writeVarInt(r.state);
            buf.writeVarInt(r.tier);
            buf.writeVarInt(r.maxTier);
            buf.writeBoolean(r.startPick);
            buf.writeBoolean(r.choosable);
        }
        buf.writeVarInt(d.wild.size());
        for (WildGroup g : d.wild) {
            buf.writeUtf(g.title);
            buf.writeVarInt(g.upcoming);
            buf.writeVarInt(g.species.size());
            for (WildSpecies s : g.species) {
                buf.writeResourceLocation(s.id);
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

        List<RoadEntry> road = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            road.add(new RoadEntry(buf.readVarInt(), buf.readVarInt(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readVarInt()));
        }
        List<RegionEntry> regions = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            regions.add(new RegionEntry(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean(), buf.readBoolean()));
        }
        List<WildGroup> wild = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            String title = buf.readUtf();
            int upcoming = buf.readVarInt();
            List<WildSpecies> species = new ArrayList<>();
            for (int j = buf.readVarInt(); j > 0; j--) species.add(new WildSpecies(buf.readResourceLocation(), buf.readVarInt()));
            wild.add(new WildGroup(title, upcoming, species));
        }
        boolean choosingNext = buf.readBoolean();
        List<RegionChoice> regionChoices = new ArrayList<>();
        for (int i = buf.readVarInt(); i > 0; i--) {
            regionChoices.add(new RegionChoice(buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readVarInt(),
                    ComponentSerialization.STREAM_CODEC.decode(buf)));
        }
        return new TrainerCardData(player, regionMode, regionTitle, tier, maxTier, levelCap, seen, caught,
                next, hint, allDone, road, regions, wild, regionChoices, choosingNext);
    }
}
