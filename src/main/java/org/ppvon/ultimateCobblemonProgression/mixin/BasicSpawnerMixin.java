// src/main/java/org/ppvon/ultimateCobblemonProgression/mixin/BasicSpawnerMixin.java
package org.ppvon.ultimateCobblemonProgression.mixin;

import com.cobblemon.mod.common.api.spawning.detail.SpawnPool;
import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.spawner.BasicSpawner;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = BasicSpawner.class, remap = false)
public abstract class BasicSpawnerMixin {

    @Shadow
    public abstract List<SpawningInfluence> getInfluences();

    // Cobblemon 1.8 added the buckets parameter; target the primary constructor explicitly
    // since Kotlin also generates a synthetic default-args constructor that delegates to it.
    @Inject(
            method = "<init>(Ljava/lang/String;Lcom/cobblemon/mod/common/api/spawning/detail/SpawnPool;FLjava/util/Map;)V",
            at = @At("TAIL")
    )
    private void ucp$attachTrainerLevelInfluence(
            String name,
            SpawnPool spawnPool,
            float maxPokemonPerChunk,
            Map<String, Float> buckets,
            CallbackInfo ci
    ) {
        List<SpawningInfluence> influences = this.getInfluences();

        boolean alreadyPresent = influences.stream()
                .anyMatch(inf -> inf instanceof TrainerLevelInfluence);

        if (!alreadyPresent) {
            influences.addFirst(new TrainerLevelInfluence());
        }
    }
}
