package org.ppvon.ultimateCobblemonProgression.config;

public class CommonConfig {
    public boolean doLevelCap = true;
    public boolean doDexProgression = true;
    /** Share of new villages (0 to 0.95) whose centre is a gym. Read when a world starts; 0 turns village gyms off. */
    public double gymVillageChance = 0.3;
    /** Difficulty option: players may bring no more Pokémon to a gym battle than the gym leader has. */
    public boolean gymTeamLimit = false;
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
    /** Give every player a Trainer Card on their first join. */
    public boolean giveTrainerCard = true;
}
