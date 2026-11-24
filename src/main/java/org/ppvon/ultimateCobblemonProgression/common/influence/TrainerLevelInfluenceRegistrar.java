package org.ppvon.ultimateCobblemonProgression.common.influence;

import com.cobblemon.mod.common.api.spawning.influence.SpawningInfluence;
import com.cobblemon.mod.common.api.spawning.spawner.FishingSpawnerFactory;
import kotlin.jvm.functions.Function1;

import java.util.ArrayList;
import java.util.List;

/**
 * World and Poké Snack spawns get {@link TrainerLevelInfluence} from their spawner (see BasicSpawnerMixin).
 * Fishing doesn't: the Poké Rod builds its spawn position with only the fishing position influences and
 * never consults the fishing spawner's own list, so the influence has to be added there too.
 */
public class TrainerLevelInfluenceRegistrar {
    private static final Function1<FishingSpawnerFactory.Context, List<SpawningInfluence>> FISHING_BUILDER =
            ctx -> List.of(new TrainerLevelInfluence());

    private static boolean registered = false;

    public static void registerOnce() {
        if (registered) return;
        // Copied into a fresh list in case Cobblemon's is read-only. Added last so our level scaling
        // runs after Cobblemon's own fishing level influence and has the final say.
        var builders = new ArrayList<>(FishingSpawnerFactory.INSTANCE.getPositionInfluenceBuilders());
        if (!builders.contains(FISHING_BUILDER)) {
            builders.add(FISHING_BUILDER);
        }
        FishingSpawnerFactory.INSTANCE.setPositionInfluenceBuilders(builders);
        registered = true;
    }
}
