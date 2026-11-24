package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * One Pokémon of a region's tier: a species, or one of its regional forms (Alolan Rattata). A regional form belongs to
 * the region that claims it, at its own tier, so it spawns, levels and counts for catchWholeTier there, apart from its
 * species. The species' entry covers every other form of it (a Pikachu with a cap, a region-bias Pikachu).
 *
 * @param form   the regional form, or null for the species' entry
 * @param aspect the Cobblemon aspect that marks the form (e.g. "alolan"), or null for the species' entry
 * @param region index into {@link RegionRegistry#ordered()}
 * @param tier   1-based tier in that region
 */
public record TierEntry(Species species, FormData form, String aspect, int region, int tier) {
    public boolean isForm() {
        return form != null;
    }

    /** "Alolan" for an Alolan form, "" for a species' entry. */
    public String prefix() {
        if (aspect == null || aspect.isEmpty()) return "";
        return aspect.substring(0, 1).toUpperCase(Locale.ROOT) + aspect.substring(1);
    }

    /** "Alolan Rattata", or the species' name. */
    public Component displayName() {
        return isForm()
                ? Component.literal(prefix() + " ").append(species.getTranslatedName())
                : species.getTranslatedName();
    }

    /** The form's Cobblemon Pokédex form name ("Alola"), or "" for a species' entry. */
    public String formName() {
        return form != null ? form.getName() : "";
    }
}
