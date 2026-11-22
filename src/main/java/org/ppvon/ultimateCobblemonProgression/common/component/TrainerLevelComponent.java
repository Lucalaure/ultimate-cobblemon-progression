package org.ppvon.ultimateCobblemonProgression.common.component;

import org.ladysnake.cca.api.v3.component.ComponentV3;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Trainer level applies many scalings in org.ppvon.ultimateCobblemonProgression.influence
 *
 * NOTE
 * Currently there is no way to naturally increase trainer level other than console commands
 * /trainerlevel set x
 *
 * I am still deciding whether to integrate with RAD Gyms or RAD Trainers.
 * RAD Trainers has their own level cap which seems pretty tightly coupled with their
 * series feature, which may be a bit to complex on top of this level cap system.
 */
public interface TrainerLevelComponent extends ComponentV3 {
    int getLevel();
    void setLevel(int level);

    /** Region mode: the extra region this player chose to have open from the start, if any. */
    Optional<String> getStartRegion();
    /** Pass null to clear the choice. */
    void setStartRegion(String regionId);

    /** Region mode: the region this player's journey starts in, picked on their Trainer Card. Empty until picked. */
    Optional<String> getHomeRegion();
    /** Pass null to clear the choice. */
    void setHomeRegion(String regionId);

    /**
     * Region mode: the regions this player travelled to after their home region, in the order they picked them.
     * Each was picked on their Trainer Card after completing every region of their journey so far.
     */
    List<String> getNextRegions();
    void addNextRegion(String regionId);
    void clearNextRegions();

    /**
     * Radical Cobblemon Trainers' wild trainers (not league trainers) this player has beaten, as recorded by CJP since
     * the update that added the Trainer Card's Trainers tab. Earlier wins aren't counted: those trainers count once
     * they're beaten again.
     */
    Set<String> getBeatenWildTrainers();
    /** Returns whether the trainer is new to the record. */
    boolean addBeatenWildTrainer(String trainerId);

    /** Region mode: the gym difficulty this player picked ("normal" or "hard"). Empty until picked. */
    Optional<String> getGymDifficulty();
    /** Pass null to clear the choice. */
    void setGymDifficulty(String difficultyId);
}
