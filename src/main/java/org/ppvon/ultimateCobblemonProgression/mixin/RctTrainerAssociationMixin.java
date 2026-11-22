package org.ppvon.ultimateCobblemonProgression.mixin;

import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Radical Cobblemon Trainers spawns a Trainer Association NPC near players who have no series or have finished
 * theirs, and it lets them join a series or switch to freeroam (level cap 100). In region mode UCP decides each
 * player's series, and RCT's series cap is the player's level cap, so the NPC would let players skip past UCP's
 * journey. While region mode is active, RCT's {@code spawnTrainerAssociation} setting reads as off, so no new
 * ones spawn. One that had already spawned in an existing world still despawns once no player is near it.
 *
 * <p>Applied only when RCT is installed. Without region mode, RCT's own setting applies.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.config.ServerConfig", remap = false)
public abstract class RctTrainerAssociationMixin {
    @Inject(method = "spawnTrainerAssociation", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$noTrainerAssociationInRegionMode(CallbackInfoReturnable<Boolean> cir) {
        if (RegionRegistry.isActive()) cir.setReturnValue(false);
    }
}
