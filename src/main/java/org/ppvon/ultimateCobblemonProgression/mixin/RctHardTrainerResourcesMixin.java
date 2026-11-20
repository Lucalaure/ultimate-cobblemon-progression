package org.ppvon.ultimateCobblemonProgression.mixin;

import org.ppvon.ultimateCobblemonProgression.common.gym.GymDifficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.Set;

/**
 * A Hard league trainer ({@code <id>_hard}, see {@link GymDifficulty}) is the same person as its Normal version, so
 * Radical Cobblemon Trainers gives it the Normal version's look, speech bubbles, dialog and loot. Resource packs that
 * retexture the Normal trainer (like RCT Trainer Textures Plus) then cover the Hard one too. Its team and trainer data
 * stay its own.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the target is named by string.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.api.data.pack.DataPackManager", remap = false)
public abstract class RctHardTrainerResourcesMixin {
    /** Resource kinds a Hard trainer shares with its Normal version. */
    @Unique
    private static final Set<String> ucp$SHARED = Set.of("textures", "speech_bubbles", "dialogs", "loot_table");

    @Shadow
    @SuppressWarnings("rawtypes")
    public abstract Optional findResource(String trainerId, String context);

    @Inject(method = "findResource", at = @At("HEAD"), cancellable = true, require = 0)
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void ucp$hardTrainerLooksLikeNormal(String trainerId, String context, CallbackInfoReturnable<Optional> cir) {
        if (!GymDifficulty.isHard(trainerId) || !ucp$SHARED.contains(context)) return;
        String normal = trainerId.substring(0, trainerId.length() - GymDifficulty.HARD_SUFFIX.length());
        cir.setReturnValue(findResource(normal, context));
    }
}
