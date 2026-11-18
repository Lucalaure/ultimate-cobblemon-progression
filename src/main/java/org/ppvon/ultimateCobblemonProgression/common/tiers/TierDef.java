package org.ppvon.ultimateCobblemonProgression.common.tiers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.TierRequirements;

import java.util.*;

public final class TierDef {
    public final int index;
    public final int levelCap;
    public final Set<ResourceLocation> species;
    public final TierRequirements requirements;
    /**
     * Radical Cobblemon Trainers trainer ids (e.g. "leader_brock_019e") that a Gym Challenge block summons
     * to unlock this tier, battled in this order. Each entry is a group of alternatives: beating any one
     * of them counts, and the first is the one summoned. Only used in region mode.
     */
    public final List<List<String>> trainers;

    public TierDef(int index, int levelCap, Set<ResourceLocation> species, TierRequirements requirements) {
        this(index, levelCap, species, requirements, List.of());
    }

    public TierDef(int index, int levelCap, Set<ResourceLocation> species, TierRequirements requirements, List<List<String>> trainers) {
        this.index = index;
        this.levelCap = levelCap;
        this.species = Collections.unmodifiableSet(species);
        this.requirements = requirements != null ? requirements : TierRequirements.NONE;
        this.trainers = trainers != null ? trainers.stream().filter(g -> !g.isEmpty()).map(List::copyOf).toList() : List.of();
    }

    /** A copy of this tier with its index set. */
    public TierDef withIndex(int index) {
        return new TierDef(index, levelCap, species, requirements, trainers);
    }

    private static final Codec<Set<ResourceLocation>> SPECIES_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(
                    list -> new LinkedHashSet<>(list),
                    set  -> new ArrayList<>(set)
            );

    /** A trainer id, or a list of alternative trainer ids. */
    private static final Codec<List<String>> TRAINER_GROUP_CODEC =
            Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
                    e -> e.map(List::of, l -> l),
                    l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
            );

    public static final Codec<TierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("levelCap").forGetter(t -> t.levelCap),

            SPECIES_SET_CODEC.optionalFieldOf("species", Set.of()).forGetter(t -> t.species),
            TierRequirements.CODEC.optionalFieldOf("requirements", TierRequirements.NONE).forGetter(t -> t.requirements),
            TRAINER_GROUP_CODEC.listOf().optionalFieldOf("trainers", List.of()).forGetter(t -> t.trainers)
    ).apply(i, (cap, species, reqs, trainers) -> new TierDef(-1, cap, species, reqs, trainers)));
}
