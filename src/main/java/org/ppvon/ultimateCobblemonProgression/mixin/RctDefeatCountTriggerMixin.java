package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.advancements.DefeatTrainerTrigger;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymDifficulty;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Radical Cobblemon Trainers fires its {@code rctmod:defeat_count} advancement trigger every time a player beats one
 * of its trainers. UCP hooks in there to fire its own {@link DefeatTrainerTrigger}, and for the trainers of UCP's
 * leagues (every trainer listed in a region's tiers) it stops RCT's trigger, so beating a UCP gym leader only ever
 * awards UCP's advancements, never RCT's.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the trainer is taken as an
 * {@link Entity} and its id read through RCT's public {@code TrainerMob.getTrainerId()}.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.advancements.criteria.DefeatCountTrigger", remap = false)
public abstract class RctDefeatCountTriggerMixin {
    @Unique
    private static Method ucp$getTrainerId;

    @Inject(method = "trigger", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$onTrainerDefeated(ServerPlayer player, @Coerce Entity trainer, CallbackInfo ci) {
        String trainerId = ucp$trainerId(trainer);
        if (trainerId == null) return;
        GymDifficulty.onTrainerDefeated(player, trainerId);
        DefeatTrainerTrigger.INSTANCE.trigger(player, trainerId);
        if (RegionRegistry.isLeagueTrainer(trainerId)) ci.cancel();
    }

    @Unique
    private static String ucp$trainerId(Entity trainer) {
        try {
            if (ucp$getTrainerId == null) ucp$getTrainerId = trainer.getClass().getMethod("getTrainerId");
            return (String) ucp$getTrainerId.invoke(trainer);
        } catch (ReflectiveOperationException | ClassCastException e) {
            UltimateCobblemonProgression.LOG.error("Couldn't read the id of a Radical Cobblemon Trainers trainer", e);
            return null;
        }
    }
}
