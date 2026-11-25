package org.ppvon.ultimateCobblemonProgression.common.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierCatches;

import java.util.List;
import java.util.Optional;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Advancement trigger {@code ultimate-cobblemon-progression:catch_tier}: the player has caught every obtainable
 * Pokémon of a region's tier (see {@link TierCatches}), whether or not the catchWholeTier option is on.
 *
 * <pre>{"trigger": "ultimate-cobblemon-progression:catch_tier", "conditions": {"region": "kanto", "tier": 2}}</pre>
 *
 * Checked when the player's Pokédex changes and when they join, so players who caught them before the advancement
 * existed get it too.
 */
public final class CatchTierTrigger extends SimpleCriterionTrigger<CatchTierTrigger.Instance> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "catch_tier");
    public static final CatchTierTrigger INSTANCE = new CatchTierTrigger();

    private CatchTierTrigger() {}

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, ID, INSTANCE);
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Awards each of the player's not yet earned catch_tier criteria whose tier they've now caught in full. */
    public void check(ServerPlayer player) {
        if (!RegionRegistry.isActive() || !TierCatches.isReady()) return;
        // trigger() only asks about the player's unearned criteria, so only those tiers are worked out, each once.
        java.util.Map<String, Boolean> done = new java.util.HashMap<>();
        trigger(player, instance -> done.computeIfAbsent(instance.region + "/" + instance.tier, k -> {
            RegionDef region = RegionRegistry.byId(instance.region);
            return region != null && instance.tier >= 1 && instance.tier <= region.maxTier()
                    && TierCatches.targets(region, instance.tier) > 0
                    && TierCatches.missingAll(player, region, instance.tier).isEmpty();
        }));
    }

    public record Instance(Optional<ContextAwarePredicate> player, String region, int tier)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.fieldOf("region").forGetter(Instance::region),
                Codec.INT.fieldOf("tier").forGetter(Instance::tier)
        ).apply(i, Instance::new));
    }
}
