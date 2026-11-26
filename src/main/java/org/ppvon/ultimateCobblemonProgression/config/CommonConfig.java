package org.ppvon.ultimateCobblemonProgression.config;

public class CommonConfig {
    public boolean doLevelCap = true;
    public boolean doDexProgression = true;
    /** Share of new villages (0 to 0.95) whose centre is a gym. Read when a world starts; 0 turns village gyms off. */
    public double gymVillageChance = 0.3;
    /** Difficulty option: players may bring no more Pokémon to a gym battle than the gym leader has. */
    public boolean gymTeamLimit = false;
    /**
     * Whether the Pokémon a player brings to a gym battle must be ones they could have by now (eggs, trades and fossils
     * can bring others; see gymLegalityRule). "off": no check. "warn": the leader names them when it accepts. "block":
     * the battle doesn't start while one is in the party. The player's starter is always allowed.
     */
    public String gymLegalityCheck = "off";
    /**
     * What gymLegalityCheck allows. "region": any Pokémon of a region the player has opened (so evolutions of what they
     * catch), at or below their level cap; a regional form counts as its own region's. "strict": only Pokémon of tiers
     * they've unlocked, the ones they can meet in the wild now, at or below their level cap.
     */
    public String gymLegalityRule = "region";
    /**
     * Gym leader difficulty (region mode, with RCT): "player_choice" lets each player pick Normal or Hard on their
     * Trainer Card and change it between gyms; "normal" or "hard" sets it for everyone.
     */
    public String gymDifficulty = "player_choice";
    /**
     * Difficulty option (region mode): to reach a region's next tier, players must also have caught every obtainable
     * Pokémon of their current tier there. Gyms won't summon a league's next leader until they have; the extra
     * starting region just waits at its tier, without holding the league back.
     */
    public boolean catchWholeTier = false;
    /**
     * Region mode: a player's second region (picked on the Trainer Card next to their home league) may also be a
     * league with gym leaders, so they can work through two leagues side by side. Off: only a region without gyms.
     */
    public boolean allowTwoLeagues = true;
    /**
     * Two leagues at once: how the level cap follows both leagues. "average" (the default): the average of the two
     * leagues' caps, rounded up, so it rises whichever league the player advances. "lowest": the lower of the two, so
     * the player has to keep both leagues level, much like one combined league.
     */
    public String twoLeagueCap = "average";
    /**
     * Region mode: players pick their starter only once they've chosen where their journey starts, and only from that
     * region's starters (plus their second region's, if they picked it first).
     */
    public boolean regionStarters = true;
    /** The level starters come at, as in the original games. 0 keeps the level in Cobblemon's starter list (10). */
    public int starterLevel = 5;
    /** Give every player a Trainer Card on their first join. */
    public boolean giveTrainerCard = true;
}
