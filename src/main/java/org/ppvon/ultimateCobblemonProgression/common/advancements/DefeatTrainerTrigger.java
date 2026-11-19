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

import java.util.List;
import java.util.Optional;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Advancement trigger {@code ultimate-cobblemon-progression:defeat_trainer}: the player beat a Radical Cobblemon
 * Trainers trainer. It's fired from RCT's own "trainer defeated" trigger (see {@code RctDefeatCountTriggerMixin}).
 *
 * <pre>{"trigger": "ultimate-cobblemon-progression:defeat_trainer", "conditions": {"trainers": ["leader_brock_019e"]}}</pre>
 *
 * {@code trainers} lists RCT trainer ids, any one of which counts; leave it out to match every trainer.
 */
public final class DefeatTrainerTrigger extends SimpleCriterionTrigger<DefeatTrainerTrigger.Instance> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MOD_ID, "defeat_trainer");
    public static final DefeatTrainerTrigger INSTANCE = new DefeatTrainerTrigger();

    private DefeatTrainerTrigger() {}

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, ID, INSTANCE);
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Awards every matching criterion: the player just beat the trainer with this RCT id. */
    public void trigger(ServerPlayer player, String trainerId) {
        trigger(player, instance -> instance.matches(trainerId));
    }

    public record Instance(Optional<ContextAwarePredicate> player, List<String> trainers)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.listOf().optionalFieldOf("trainers", List.of()).forGetter(Instance::trainers)
        ).apply(i, Instance::new));

        boolean matches(String trainerId) {
            return trainers.isEmpty() || trainers.contains(trainerId);
        }
    }
}
