package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.progression.ProgressionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Re-checks progression whenever an advancement criterion is awarded or revoked, so tiers and regions
 * can be unlocked by advancements (e.g. Radical Cobblemon Trainers' "defeat leader" advancements).
 */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void ucp$onAward(AdvancementHolder advancement, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        ucp$onChanged(advancement, cir.getReturnValueZ());
    }

    @Inject(method = "revoke", at = @At("RETURN"))
    private void ucp$onRevoke(AdvancementHolder advancement, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        ucp$onChanged(advancement, cir.getReturnValueZ());
    }

    private void ucp$onChanged(AdvancementHolder advancement, boolean changed) {
        // Recipe unlocks are advancements too and fire constantly; they never gate progression.
        if (!changed || advancement.id().getPath().startsWith("recipes/")) return;
        ProgressionManager.onAdvancementsChanged(this.player);
    }
}
