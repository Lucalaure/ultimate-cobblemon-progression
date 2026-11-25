package org.ppvon.ultimateCobblemonProgression.common.gym;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeagueLevelCapTest {
    @Test
    void theAverageOfTwoLeaguesRoundsUp() {
        assertEquals(20, LeagueLevelCap.average(20, 20));
        assertEquals(21, LeagueLevelCap.average(20, 21));
        assertEquals(30, LeagueLevelCap.average(14, 45));
    }

    @Test
    void aCapBelowTheFloorIsHeldAtIt() {
        // Started a second league partway through: the single-league cap was 40, the pair's average is 25.
        assertArrayEquals(new int[]{40, 40}, LeagueLevelCap.applyFloor(25, 40));
    }

    @Test
    void theFloorIsDroppedOnceTheCapReachesIt() {
        assertArrayEquals(new int[]{40, 0}, LeagueLevelCap.applyFloor(40, 40));
        assertArrayEquals(new int[]{45, 0}, LeagueLevelCap.applyFloor(45, 40));
    }

    @Test
    void aDroppedFloorNeverHoldsTheCapAgain() {
        // Stored as 0 once reached: any later cap goes through as it is.
        assertArrayEquals(new int[]{12, 0}, LeagueLevelCap.applyFloor(12, 0));
    }
}
