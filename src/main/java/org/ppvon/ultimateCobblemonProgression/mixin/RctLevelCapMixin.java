package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.world.entity.player.Player;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.gym.LeagueLevelCap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Two leagues at once: Radical Cobblemon Trainers' level cap follows one trainer series, so for a player with a league
 * as their second region it's replaced by CJP's (see {@link LeagueLevelCap}), here in the server, which applies it to every Pokémon's experience.
 * Other players keep RCT's own cap.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the target is named by string.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.api.data.save.TrainerPlayerData", remap = false)
public abstract class RctLevelCapMixin {
    @Shadow
    private Player player;

    @Inject(method = "getLevelCap", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$twoLeagueCap(CallbackInfoReturnable<Integer> cir) {
        if (player == null) return;
        int cap = TrainerLevelComponents.KEY.get(player).getLevelCapOverride();
        if (cap > 0) cir.setReturnValue(cap);
    }
}
