package org.ppvon.ultimateCobblemonProgression.common.gym;

import org.junit.jupiter.api.Test;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.config.SpawnConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GymLegalityTest {
    @Test
    void namesAreJoinedLikeASentence() {
        assertEquals("", GymLegality.joinNames(List.of()));
        assertEquals("Piplup", GymLegality.joinNames(List.of("Piplup")));
        assertEquals("Piplup and Eevee", GymLegality.joinNames(List.of("Piplup", "Eevee")));
        assertEquals("Piplup, Eevee and Riolu", GymLegality.joinNames(List.of("Piplup", "Eevee", "Riolu")));
        assertEquals("Piplup, Eevee, Riolu and 2 more",
                GymLegality.joinNames(List.of("Piplup", "Eevee", "Riolu", "Zorua", "Axew")));
    }

    @Test
    void theModeFollowsTheConfigAndDefaultsToOff() {
        CommonConfig common = new CommonConfig();
        ConfigLoader.initWith(new SpawnConfig(), common);
        assertEquals(GymLegality.Mode.OFF, GymLegality.mode());
        common.gymLegalityCheck = "warn";
        assertEquals(GymLegality.Mode.WARN, GymLegality.mode());
        common.gymLegalityCheck = "BLOCK";
        assertEquals(GymLegality.Mode.BLOCK, GymLegality.mode());
        common.gymLegalityCheck = "nonsense";
        assertEquals(GymLegality.Mode.OFF, GymLegality.mode());
        common.gymLegalityCheck = null;
        assertEquals(GymLegality.Mode.OFF, GymLegality.mode());
    }
}
