package org.ppvon.ultimateCobblemonProgression.common.influence;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ppvon.ultimateCobblemonProgression.common.progression.TierStanding;
import org.ppvon.ultimateCobblemonProgression.common.progression.TierStanding.Pool;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.config.SpawnConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrainerLevelInfluenceTest {
    private SpawnConfig spawn;

    @BeforeEach
    void defaults() {
        spawn = new SpawnConfig();
        ConfigLoader.initWith(spawn, new CommonConfig());
    }

    private static Pool pool(Pool.Kind kind, int speciesCap, int playerCap, int tierGap, double weight) {
        return new Pool(kind, speciesCap, playerCap, tierGap, weight);
    }

    @Test
    void levelRangeOfASpeciesAtThePlayersTier() {
        // Cap 15, no boost: 0.45 * 15 = 6.75 -> 6, 0.75 * 15 = 11.25 -> 11, 1.1 * 15 = 16.5 -> 16.
        int[] range = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 15, 15, 0, 1));
        assertArrayEquals(new int[]{6, 11, 16}, range);
    }

    @Test
    void lowerTierSpeciesAreBoostedTowardsThePlayersCap() {
        // The example from the class comment: a tier 1 species (cap 15) for a tier 5 player (cap 69).
        // 15 + (69 - 15) * 0.25 = 28.5 -> 29.
        int[] range = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 15, 69, 4, 1));
        assertEquals((int) Math.floor(0.45 * 29), range[0]);
        assertEquals((int) Math.round(0.75 * 29), range[1]);
        assertEquals((int) Math.floor(1.1 * 29), range[2]);
    }

    @Test
    void higherTierSpeciesAreNotLoweredToThePlayersCap() {
        int[] ahead = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 50, 20, -3, 1));
        int[] own = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 50, 50, 0, 1));
        assertArrayEquals(own, ahead);
    }

    @Test
    void levelsNeverLeaveOneToOneHundred() {
        int[] high = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 100, 100, 0, 1));
        assertEquals(100, high[2]);
        int[] low = TrainerLevelInfluence.levelRange(pool(Pool.Kind.REGION, 1, 1, 0, 1));
        assertTrue(low[0] >= 1 && low[1] >= 1 && low[2] >= 1);
    }

    @Test
    void triangularSamplesStayInRangeAndCentreOnTheMode() {
        RandomSource random = RandomSource.create(42);
        long sum = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            int level = TrainerLevelInfluence.sampleTriangularInt(10, 20, 40, random);
            assertTrue(level >= 10 && level <= 40, "level " + level);
            sum += level;
        }
        // The mean of a triangular distribution is (min + mode + max) / 3 = 23.3.
        assertEquals(70 / 3.0, sum / (double) n, 0.3);
    }

    @Test
    void triangularSamplingHandlesDegenerateRanges() {
        RandomSource random = RandomSource.create(1);
        assertEquals(7, TrainerLevelInfluence.sampleTriangularInt(7, 7, 7, random));
        assertEquals(9, TrainerLevelInfluence.sampleTriangularInt(9, 5, 3, random));
        for (int i = 0; i < 100; i++) {
            assertTrue(TrainerLevelInfluence.sampleTriangularInt(90, 95, 130, random) <= 100);
        }
    }

    @Test
    void weightIsThePoolWeightWithoutTierScaling() {
        spawn.doWeightScaling = false;
        var standing = new TierStanding(true, List.of(
                pool(Pool.Kind.PAST_FULL, 40, 40, 0, 0.15),
                pool(Pool.Kind.PAST_IN_CAP, 20, 30, 1, 0.5)));
        assertEquals(0.65, TrainerLevelInfluence.weightMultiplier(standing), 1e-9);
    }

    @Test
    void tierScalingFavoursSpeciesNearThePlayersTier() {
        spawn.doWeightScaling = true;
        // 2.0 - 0.2 * gap, never below 0.15.
        assertEquals(2.0, TrainerLevelInfluence.weightFactor(0), 1e-9);
        assertEquals(1.2, TrainerLevelInfluence.weightFactor(4), 1e-9);
        assertEquals(1.2, TrainerLevelInfluence.weightFactor(-4), 1e-9);
        assertEquals(0.15, TrainerLevelInfluence.weightFactor(20), 1e-9);
        assertEquals(1.2, TrainerLevelInfluence.poolWeight(pool(Pool.Kind.REGION, 15, 69, 4, 1.0)), 1e-9);
    }

    @Test
    void poolsArePickedInProportionToTheirWeight() {
        spawn.doWeightScaling = false;
        Pool full = pool(Pool.Kind.PAST_FULL, 40, 40, 0, 0.25);
        Pool inCap = pool(Pool.Kind.PAST_IN_CAP, 20, 30, 1, 0.75);
        var standing = new TierStanding(true, List.of(full, inCap));
        RandomSource random = RandomSource.create(7);
        int inCapPicks = 0, n = 20_000;
        for (int i = 0; i < n; i++) {
            if (TrainerLevelInfluence.pickPool(standing, random) == inCap) inCapPicks++;
        }
        assertEquals(0.75, inCapPicks / (double) n, 0.02);
    }

    @Test
    void aSinglePoolIsAlwaysPicked() {
        Pool only = pool(Pool.Kind.REGION, 10, 10, 0, 1);
        assertSame(only, TrainerLevelInfluence.pickPool(new TierStanding(true, List.of(only)), RandomSource.create(3)));
    }
}
