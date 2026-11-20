package org.ppvon.ultimateCobblemonProgression.mixin;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.common.gym.GymRematch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A leader summoned for a rematch ({@link GymRematch#REMATCH_TAG}) has already been beaten, so Radical Cobblemon
 * Trainers' progression rules don't apply to it: it battles players whatever their current trainer series, the
 * trainers they've beaten in it, or their level cap (switching series clears RCT's record of earlier regions, and the
 * cap starts over). The rest still applies: the trainer isn't cooling down or already battling, the player isn't in
 * a battle and has a Pokémon able to fight, and this trainer entity hasn't already beaten or lost to them.
 *
 * <p>Applied only when RCT is installed. RCT isn't a compile-time dependency, so the target is named by string.
 */
@Pseudo
@Mixin(targets = "com.gitlab.srcmc.rctmod.world.entities.TrainerMob", remap = false)
public abstract class RctRematchBattleMixin {
    @Shadow
    public abstract int getCooldown();

    @Shadow
    public abstract boolean isInBattle();

    @Shadow
    public abstract boolean couldBattleAgainst(Entity entity);

    @Inject(method = "canBattleAgainst", at = @At("HEAD"), cancellable = true, require = 0)
    private void ucp$rematchIgnoresProgression(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!((Entity) (Object) this).getTags().contains(GymRematch.REMATCH_TAG)) return;
        if (!(entity instanceof ServerPlayer player)) {
            cir.setReturnValue(false);
            return;
        }
        boolean canFight = false;
        for (Pokemon pokemon : PlayerExtensionsKt.party(player)) {
            if (pokemon != null && !pokemon.isFainted()) {
                canFight = true;
                break;
            }
        }
        cir.setReturnValue(getCooldown() == 0 && !isInBattle() && canFight
                && BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player) == null
                && couldBattleAgainst(entity));
    }
}
