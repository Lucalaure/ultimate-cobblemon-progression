package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

/**
 * Optional difficulty rule ({@code gymTeamLimit}): a player may bring no more Pokémon to a gym battle than the
 * leader has. A battle against a gym-summoned leader doesn't start while the challenger's party is bigger.
 */
public final class GymTeamLimit {
    private GymTeamLimit() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, GymTeamLimit::onBattleStart);
    }

    public static boolean isOn() {
        return ConfigLoader.GYM_TEAM_LIMIT.get();
    }

    /** The line a challenger is told when a leader accepts, or null if the team size is unknown. */
    public static Component hint(String leaderName, int teamSize) {
        if (teamSize <= 0) return null;
        return Component.literal(leaderName + " uses " + pokemon(teamSize) + ", so you can bring at most "
                + teamSize + ".").withStyle(ChatFormatting.YELLOW);
    }

    private static Unit onBattleStart(BattleStartedEvent.Pre event) {
        if (!isOn() || event.isCanceled()) return Unit.INSTANCE;

        for (BattleActor actor : event.getBattle().getActors()) {
            if (!(actor instanceof EntityBackedBattleActor<?> backed)) continue;
            Entity leader = backed.getEntity();
            if (leader == null || !leader.getTags().contains(GymChallengeBlockEntity.LEADER_TAG)) continue;

            int limit = actor.getPokemonList().size();
            for (BattleActor other : event.getBattle().getActors()) {
                if (!(other instanceof PlayerBattleActor) || other.getSide() == actor.getSide()) continue;
                int brought = other.getPokemonList().size();
                if (brought <= limit) continue;

                int extra = brought - limit;
                event.setReason(Component.literal(leader.getDisplayName().getString() + " uses " + pokemon(limit)
                        + ", so you can bring at most " + limit + ". Put " + extra + " of yours in the PC first."));
                event.cancel();
                return Unit.INSTANCE;
            }
        }
        return Unit.INSTANCE;
    }

    private static String pokemon(int n) {
        return n + " Pokémon";
    }
}
