package org.ppvon.ultimateCobblemonProgression.mixin;

import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnAction;
import com.cobblemon.mod.common.api.spawning.position.SpawnablePosition;
import com.cobblemon.mod.common.api.spawning.selection.SpawnSelectionData;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Leaves locked species out of a herd before Cobblemon picks its members, so a herd led by a Pokémon the
 * player can't meet yet still forms from the ones they can (see {@link TrainerLevelInfluence#allowedHerdMembers}).
 */
@Mixin(value = PokemonHerdSpawnDetail.class, remap = false)
public abstract class PokemonHerdSpawnDetailMixin {

    // Runs before each member is picked (and before onSelection, which uses the same members).
    @Inject(
            method = "createSpawnAction(Lcom/cobblemon/mod/common/api/spawning/position/SpawnablePosition;Ljava/lang/String;Lcom/cobblemon/mod/common/api/spawning/selection/SpawnSelectionData;)Lcom/cobblemon/mod/common/api/spawning/detail/SpawnAction;",
            at = @At("HEAD")
    )
    private void ucp$rememberHerdPlayer(SpawnablePosition position, String bucket, SpawnSelectionData selection,
                                        CallbackInfoReturnable<SpawnAction<?>> cir) {
        TrainerLevelInfluence.rememberHerdPlayer(selection, position);
    }

    // Every leader/follower choice and the "can the herd grow" check go through this list.
    @Inject(
            method = "getValidHerdMembers(Lcom/cobblemon/mod/common/api/spawning/selection/SpawnSelectionData;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void ucp$dropLockedMembers(SpawnSelectionData selection,
                                       CallbackInfoReturnable<List<PokemonHerdSpawnDetail.Herdable>> cir) {
        cir.setReturnValue(TrainerLevelInfluence.allowedHerdMembers(cir.getReturnValue(), selection));
    }
}
