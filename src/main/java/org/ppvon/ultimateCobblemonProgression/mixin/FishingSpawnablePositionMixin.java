package org.ppvon.ultimateCobblemonProgression.mixin;

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.position.FishingSpawnablePosition;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Gives every fishing spawn position the {@link TrainerLevelInfluence}, so fishing only offers Pokémon of the player's
 * open regions and reached tiers, at their levels and weights.
 *
 * <p>Fishing positions carry their own influences: the spawner's aren't consulted. Cobblemon builds them from
 * FishingSpawnerFactory, but other mods build their own list (Cobblenav's Fishingnav, to show what can bite), so the
 * influence is added here, whoever makes the position.
 */
@Mixin(value = FishingSpawnablePosition.class, remap = false)
public abstract class FishingSpawnablePositionMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void ucp$addTrainerLevelInfluence(CallbackInfo ci) {
        List<SpawningInfluence> influences = ((SpawnablePosition) (Object) this).getInfluences();
        if (influences.stream().anyMatch(i -> i instanceof TrainerLevelInfluence)) return;
        try {
            // Last, so its levels have the final say over Cobblemon's fishing level influence.
            influences.add(new TrainerLevelInfluence());
        } catch (UnsupportedOperationException e) {
            // A read-only list: nothing to add to. Real spawns are still checked by TrainerLevelInfluence's spawn guard.
        }
    }
}
