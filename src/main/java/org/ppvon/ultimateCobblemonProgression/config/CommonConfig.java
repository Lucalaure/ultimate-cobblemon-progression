package org.ppvon.ultimateCobblemonProgression.config;

public class CommonConfig {
    public boolean doLevelCap = true;
    public boolean doDexProgression = true;
    /** Share of new villages (0 to 0.95) whose centre is a gym. Read when a world starts; 0 turns village gyms off. */
    public double gymVillageChance = 0.3;
    /** Give every player a Trainer Card on their first join. */
    public boolean giveTrainerCard = true;
}
