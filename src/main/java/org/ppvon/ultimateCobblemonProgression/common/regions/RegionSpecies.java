package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.tiers.UltimateCobblemonProgressionTierExt;

import java.util.List;

/**
 * Assigns each Cobblemon species its region and tier within that region.
 * A species belongs to the first region (in unlock order) that lists it, at the lowest tier listing it there.
 */
public final class RegionSpecies {
    private RegionSpecies() {}

    public static void applyFromRegistry() {
        for (Species sp : PokemonSpecies.INSTANCE.getSpecies()) {
            ext(sp).ucp$setRegion(-1, -1);
        }

        List<RegionDef> regions = RegionRegistry.ordered();
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);
            for (var tier : region.tiers.entrySet()) {
                int real = 0;
                for (ResourceLocation id : tier.getValue().species) {
                    Species sp = PokemonSpecies.getByIdentifier(id);
                    if (sp == null) continue;
                    var ext = ext(sp);
                    if (ext.ucp$getRegion() == -1) {
                        ext.ucp$setRegion(r, tier.getKey());
                        real++;
                    } else if (ext.ucp$getRegion() != r) {
                        UltimateCobblemonProgression.LOG.warn("{} is listed in regions '{}' and '{}'; keeping '{}'",
                                id, regions.get(ext.ucp$getRegion()).id, region.id, regions.get(ext.ucp$getRegion()).id);
                    }
                }
                region.putRealSpecies(tier.getKey(), real);
            }
        }
    }

    /** Index into RegionRegistry.ordered(), or -1 if the species is in no region. */
    public static int region(Species sp) {
        return ext(sp).ucp$getRegion();
    }

    /** Tier within the species' region, or -1. */
    public static int regionTier(Species sp) {
        return ext(sp).ucp$getRegionTier();
    }

    private static UltimateCobblemonProgressionTierExt ext(Species sp) {
        return (UltimateCobblemonProgressionTierExt) (Object) sp;
    }
}
