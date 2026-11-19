package org.ppvon.ultimateCobblemonProgression.config;

public class CommonConfig {
    public boolean doLevelCap = true;
    public boolean doDexProgression = true;
    /** Share of new villages (0 to 0.95) whose centre is a gym. Read when a world starts; 0 turns village gyms off. */
    public double gymVillageChance = 0.3;
    /** Difficulty option: players may bring no more Pokémon to a gym battle than the gym leader has. */
    public boolean gymTeamLimit = false;
    /**
     * Difficulty option (region mode): to reach a league region's next tier, players must also have caught every
     * obtainable Pokémon of their current tier. Gyms won't summon the next leader until they have.
     */
    public boolean catchWholeTier = false;
    /** Give every player a Trainer Card on their first join. */
    public boolean giveTrainerCard = true;
}
