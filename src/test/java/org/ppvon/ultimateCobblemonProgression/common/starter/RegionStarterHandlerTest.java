package org.ppvon.ultimateCobblemonProgression.common.starter;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegionStarterHandlerTest {
    private static final Set<String> KANTO_AND_HOENN = Set.of("kanto", "hoenn");

    @Test
    void categoriesMatchTheirRegionByName() {
        assertTrue(RegionStarterHandler.matches("Kanto", "cobblemon.starterselection.category.kanto", KANTO_AND_HOENN));
        assertTrue(RegionStarterHandler.matches("HOENN", "", KANTO_AND_HOENN));
        assertFalse(RegionStarterHandler.matches("Johto", "cobblemon.starterselection.category.johto", KANTO_AND_HOENN));
    }

    @Test
    void aRenamedCategoryStillMatchesByItsTranslationKey() {
        assertTrue(RegionStarterHandler.matches("Classic", "cobblemon.starterselection.category.kanto", KANTO_AND_HOENN));
        assertFalse(RegionStarterHandler.matches("Classic", "My Starters", KANTO_AND_HOENN));
        assertFalse(RegionStarterHandler.matches(null, null, KANTO_AND_HOENN));
    }
}
