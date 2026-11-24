package org.ppvon.ultimateCobblemonProgression.common.tiers;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.TierRequirements;

import java.util.*;

public final class TierDef {
    public final int index;
    public final int levelCap;
    public final Set<ResourceLocation> species;
    /**
     * Regional forms listed in the "species" list as a species id and a form aspect, e.g. "cobblemon:rattata alolan".
     * Only used in region mode, where the form belongs to this tier of the region instead of following its species.
     */
    public final List<FormRef> forms;
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
        this(index, levelCap, species, List.of(), requirements, trainers);
    }

    public TierDef(int index, int levelCap, Set<ResourceLocation> species, List<FormRef> forms,
                   TierRequirements requirements, List<List<String>> trainers) {
        this.index = index;
        this.levelCap = levelCap;
        this.species = Collections.unmodifiableSet(species);
        this.forms = forms != null ? List.copyOf(forms) : List.of();
        this.requirements = requirements != null ? requirements : TierRequirements.NONE;
        this.trainers = trainers != null ? trainers.stream().filter(g -> !g.isEmpty()).map(List::copyOf).toList() : List.of();
    }

    /** A copy of this tier with its index set. */
    public TierDef withIndex(int index) {
        return new TierDef(index, levelCap, species, forms, requirements, trainers);
    }

    /**
     * A regional form in a tier's species list: the species and the Cobblemon aspects that mark the form, e.g.
     * "cobblemon:rattata alolan", or "cobblemon:tauros paldean blaze-breed" when several forms share an aspect.
     */
    public record FormRef(ResourceLocation species, List<String> aspects) {
        @Override
        public String toString() {
            return species + " " + String.join(" ", aspects);
        }
    }

    /** A tier's "species" list, split into plain species and regional forms. */
    private record SpeciesList(Set<ResourceLocation> species, List<FormRef> forms) {}

    /** "cobblemon:pidgey" is a species; "cobblemon:rattata alolan" is the species' form with that aspect. */
    private static final Codec<SpeciesList> SPECIES_LIST_CODEC =
            Codec.STRING.listOf().comapFlatMap(TierDef::parseSpeciesList, list -> {
                List<String> out = new ArrayList<>();
                list.species.forEach(id -> out.add(id.toString()));
                list.forms.forEach(form -> out.add(form.toString()));
                return out;
            });

    private static DataResult<SpeciesList> parseSpeciesList(List<String> entries) {
        Set<ResourceLocation> species = new LinkedHashSet<>();
        List<FormRef> forms = new ArrayList<>();
        for (String entry : entries) {
            String[] parts = entry.trim().split("\\s+");
            ResourceLocation id = parts.length >= 1 ? ResourceLocation.tryParse(parts[0].toLowerCase(Locale.ROOT)) : null;
            if (id == null) {
                return DataResult.error(() -> "Invalid species entry '" + entry + "' (expected e.g. \"cobblemon:rattata\" or \"cobblemon:rattata alolan\")");
            }
            if (parts.length == 1) species.add(id);
            else forms.add(new FormRef(id, Arrays.stream(parts).skip(1).map(a -> a.toLowerCase(Locale.ROOT)).toList()));
        }
        return DataResult.success(new SpeciesList(species, forms));
    }

    /** A trainer id, or a list of trainer ids (alternatives in a tier slot, versions of one trainer in a wild trainer list). */
    public static final Codec<List<String>> TRAINER_GROUP_CODEC =
            Codec.either(Codec.STRING, Codec.STRING.listOf()).xmap(
                    e -> e.map(List::of, l -> l),
                    l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
            );

    public static final Codec<TierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("levelCap").forGetter(t -> t.levelCap),

            SPECIES_LIST_CODEC.optionalFieldOf("species", new SpeciesList(Set.of(), List.of()))
                    .forGetter(t -> new SpeciesList(t.species, t.forms)),
            TierRequirements.CODEC.optionalFieldOf("requirements", TierRequirements.NONE).forGetter(t -> t.requirements),
            TRAINER_GROUP_CODEC.listOf().optionalFieldOf("trainers", List.of()).forGetter(t -> t.trainers)
    ).apply(i, (cap, species, reqs, trainers) -> new TierDef(-1, cap, species.species(), species.forms(), reqs, trainers)));
}
