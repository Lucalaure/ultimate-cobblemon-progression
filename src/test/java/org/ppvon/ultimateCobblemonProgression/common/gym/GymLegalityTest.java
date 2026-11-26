package org.ppvon.ultimateCobblemonProgression.common.gym;

import org.junit.jupiter.api.Test;
import org.ppvon.ultimateCobblemonProgression.config.CommonConfig;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.config.SpawnConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    void theRuleFollowsTheConfigAndDefaultsToRegion() {
        CommonConfig common = new CommonConfig();
        ConfigLoader.initWith(new SpawnConfig(), common);
        assertEquals(GymLegality.Rule.REGION, GymLegality.rule());
        common.gymLegalityRule = "STRICT";
        assertEquals(GymLegality.Rule.STRICT, GymLegality.rule());
        common.gymLegalityRule = "nonsense";
        assertEquals(GymLegality.Rule.REGION, GymLegality.rule());
        common.gymLegalityRule = null;
        assertEquals(GymLegality.Rule.REGION, GymLegality.rule());
    }

    @Test
    void problemsAreDescribedInOneSentence() {
        Map<GymLegality.Problem, List<String>> one = new LinkedHashMap<>();
        one.put(GymLegality.Problem.REGION, List.of("Raichu"));
        assertEquals("Raichu isn't from a region you've opened", GymLegality.describeNames(one));

        Map<GymLegality.Problem, List<String>> two = new LinkedHashMap<>();
        two.put(GymLegality.Problem.TIER, List.of("Gyarados", "Onix"));
        two.put(GymLegality.Problem.LEVEL, List.of("Snorlax"));
        assertEquals("Gyarados and Onix aren't from a tier you've unlocked yet, and Snorlax isn't within your level cap",
                GymLegality.describeNames(two));
    }
}
