package org.ppvon.ultimateCobblemonProgression.common.tiers;

public interface UltimateCobblemonProgressionTierExt {
    int ucp$getTier();
    void ucp$setTier(int tier);

    /** Index into RegionRegistry.ordered(), or -1 if the species is in no region. */
    int ucp$getRegion();
    /** Tier within the species' region, or -1. */
    int ucp$getRegionTier();
    void ucp$setRegion(int region, int regionTier);
}
