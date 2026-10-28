package org.ppvon.ultimateCobblemonProgression.mixin;

import com.cobblemon.mod.common.pokemon.Species;
import org.ppvon.ultimateCobblemonProgression.common.tiers.UltimateCobblemonProgressionTierExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(Species.class)
public class SpeciesTierMixin implements UltimateCobblemonProgressionTierExt {
    @Unique private int ucp$tier = -1;
    @Unique private int ucp$region = -1;
    @Unique private int ucp$regionTier = -1;

    @Override
    public int ucp$getTier() {
        return ucp$tier;
    }

    @Override
    public void ucp$setTier(int tier) {
        this.ucp$tier = tier;
    }

    @Override
    public int ucp$getRegion() {
        return ucp$region;
    }

    @Override
    public int ucp$getRegionTier() {
        return ucp$regionTier;
    }

    @Override
    public void ucp$setRegion(int region, int regionTier) {
        this.ucp$region = region;
        this.ucp$regionTier = regionTier;
    }
}
