package org.ppvon.ultimateCobblemonProgression.common.influence;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.ppvon.ultimateCobblemonProgression.common.progression.TierStanding;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Influence that affects spawnability, weight, and level of spawned pokemon
 *
 * Spawnability:
 *  This mod has a "trainer level" component saved to a player that maps to
 *  a "tier" defined in data.  This tier defines a list of pokemon that are
 *  allowed to spawn if a trainer is at least that tier.  If a species is not
 *  found in a tier, by default the spawn is still blocked, but can be configured
 *  with BLOCK_UNKNOWN_SPECIES.  In region mode, each region has its own tier
 *  ladder instead (see {@link TierStanding}).
 *
 * Weight:
 *  Pokemon in the same tier as the player get a buff to their weight via
 *  WEIGHT_CURRENT_TIER_BUFF.  Pokemon below the player tier have a decay
 *  applied to their weight WEIGHT_DECAY_PER_TIER but can't go below the minimum
 *  WEIGHT_MIN_FACTOR.
 *  For example
 *      Tier 1 mon with weight 300
 *      Tier 5 player
 *      Diff in tier is 4
 *      2.0 - 4 * WEIGHT_DECAY_PER_TIER(0.20) = 1.2
 *      Weight multiplier = 1.2 (not lower than min so no clamping)
 *
 *      New tier 1 mon weight = 360
 *
 *  Level:
 *   The level of mons is scaled to a triangle distribution around the level cap
 *   for the tier they're in.  The distribution is scaled around MIN_LEVEL_SCALING,
 *   AVG_LEVEL_SCALING, and MAX_LEVEL_SCALING.  If a pokemon is below the
 *   player's tier, their cap is buffed by the difference in cap between the tiers.
 *   For example
 *      Tier 1 mon, level cap 15 (default data)
 *      Tier 5 player, level cap 69 (default data)
 *      69 - 15 = 54
 *      54 * TIER_CAP_SCALING (0.25) =~ 14
 *
 *      New tier 1 cap is player is level 5 = 29
 *
 *      With default min/avg/max, the Tier 1pokemon will spawn between
 *      level 13 and 31, with an average of 22.
 *
 *    This means lower tier pokemon will still stay lower level than higher tier
 *    pokemon, but will not be level 5-16 forever.
 */
public final class TrainerLevelInfluence implements SpawningInfluence {
    private static final Supplier<Boolean> BLOCK_UNKNOWN_SPECIES = ConfigLoader.BLOCK_UNKNOWN_SPECIES;
    private static final Supplier<Double> TIER_CAP_SCALING = ConfigLoader.TIER_CAP_SCALING;

    private static final Supplier<Double> MIN_LEVEL_SCALING = ConfigLoader.MIN_LEVEL_SCALING;
    private static final Supplier<Double> AVG_LEVEL_SCALING = ConfigLoader.AVG_LEVEL_SCALING;
    private static final Supplier<Double> MAX_LEVEL_SCALING = ConfigLoader.MAX_LEVEL_SCALING;

    private static final Supplier<Double> WEIGHT_DECAY_PER_TIER = ConfigLoader.WEIGHT_DECAY_PER_TIER;
    private static final Supplier<Double> WEIGHT_MIN_FACTOR = ConfigLoader.WEIGHT_MIN_FACTOR;
    private static final Supplier<Double> WEIGHT_CURRENT_TIER_BUFF = ConfigLoader.WEIGHT_CURRENT_TIER_BUFF;

    private static int sampleTriangularInt(int min, int mode, int max, RandomSource rand) {
        if (min >= max) return min;
        if (max > 100) max = 100;
        mode = Mth.clamp(mode, min, max);
        double u = rand.nextDouble();
        double F = (double)(mode - min) / (max - min);

        double x;
        if (u < F) {
            x = min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            x = max - Math.sqrt((1 - u) * (max - min) * (max - mode));
        }
        return Mth.clamp((int)Math.round(x), min, max);
    }

    private static double weightFactor(int tierGap) {
        int diff = Math.abs(tierGap);
        double f = WEIGHT_CURRENT_TIER_BUFF.get() - (WEIGHT_DECAY_PER_TIER.get() * diff);
        return (f < WEIGHT_MIN_FACTOR.get()) ? WEIGHT_MIN_FACTOR.get() : f;
    }

    /** The detail's species, or null for non-Pokémon details, "random" species and unknown ids. */
    private static Species speciesOf(SpawnDetail detail) {
        if (!(detail instanceof PokemonSpawnDetail psd)) return null;
        return speciesOf(psd.getPokemon().getSpecies());
    }

    /** Resolves a species string from spawn data ("pidgey", "cobblemon:pidgey"), or null for "random" and unknown ids. */
    private static Species speciesOf(String speciesStr) {
        if (speciesStr == null || "random".equalsIgnoreCase(speciesStr)) return null;

        String lower = speciesStr.toLowerCase(Locale.ROOT);
        ResourceLocation id = lower.contains(":")
                ? ResourceLocation.tryParse(lower)
                : ResourceLocation.fromNamespaceAndPath("cobblemon", lower);
        return id != null ? PokemonSpecies.getByIdentifier(id) : null;
    }

    /** Whether species blocking lets this species spawn for the player. */
    static boolean isAllowed(Species sp, ServerPlayer player) {
        if (!TierStanding.isKnown(sp)) return !BLOCK_UNKNOWN_SPECIES.get();
        return player != null && TierStanding.of(player, sp).allowed();
    }

    /**
     * Herds (and alphas, which are herds with a leader) list several species in one spawn entry.
     * The herd may spawn if any member is allowed; locked members are cancelled one by one in
     * {@link #onEntitySpawn}.
     */
    private static boolean anyHerdMemberAllowed(PokemonHerdSpawnDetail herd, ServerPlayer player) {
        for (var member : herd.getHerdablePokemon()) {
            Species sp = speciesOf(member.getPokemon().getSpecies());
            if (sp == null || isAllowed(sp, player)) return true;
        }
        return false;
    }

    /**
     * Last check for every Pokémon Cobblemon is about to spawn. Single spawns were already filtered in
     * {@link #affectSpawnable}; this catches herd members, which share one spawn entry.
     */
    public static void registerSpawnGuard() {
        CobblemonEvents.ENTITY_SPAWN.subscribe(Priority.NORMAL, (Function1<? super SpawnEvent<?>, Unit>) TrainerLevelInfluence::onEntitySpawn);
    }

    private static Unit onEntitySpawn(SpawnEvent<?> event) {
        if (!ConfigLoader.DO_SPECIES_BLOCKING.get() || !TierStanding.hasTierData()) return Unit.INSTANCE;
        if (!(event.getEntity() instanceof PokemonEntity pe)) return Unit.INSTANCE;

        if (!isAllowed(pe.getPokemon().getSpecies(), resolvePlayer(event.getSpawnablePosition()))) {
            event.cancel();
        }
        return Unit.INSTANCE;
    }

    private static ServerPlayer resolvePlayer(SpawnablePosition ctx) {
        var cause = ctx.getCause();
        if (cause != null) {
            Entity e = cause.getEntity();
            if (e instanceof ServerPlayer sp) return sp;
            if (e instanceof net.minecraft.world.entity.projectile.Projectile p && p.getOwner() instanceof ServerPlayer sp2) return sp2;
            if (e instanceof net.minecraft.world.entity.TamableAnimal t && t.getOwner() instanceof ServerPlayer sp3) return sp3;
        }
        ServerLevel level = ctx.getWorld();
        BlockPos origin = ctx.getPosition();
        if (level != null && origin != null) {
            return (ServerPlayer) level.getNearestPlayer(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5, 128.0, false);
        }
        return null;
    }

    @Override
    public boolean affectSpawnable(@NotNull SpawnDetail spawnDetail, @NotNull SpawnablePosition spawningContext) {
        if (!ConfigLoader.DO_SPECIES_BLOCKING.get() || !TierStanding.hasTierData()) {
            return true;
        }
        if (spawnDetail instanceof PokemonHerdSpawnDetail herd) {
            return anyHerdMemberAllowed(herd, resolvePlayer(spawningContext));
        }
        Species sp = speciesOf(spawnDetail);
        if (sp == null) return true;

        return isAllowed(sp, resolvePlayer(spawningContext));
    }

    @Override
    public float affectWeight(@NotNull SpawnDetail spawnDetail,
                              @NotNull SpawnablePosition ctx,
                              float currentWeight) {
        if (!ConfigLoader.DO_WEIGHT_SCALING.get() || !TierStanding.hasTierData()) {
            return currentWeight;
        }
        Species sp = speciesOf(spawnDetail);
        if (sp == null) return currentWeight;

        if (!TierStanding.isKnown(sp)) {
            return BLOCK_UNKNOWN_SPECIES.get() ? 0.0f : currentWeight;
        }

        ServerPlayer player = resolvePlayer(ctx);
        if (player == null) return currentWeight;

        double factor = weightFactor(TierStanding.of(player, sp).tierGap());
        return (float)(currentWeight * factor);
    }

    @Override
    public void affectSpawn(@NotNull SpawnAction<?> action, @NotNull Entity entity) {
        if (!ConfigLoader.DO_LEVEL_SCALING.get() || !TierStanding.hasTierData()) {
            return;
        }
        if (!(entity instanceof PokemonEntity pe)){
            return;
        }
        if (!(pe.level() instanceof ServerLevel server)) {
            return;
        }

        ServerPlayer player = (ServerPlayer) server.getNearestPlayer(pe, 128.0);
        if (player == null) return;

        var poke = pe.getPokemon();
        Species sp = poke.getSpecies();
        if (!TierStanding.isKnown(sp)) return;

        TierStanding standing = TierStanding.of(player, sp);
        int speciesTierCap = standing.speciesCap();
        int playerCap = standing.playerCap();

        int capDelta = Math.max(0, playerCap - speciesTierCap);
        double capScaling = TIER_CAP_SCALING.get();
        int buffedCap = (int)Math.round(speciesTierCap + (capDelta*capScaling));

        double minScaling = MIN_LEVEL_SCALING.get();
        double avgScaling = AVG_LEVEL_SCALING.get();
        double maxScaling = MAX_LEVEL_SCALING.get();

        int min = Math.max(1, (int)Math.floor(minScaling * buffedCap));
        int mode = Math.max(1, (int)Math.round(avgScaling * buffedCap));
        int max = Math.max(1, (int)Math.floor(maxScaling * buffedCap));

        if (max > 100) {
            max = 100;
        }

        int newLevel = sampleTriangularInt(min, mode, max, pe.getRandom());

        int before = poke.getLevel();
        if (before != newLevel) {
            poke.setLevel(newLevel);
        }
    }
}
