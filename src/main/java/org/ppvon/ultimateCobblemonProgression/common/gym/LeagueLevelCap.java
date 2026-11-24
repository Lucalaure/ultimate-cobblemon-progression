package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.List;

/**
 * The level cap of a player working through two leagues at once (a league as their second region).
 *
 * <p>Radical Cobblemon Trainers keeps one trainer series per player and caps levels at the strongest Pokémon of the
 * next trainer in it. That can't follow two leagues, so for these players CJP works the cap out itself and hands it to
 * RCT (see the RCT level cap mixins), the same way: each league's cap is the level of its next leader's strongest
 * Pokémon. The two leagues progress as a pair, and the player's cap is:
 * <ul>
 *   <li>with both in progress, the average of their caps, rounded up, so it rises whichever league they play, and
 *       rises most when they advance both (or, with twoLeagueCap set to "lowest", the lower of the two);</li>
 *   <li>with one finished first, the other's cap;</li>
 *   <li>with both complete, 100, until they pick where their journey goes next. Then their cap starts over with the
 *       new region, as for a single league (the finished second league joins their journey's history).</li>
 * </ul>
 * When a second league starts partway through a journey (or a region they'd picked becomes a league in an update),
 * the cap doesn't drop below the one they had with a single league until the average catches up (see {@link #update}).
 * Players with a single league keep RCT's own cap.
 */
public final class LeagueLevelCap {
    private static final int MAX = 100;

    private LeagueLevelCap() {}

    /**
     * Works the player's cap out again and stores it where the RCT mixins read it.
     *
     * <p>Starting a second league partway through a journey, or a world updating to a version where the player's second
     * region became a league, mustn't leave their team above a lower cap. So the first time a player has two leagues,
     * CJP remembers the cap they had with one (the floor), and their cap doesn't go below it. The floor is a bridge:
     * once the average reaches it, it's dropped for good (stored as 0), and it's cleared when the player picks their
     * next region after the pair, so the new region's cap starts over.
     */
    public static void update(ServerPlayer player) {
        var component = TrainerLevelComponents.KEY.get(player);
        int cap = compute(player);
        if (cap > 0) {
            int floor = component.getLevelCapFloor();
            if (floor < 0) {
                floor = journeyCap(player);
                component.setLevelCapFloor(floor);
            }
            if (cap >= floor) {
                if (floor > 0) component.setLevelCapFloor(0);
            } else {
                cap = floor;
            }
        }
        component.setLevelCapOverride(cap);
    }

    /** The cap CJP gives RCT for this player, or -1 to keep RCT's own. */
    public static int compute(ServerPlayer player) {
        if (!RegionRegistry.isActive() || !RctBridge.isLoaded()) return -1;
        RegionProgress progress = RegionProgressTracker.get(player);
        if (!progress.hasSecondLeague()) return -1;

        List<Integer> active = progress.activeLeagues();
        if (active.size() >= 2) {
            int a = leagueCap(player, progress, active.get(0)), b = leagueCap(player, progress, active.get(1));
            return ConfigLoader.TWO_LEAGUE_CAP_LOWEST.get() ? Math.min(a, b) : average(a, b);
        }
        if (active.size() == 1) return leagueCap(player, progress, active.get(0));
        return MAX;
    }

    /**
     * For a player working through two leagues: their first party Pokémon above their level cap, or null. Like RCT,
     * whose rule this replaces for these players, gym leaders won't battle them while one is.
     */
    public static Pokemon aboveCap(ServerPlayer player) {
        int cap = TrainerLevelComponents.KEY.get(player).getLevelCapOverride();
        if (cap <= 0) return null;
        for (Pokemon pokemon : PlayerExtensionsKt.party(player)) {
            if (pokemon != null && pokemon.getLevel() > cap) return pokemon;
        }
        return null;
    }

    /**
     * The cap the player had with one league: that of the journey league in progress (the one RCT's series follows),
     * worked out from CJP's own records so it doesn't depend on RCT having loaded the player yet.
     */
    private static int journeyCap(ServerPlayer player) {
        RegionProgress progress = RegionProgressTracker.get(player);
        int cap = 0;
        for (int r : progress.activeLeagues()) {
            if (r != progress.secondRegion()) cap = Math.max(cap, leagueCap(player, progress, r));
        }
        return cap;
    }

    private static int average(int a, int b) {
        return (a + b + 1) / 2;
    }

    /**
     * One league's cap: the level of the strongest Pokémon of its next unbeaten trainer (never below its current
     * tier's cap), or its Champion's once it's complete.
     */
    static int leagueCap(ServerPlayer player, RegionProgress progress, int r) {
        RegionDef region = RegionRegistry.get(r);
        int step = progress.step(r);
        if (progress.isCompleted(r)) return championLevel(player, region);
        int tierCap = region.levelCap(Math.max(1, step));
        TierDef next = region.tiers.get(step + 1);
        if (next == null) return championLevel(player, region);
        if (next.trainers.isEmpty()) return tierCap;

        int strongest = -1;
        for (List<String> group : next.trainers) {
            int ace = RctBridge.aceLevel(player.server, group.get(0));
            if (!beaten(player, group)) return Math.max(tierCap, ace);
            strongest = Math.max(strongest, ace);
        }
        // Every trainer of the next tier is beaten, but its other requirements aren't met yet.
        return Math.max(tierCap, strongest);
    }

    /** The level of the strongest Pokémon of the league's last trainer: its Champion. */
    static int championLevel(ServerPlayer player, RegionDef region) {
        for (int t = region.maxTier(); t >= 1; t--) {
            List<List<String>> trainers = region.tiers.get(t).trainers;
            if (!trainers.isEmpty()) {
                int ace = RctBridge.aceLevel(player.server, trainers.get(trainers.size() - 1).get(0));
                if (ace > 0) return ace;
            }
        }
        return MAX;
    }

    private static boolean beaten(ServerPlayer player, List<String> group) {
        // A slot lists its Normal and Hard versions; beating either counts.
        for (String id : group) {
            if (RctBridge.defeats(player, id) > 0) return true;
        }
        return false;
    }

}
