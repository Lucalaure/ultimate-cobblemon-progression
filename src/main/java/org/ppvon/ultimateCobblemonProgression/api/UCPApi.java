package org.ppvon.ultimateCobblemonProgression.api;

import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.UltimateCobblemonProgressionTierExt;

import java.util.*;
import java.util.stream.IntStream;

/**
 * The public API surface for the Cobblemon Journey Progression (CJP) mod, formerly Ultimate Cobblemon Progression.
 * <p>
     * This class exposes stable, safe methods that other mods can use to:
     * <ul>
     *     <li>Read or modify a player's trainer level</li>
     *     <li>Access tier and level cap data</li>
     *     <li>Inspect which Pokémon species belong to each tier</li>
     * </ul>
     * All methods are static and can be safely called from either client or server context,
     * provided the trainer component is synchronized via CCA.
 *
 * @since 1.0.0
 */
public final class UCPApi {
    private UCPApi() {}

    /**
     * Gets the current trainer level of the specified player.
     * <p>
     * This represents the player's progression tier. The value increases as the player
     * unlocks higher tiers of Pokémon and corresponding level caps.
     *
     * @param player the player whose trainer level should be retrieved
     * @return the current trainer level of the player (e.g., 1–7)
     * @since 1.0.0
     */
    public static int getTrainerLevel(@NotNull Player player) {
        return TrainerLevelComponents.KEY.get(player).getLevel();
    }

    /**
     * Increases a player's trainer level by one.
     * <p>
     * This method is equivalent to leveling up the trainer, unlocking the next
     * Pokémon tier and raising their level cap. It does not perform validation,
     * so calling mods should ensure the player is eligible to advance.
     *
     * @param player the player whose trainer level will be incremented
     * @since 1.0.0
     */
    public static void trainerLevelUp(@NotNull Player player) {
        int level = TrainerLevelComponents.KEY.get(player).getLevel();
        TrainerLevelComponents.KEY.get(player).setLevel(level + 1);
    }

    /**
     * Returns the maximum configured trainer tier.
     * <p>
     * This value represents the highest available tier index in the tier registry
     * (for example, 7 if there are seven progression tiers defined).
     *
     * @return the highest configured tier index
     * @since 1.0.0
     */
    public static int getMaxTier() {
        return TierRegistry.maxTier();
    }

    /**
     * Gets the level cap associated with a specific trainer tier.
     * <p>
     * For example, if tier 1 Pokémon are capped at level 15, calling
     * {@code getLevelCapForTier(1)} will return 15.
     *
     * @param tier the tier index (1-based)
     * @return the level cap value for the given tier
     * @throws NullPointerException if the requested tier does not exist in the registry
     * @since 1.0.0
     */
    public static int getLevelCapForTier(int tier) {
        return TierRegistry.getCapForTier(tier);
    }

    /**
     * Determines the minimum trainer tier required for a given Pokémon species.
     * <p>
     * This can be used to gate spawning, capturing, or battling logic based on
     * the player's current progression tier.
     *
     * @param speciesId the {@link ResourceLocation} identifier for a Pokémon species
     * @return the minimum tier that species belongs to, or {@link Integer#MAX_VALUE} if unknown
     * @since 1.0.0
     */
    public static int getMinTierFor(@NotNull ResourceLocation speciesId) {
        return TierRegistry.getMinTierFor(speciesId);
    }

    /**
     * Retrieves a single tier definition by index.
     * <p>
     * The returned {@link TierDefinition} contains the tier index, its level cap,
     * and the full set of Pokémon species belonging to that tier.
     *
     * @param index the 1-based tier index to retrieve
     * @return an {@link Optional} containing the tier definition if found, or an empty optional otherwise
     * @since 1.0.0
     */
    public static Optional<TierDefinition> getTier(int index) {
        return TierRegistry.get(index).map(td ->
                new TierDefinition(td.index, td.levelCap, td.species)
        );
    }

    /**
     * Retrieves all tier definitions in ascending order.
     * <p>
     * This includes all defined tiers from 1 through {@link #getMaxTier()},
     * skipping any missing tiers. The returned list is immutable.
     *
     * @return an unmodifiable list of all defined {@link TierDefinition}s
     * @since 1.0.0
     */
    public static List<TierDefinition> getAllTiers() {
        int max = getMaxTier();
        if (max <= 0) return List.of();
        List<TierDefinition> out = new ArrayList<>(max);
        IntStream.rangeClosed(1, max).forEach(i ->
                TierRegistry.get(i).ifPresent(td ->
                        out.add(new TierDefinition(td.index, td.levelCap, td.species))
                )
        );
        return Collections.unmodifiableList(out);
    }

    /**
     * Returns the version tag for the UCP public API.
     * <p>
     * This version reflects the API surface only. It may differ from the mod's
     * runtime version. Increment this when breaking changes are introduced to
     * the API methods or signatures.
     *
     * @return the API version string (e.g., {@code "1.0.0"})
     * @since 1.0.0
     */
    @ApiStatus.AvailableSince("1.0.0")
    public static String apiVersion() {
        return "1.0.0";
    }

    /**
     * Returns the trainer tier value assigned to the given Cobblemon {@link Species}.
     * <p>
     * This value is stored in a runtime-only field injected into {@code Species}
     * via the {@link org.ppvon.ultimateCobblemonProgression.mixin.SpeciesTierMixin}.
     * It is not part of Cobblemon's base data or CODECs.
     * <p>
     * When no tier has been applied, {@code -1} is returned.
     *
     * @param sp the species instance
     * @return the numeric tier for this species, or {@code -1} if none
     * @since 1.2.0
     */
    public static int getTier(Species sp) {
        return ((UltimateCobblemonProgressionTierExt) (Object) sp).ucp$getTier();
    }

    /**
     * Sets the trainer tier value on the given Cobblemon {@link Species}.
     * <p>
     * This method assigns the in-memory tier field added by the
     * {@link org.ppvon.ultimateCobblemonProgression.mixin.SpeciesTierMixin}.
     * It should only be called on the server thread after datapack reloads
     * (e.g. from {@code TierSpeciesApplier.applyFromRegistry()}).
     * <p>
     * The cast through {@code Object} is required because the
     * {@link UltimateCobblemonProgressionTierExt} interface is applied at runtime
     * by Mixin and is not visible to the Java compiler.
     *
     * @param sp   the species instance to modify
     * @param tier the numeric tier to assign, or {@code -1} to clear
     * @since 1.2.0
     */
    public static void setTier(Species sp, int tier) {
        ((UltimateCobblemonProgressionTierExt) (Object) sp).ucp$setTier(tier);
    }
}
