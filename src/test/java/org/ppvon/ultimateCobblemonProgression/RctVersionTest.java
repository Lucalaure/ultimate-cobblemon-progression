package org.ppvon.ultimateCobblemonProgression;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RctVersionTest {
    private static boolean tested(String version) throws Exception {
        return VersionPredicate.parse(UltimateCobblemonProgression.RCT_TESTED).test(SemanticVersion.parse(version));
    }

    @Test
    void every019ReleaseCountsAsTested() throws Exception {
        assertTrue(tested("0.19.0-beta"));
        assertTrue(tested("0.19.2-beta"));
        assertTrue(tested("0.19.9"));
    }

    @Test
    void otherVersionsAreWarnedAbout() throws Exception {
        assertFalse(tested("0.18.5-beta"));
        assertFalse(tested("0.20.0-beta"));
        assertFalse(tested("1.0.0"));
    }
}
