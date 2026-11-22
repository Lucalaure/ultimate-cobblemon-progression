package org.ppvon.ultimateCobblemonProgression.config;

public class SpawnConfig {
    public boolean doSpeciesBlocking = true;
    public boolean blockUnknownSpecies = true;
    /** Region mode: regional forms (Alolan, Galarian, ...) only spawn once their own region is open. */
    public boolean lockRegionalForms = true;
    /** Region mode: Radical Cobblemon Trainers' wild trainers that no region lists (e.g. Pokémon Unbound's) never spawn. */
    public boolean blockUnlistedTrainers = true;

    public boolean doLevelScaling = true;
    public double tierCapScaling = 0.25;
    public double minLevelScaling = 0.45;
    public double avgLevelScaling = 0.75;
    public double maxLevelScaling = 1.1;

    // Off by default: unlocking a tier only adds species, it doesn't change the odds of the ones already unlocked.
    public boolean doWeightScaling = false;
    public double weightDecayPerTier = 0.20;
    public double weightMinFactor = 0.15;
    public double weightCurrentTierBuff = 2.0;

    // Region mode: spawn pools while a player works through a region (see TierStanding).
    public boolean doRegionPools = true;
    /** Species of the region in progress. */
    public double regionPoolWeight = 1.0;
    /** Species of completed regions, at the current region's levels (tier within the player's tier there). */
    public double pastInCapPoolWeight = 0.5;
    /** Species of completed regions, at their own fully unlocked levels. */
    public double pastFullPoolWeight = 0.15;

}
