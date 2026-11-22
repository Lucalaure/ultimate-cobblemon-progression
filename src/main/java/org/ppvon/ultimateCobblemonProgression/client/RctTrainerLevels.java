package org.ppvon.ultimateCobblemonProgression.client;

import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The level of a Radical Cobblemon Trainers trainer, as shown next to its name: the level of its strongest Pokémon,
 * which is also what RCT uses to match trainers to players and to pick their loot. RCT sends every trainer's data
 * (team included) to the client, so this works without asking the server.
 *
 * <p>RCT isn't a compile-time dependency, so its classes are reached by reflection. If any of it can't be found (RCT
 * missing, or a version that changed its API), levels just aren't shown.
 */
public final class RctTrainerLevels {
    private static final String TRAINER_MOB = "com.gitlab.srcmc.rctmod.world.entities.TrainerMob";
    private static final String RCT_MOD = "com.gitlab.srcmc.rctmod.api.RCTMod";

    /** Strongest level per trainer data object. RCT replaces those objects when data reloads, so stale entries drop out. */
    private static final Map<Object, Integer> LEVELS = new WeakHashMap<>();

    private static boolean unavailable;
    private static Class<?> trainerMob;
    private static Method getTrainerId, getInstance, getTrainerManager, getData, getTrainerTeam, getTeam, getLevel;

    private RctTrainerLevels() {}

    /** The level of the trainer's strongest Pokémon, or -1 if {@code entity} isn't an RCT trainer or its team isn't known. */
    public static int strongestLevel(Entity entity) {
        if (unavailable || !init() || !trainerMob.isInstance(entity)) return -1;
        try {
            String trainerId = (String) getTrainerId.invoke(entity);
            if (trainerId == null || trainerId.isEmpty()) return -1;
            Object manager = getTrainerManager.invoke(getInstance.invoke(null));
            Object data = getData.invoke(manager, trainerId);
            if (data == null) return -1;
            Integer cached = LEVELS.get(data);
            if (cached != null) return cached;
            int level = -1;
            for (Object pokemon : (List<?>) getTeam.invoke(getTrainerTeam.invoke(data))) {
                if (getLevel == null) getLevel = pokemon.getClass().getMethod("getLevel");
                level = Math.max(level, (int) getLevel.invoke(pokemon));
            }
            // An empty team means the data hasn't arrived yet; look again next frame.
            if (level > 0) LEVELS.put(data, level);
            return level;
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return -1;
        }
    }

    private static boolean init() {
        if (trainerMob != null) return true;
        try {
            trainerMob = Class.forName(TRAINER_MOB);
            getTrainerId = trainerMob.getMethod("getTrainerId");
            getInstance = Class.forName(RCT_MOD).getMethod("getInstance");
            getTrainerManager = getInstance.getReturnType().getMethod("getTrainerManager");
            getData = getTrainerManager.getReturnType().getMethod("getData", String.class);
            getTrainerTeam = getData.getReturnType().getMethod("getTrainerTeam");
            getTeam = getTrainerTeam.getReturnType().getMethod("getTeam");
            return true;
        } catch (ClassNotFoundException e) {
            // RCT isn't installed.
            unavailable = true;
            trainerMob = null;
            return false;
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable(e);
            return false;
        }
    }

    private static void disable(Exception e) {
        unavailable = true;
        trainerMob = null;
        UltimateCobblemonProgression.LOG.warn("Couldn't read Radical Cobblemon Trainers trainer levels; they won't be shown next to trainer names", e);
    }
}
