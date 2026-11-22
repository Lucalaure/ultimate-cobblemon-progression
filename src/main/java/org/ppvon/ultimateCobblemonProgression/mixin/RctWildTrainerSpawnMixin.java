package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.ppvon.ultimateCobblemonProgression.common.regions.WildTrainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Region mode: Radical Cobblemon Trainers only spawns a wild trainer near a player who has its region open (see
 * {@link WildTrainers#maySpawnFor}). RCT weighs every trainer it could spawn for the player and leaves out the ones
 * weighing 0, so those trainers drop out of the pick. Trainers summoned by commands, gyms or spawner blocks aren't
 * affected.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the target is named by string and its
 * trainer data taken as an Object.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.api.service.TrainerSpawner", remap = false)
public abstract class RctWildTrainerSpawnMixin {
    @Inject(method = "computeWeight", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$onlyOpenRegions(Player player, String trainerId, @Coerce Object data, CallbackInfoReturnable<Double> cir) {
        if (player instanceof ServerPlayer serverPlayer && !WildTrainers.maySpawnFor(serverPlayer, trainerId)) {
            cir.setReturnValue(0.0);
        }
    }
}
