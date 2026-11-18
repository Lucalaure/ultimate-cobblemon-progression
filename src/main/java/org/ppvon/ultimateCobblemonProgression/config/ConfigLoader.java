package org.ppvon.ultimateCobblemonProgression.config;

import java.nio.file.Path;
import java.util.function.Supplier;

public final class ConfigLoader {

    private static SpawnConfig SPAWN;
    private static CommonConfig COMMON;

    // ucp-spawn.json
    public static Supplier<Boolean> DO_SPECIES_BLOCKING;
    public static Supplier<Boolean> BLOCK_UNKNOWN_SPECIES;

    public static Supplier<Boolean> DO_LEVEL_SCALING;
    public static Supplier<Double> TIER_CAP_SCALING;
    public static Supplier<Double> MIN_LEVEL_SCALING;
    public static Supplier<Double> AVG_LEVEL_SCALING;
    public static Supplier<Double> MAX_LEVEL_SCALING;

    public static Supplier<Boolean> DO_WEIGHT_SCALING;
    public static Supplier<Double> WEIGHT_DECAY_PER_TIER;
    public static Supplier<Double> WEIGHT_CURRENT_TIER_BUFF;
    public static Supplier<Double> WEIGHT_MIN_FACTOR;

    // ucp-common.json
    public static Supplier<Boolean> DO_LEVEL_CAP;
    public static Supplier<Boolean> DO_DEX_PROGRESSION;
    public static Supplier<Double> GYM_VILLAGE_CHANCE;
    public static Supplier<Boolean> GIVE_TRAINER_CARD;

    private ConfigLoader() {}

    public static void init() {
        reload();
        wireSuppliers();
    }

    public static void reload() {
        Path dir = ConfigManager.configDir();
        SPAWN = ConfigManager.loadOrCreate(dir.resolve("ucp-spawn.json"),  SpawnConfig.class,  new SpawnConfig());
        COMMON = ConfigManager.loadOrCreate(dir.resolve("ucp-common.json"),  CommonConfig.class,  new CommonConfig());
    }

    public static void saveAll() {
        Path dir = ConfigManager.configDir();
        ConfigManager.save(dir.resolve("ucp-spawn.json"),  SPAWN);
        ConfigManager.save(dir.resolve("ucp-common.json"),  COMMON);
    }

    private static void wireSuppliers() {
        DO_SPECIES_BLOCKING = () -> SPAWN.doSpeciesBlocking;
        BLOCK_UNKNOWN_SPECIES = () -> SPAWN.blockUnknownSpecies;

        DO_LEVEL_SCALING = () -> SPAWN.doLevelScaling;
        TIER_CAP_SCALING = () -> SPAWN.tierCapScaling;
        MIN_LEVEL_SCALING = () -> SPAWN.minLevelScaling;
        AVG_LEVEL_SCALING = () -> SPAWN.avgLevelScaling;
        MAX_LEVEL_SCALING = () -> SPAWN.maxLevelScaling;

        DO_WEIGHT_SCALING = () -> SPAWN.doWeightScaling;
        WEIGHT_DECAY_PER_TIER = () -> SPAWN.weightDecayPerTier;
        WEIGHT_MIN_FACTOR = () -> SPAWN.weightMinFactor;
        WEIGHT_CURRENT_TIER_BUFF = () -> SPAWN.weightCurrentTierBuff;

        DO_LEVEL_CAP = () -> COMMON.doLevelCap;
        DO_DEX_PROGRESSION = () -> COMMON.doDexProgression;
        GYM_VILLAGE_CHANCE = () -> COMMON.gymVillageChance;
        GIVE_TRAINER_CARD = () -> COMMON.giveTrainerCard;
    }

    public static SpawnConfig spawn() { return SPAWN; }
    public static CommonConfig common() {return COMMON;}
}
