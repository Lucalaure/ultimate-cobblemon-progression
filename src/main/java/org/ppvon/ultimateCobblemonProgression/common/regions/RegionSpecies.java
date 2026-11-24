package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.common.tiers.UltimateCobblemonProgressionTierExt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Assigns each Cobblemon species its region and tier within that region, and each regional form its own.
 * A species belongs to the first region (in unlock order) that lists it, at the lowest tier listing it there.
 *
 * <p>A regional form (a form whose aspects include a region's "formAspects", e.g. "alolan") belongs to the region
 * claiming that aspect. Its tier is the one a tier file of that region lists it in ("cobblemon:rattata alolan");
 * a form no tier file lists takes its species' tier number there, capped at the region's last tier.
 */
public final class RegionSpecies {
    /** Prefix of the aspects Cobblemon gives Pokémon that evolve into a regional form ("region-bias-alola"). */
    public static final String REGION_BIAS_PREFIX = "region-bias-";

    /** Each species' regional forms. Species with none aren't in the map. */
    private static Map<Species, List<TierEntry>> FORMS = Map.of();

    private RegionSpecies() {}

    public static void applyFromRegistry() {
        for (Species sp : PokemonSpecies.INSTANCE.getSpecies()) {
            ext(sp).ucp$setRegion(-1, -1);
        }

        List<RegionDef> regions = RegionRegistry.ordered();
        regions.forEach(RegionDef::clearSpecies);
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);
            for (var tier : region.tiers.entrySet()) {
                for (ResourceLocation id : tier.getValue().species) {
                    Species sp = PokemonSpecies.getByIdentifier(id);
                    if (sp == null) continue;
                    var ext = ext(sp);
                    if (ext.ucp$getRegion() == -1) {
                        ext.ucp$setRegion(r, tier.getKey());
                        region.addEntry(new TierEntry(sp, null, null, r, tier.getKey()));
                    } else if (ext.ucp$getRegion() != r) {
                        UltimateCobblemonProgression.LOG.warn("{} is listed in regions '{}' and '{}'; keeping '{}'",
                                id, regions.get(ext.ucp$getRegion()).id, region.id, regions.get(ext.ucp$getRegion()).id);
                    }
                }
            }
        }

        Map<Species, List<TierEntry>> forms = new HashMap<>();
        // Forms the tier files list, at their tier.
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);
            for (var tier : region.tiers.entrySet()) {
                for (TierDef.FormRef ref : tier.getValue().forms) {
                    addListedForm(forms, regions, r, tier.getKey(), ref);
                }
            }
        }
        // Every other regional form, at its species' tier number.
        for (Species sp : PokemonSpecies.INSTANCE.getSpecies()) {
            for (FormData form : sp.getForms()) {
                String aspect = regionalAspect(form);
                if (aspect == null || find(forms.get(sp), form) != null) continue;
                int region = RegionRegistry.formRegion(List.of(aspect));
                int speciesTier = regionTier(sp);
                if (speciesTier < 1) continue; // its species is in no region, so there's no tier to give it
                RegionDef def = RegionRegistry.get(region);
                TierEntry entry = new TierEntry(sp, form, aspect, region, Math.min(speciesTier, def.maxTier()));
                forms.computeIfAbsent(sp, k -> new ArrayList<>()).add(entry);
                def.addEntry(entry);
            }
        }
        forms.replaceAll((sp, list) -> List.copyOf(list));
        FORMS = Map.copyOf(forms);
    }

    private static void addListedForm(Map<Species, List<TierEntry>> forms, List<RegionDef> regions, int r, int tier,
                                      TierDef.FormRef ref) {
        RegionDef region = regions.get(r);
        Species sp = PokemonSpecies.getByIdentifier(ref.species());
        if (sp == null) return;
        // The form with all the listed aspects and the fewest others ("tauros paldean" is the Combat Breed).
        FormData form = null;
        for (FormData f : sp.getForms()) {
            if (f.getAspects().containsAll(ref.aspects()) && (form == null || f.getAspects().size() < form.getAspects().size())) {
                form = f;
            }
        }
        if (form == null) {
            UltimateCobblemonProgression.LOG.warn("Region '{}' lists '{}', but {} has no form with the aspects {}; skipping it",
                    region.id, ref, ref.species(), ref.aspects());
            return;
        }
        String aspect = regionalAspect(form);
        if (aspect == null) aspect = ref.aspects().get(0);
        TierEntry existing = find(forms.get(sp), form);
        if (existing != null) {
            if (existing.region() != r) {
                UltimateCobblemonProgression.LOG.warn("'{}' is listed in regions '{}' and '{}'; keeping '{}'",
                        ref, regions.get(existing.region()).id, region.id, regions.get(existing.region()).id);
            }
            return;
        }
        int claimedBy = RegionRegistry.formRegion(List.of(aspect));
        if (claimedBy >= 0 && claimedBy != r) {
            UltimateCobblemonProgression.LOG.warn("Region '{}' lists '{}', but region '{}' claims the aspect '{}' in its formAspects",
                    region.id, ref, regions.get(claimedBy).id, aspect);
        }
        TierEntry entry = new TierEntry(sp, form, aspect, r, tier);
        forms.computeIfAbsent(sp, k -> new ArrayList<>()).add(entry);
        region.addEntry(entry);
    }

    private static TierEntry find(List<TierEntry> entries, FormData form) {
        if (entries == null) return null;
        for (TierEntry e : entries) {
            if (e.form() == form) return e;
        }
        return null;
    }

    /**
     * The aspect that makes this form a regional form (one some region claims in its formAspects, e.g. "alolan"),
     * or null. Region-bias forms aren't regional forms: they look like the species and are caught as it.
     */
    private static String regionalAspect(FormData form) {
        for (String aspect : form.getAspects()) {
            if (aspect.startsWith(REGION_BIAS_PREFIX)) continue;
            if (RegionRegistry.formRegion(List.of(aspect)) >= 0) return aspect;
        }
        return null;
    }

    /** Index into RegionRegistry.ordered(), or -1 if the species is in no region. */
    public static int region(Species sp) {
        return ext(sp).ucp$getRegion();
    }

    /** Tier within the species' region, or -1. */
    public static int regionTier(Species sp) {
        return ext(sp).ucp$getRegionTier();
    }

    /** The species' regional forms (empty if it has none). */
    public static List<TierEntry> forms(Species sp) {
        return FORMS.getOrDefault(sp, List.of());
    }

    /** The species' own entry, or null if it's in no region. */
    public static TierEntry speciesEntry(Species sp) {
        int region = region(sp);
        return region >= 0 ? new TierEntry(sp, null, null, region, regionTier(sp)) : null;
    }

    /**
     * Which entry a Pokémon with these aspects is: one of the species' regional forms if the aspects mark it,
     * otherwise the species' own entry. Null if neither is in a region.
     */
    public static TierEntry entry(Species sp, Collection<String> aspects) {
        List<TierEntry> forms = FORMS.get(sp);
        if (forms != null && aspects != null && !aspects.isEmpty()) {
            TierEntry exact = find(forms, sp.getForm(new HashSet<>(aspects)));
            if (exact != null) return exact;
            for (TierEntry e : forms) {
                if (aspects.contains(e.aspect())) return e;
            }
        }
        return speciesEntry(sp);
    }

    /** The species' regional form with this Pokédex form name ("Alola"), or null. */
    public static TierEntry formByName(Species sp, String formName) {
        for (TierEntry e : forms(sp)) {
            if (e.form().getName().equals(formName)) return e;
        }
        return null;
    }

    /** Whether these aspects mark a Pokémon that always evolves into a regional form ("region-bias-alola"). */
    public static boolean isRegionBias(Collection<String> aspects) {
        if (aspects == null) return false;
        for (String aspect : aspects) {
            if (aspect.startsWith(REGION_BIAS_PREFIX) && !aspect.equals(REGION_BIAS_PREFIX + "none")) return true;
        }
        return false;
    }

    private static UltimateCobblemonProgressionTierExt ext(Species sp) {
        return (UltimateCobblemonProgressionTierExt) (Object) sp;
    }
}
