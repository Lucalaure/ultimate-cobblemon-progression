package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymChallengeBlockEntity;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymLegality;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymRematch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When a player talks to a gym leader a Gym Challenge block summoned, and CJP knows the battle can't start (Pokémon
 * the legality check blocks, or one above the level cap), the leader says why instead of Radical Cobblemon Trainers'
 * generic line, or nothing at all (see {@link GymLegality#refusal}). Rematches aren't checked, as elsewhere.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the target is named by string.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.world.entities.TrainerMob", remap = false)
public abstract class RctGymLeaderRefusalMixin {
    @Inject(method = "startBattleWith", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$explainRefusal(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Entity self = (Entity) (Object) this;
        if (!self.getTags().contains(GymChallengeBlockEntity.LEADER_TAG)) return;
        if (self.getTags().contains(GymRematch.REMATCH_TAG)) return;

        var why = GymLegality.refusal(serverPlayer, self.getDisplayName().getString());
        if (why == null) return;
        serverPlayer.displayClientMessage(why, false);
        ci.cancel();
    }
}
