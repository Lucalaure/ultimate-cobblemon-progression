package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.common.progression.TierStanding;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Optional rule ({@code gymLegalityCheck}): the Pokémon a player brings to a gym battle must be ones they could have
 * met in the wild by now. Eggs, trades, fossils and other mods can hand a player a Pokémon of a tier, or a region, they
 * haven't unlocked; this keeps those out of gym battles.
 *
 * <p>A Pokémon is out of reach when it has a tier and the player hasn't unlocked that tier (in region mode: in its
 * region, see {@link TierStanding}). Pokémon without a tier are always allowed, and so is the player's starter, since
 * Cobblemon offers starters from every region. Rematches aren't checked: they don't change progress.
 *
 * <ul>
 *   <li>{@code "off"} (the default): no check.</li>
 *   <li>{@code "warn"}: the leader names the Pokémon when it accepts the challenge, but the battle goes ahead.</li>
 *   <li>{@code "block"}: the same warning, and the battle doesn't start while one is in the party.</li>
 * </ul>
 */
public final class GymLegality {
    public enum Mode { OFF, WARN, BLOCK }

    /** How many Pokémon a message names before it says "and N more". */
    private static final int MAX_NAMED = 3;

    private GymLegality() {}

    public static void register() {
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, GymLegality::onBattleStart);
    }

    public static Mode mode() {
        String value = ConfigLoader.GYM_LEGALITY_CHECK.get();
        if ("block".equalsIgnoreCase(value)) return Mode.BLOCK;
        if ("warn".equalsIgnoreCase(value)) return Mode.WARN;
        return Mode.OFF;
    }

    /** Whether the player could have this Pokémon by now (see the class comment). */
    public static boolean isLegal(ServerPlayer player, Pokemon pokemon) {
        if (pokemon == null || isStarter(player, pokemon)) return true;
        var aspects = pokemon.getAspects();
        if (!TierStanding.hasTierData() || !TierStanding.isKnown(pokemon.getSpecies(), aspects)) return true;
        return TierStanding.of(player, pokemon.getSpecies(), aspects).allowed();
    }

    /** The Pokémon in the player's party that are out of reach, in party order. */
    public static List<Pokemon> outOfReach(ServerPlayer player) {
        List<Pokemon> out = new ArrayList<>();
        for (Pokemon pokemon : PlayerExtensionsKt.party(player)) {
            if (pokemon != null && !isLegal(player, pokemon)) out.add(pokemon);
        }
        return out;
    }

    /** The line a challenger is told when a leader accepts, or null if the check is off or the party is fine. */
    public static Component hint(ServerPlayer player, String leaderName) {
        Mode mode = mode();
        if (mode == Mode.OFF) return null;
        List<Pokemon> out = outOfReach(player);
        if (out.isEmpty()) return null;
        String names = names(out);
        String verb = out.size() == 1 ? "isn't" : "aren't";
        String text = mode == Mode.BLOCK
                ? names + " " + verb + " from a tier you've unlocked yet. " + leaderName
                        + " won't battle you until you put " + (out.size() == 1 ? "it" : "them") + " in the PC."
                : names + " " + verb + " from a tier you've unlocked yet. " + leaderName + " will battle you anyway.";
        return Component.literal(text).withStyle(mode == Mode.BLOCK ? ChatFormatting.RED : ChatFormatting.YELLOW);
    }

    private static Unit onBattleStart(BattleStartedEvent.Pre event) {
        if (mode() != Mode.BLOCK || event.isCanceled()) return Unit.INSTANCE;

        for (BattleActor actor : event.getBattle().getActors()) {
            if (!(actor instanceof EntityBackedBattleActor<?> backed)) continue;
            Entity leader = backed.getEntity();
            if (leader == null || !leader.getTags().contains(GymChallengeBlockEntity.LEADER_TAG)) continue;
            if (leader.getTags().contains(GymRematch.REMATCH_TAG)) continue;

            for (BattleActor other : event.getBattle().getActors()) {
                if (!(other instanceof PlayerBattleActor playerActor) || other.getSide() == actor.getSide()) continue;
                ServerPlayer player = playerActor.getEntity();
                if (player == null) continue;

                List<Pokemon> out = new ArrayList<>();
                for (BattlePokemon battlePokemon : other.getPokemonList()) {
                    Pokemon pokemon = battlePokemon.getOriginalPokemon();
                    if (!isLegal(player, pokemon)) out.add(pokemon);
                }
                if (out.isEmpty()) continue;

                MutableComponent reason = Component.literal(leader.getDisplayName().getString()
                        + " won't battle " + names(out) + ": " + (out.size() == 1 ? "it isn't" : "they aren't")
                        + " from a tier you've unlocked yet. Put " + (out.size() == 1 ? "it" : "them")
                        + " in the PC first.");
                event.setReason(reason);
                event.cancel();
                return Unit.INSTANCE;
            }
        }
        return Unit.INSTANCE;
    }

    private static boolean isStarter(ServerPlayer player, Pokemon pokemon) {
        try {
            UUID starter = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player).getStarterUUID();
            return starter != null && starter.equals(pokemon.getUuid());
        } catch (RuntimeException e) {
            // Player data not loaded (e.g. a fake player): don't hold the battle up over it.
            return false;
        }
    }

    /** "Piplup", "Piplup and Eevee", "Piplup, Eevee and Riolu", or "Piplup, Eevee, Riolu and 2 more". */
    static String names(List<Pokemon> pokemon) {
        List<String> names = new ArrayList<>();
        for (Pokemon p : pokemon) names.add(p.getDisplayName(false).getString());
        return joinNames(names);
    }

    /** Joins names as in {@link #names}, naming at most {@value #MAX_NAMED}. */
    static String joinNames(List<String> names) {
        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);
        if (names.size() > MAX_NAMED) {
            return String.join(", ", names.subList(0, MAX_NAMED)) + " and " + (names.size() - MAX_NAMED) + " more";
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }
}
