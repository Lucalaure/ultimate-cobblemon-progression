package org.ppvon.ultimateCobblemonProgression.common.progression.dex;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokedexDataChangedEvent;
import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager;
import com.cobblemon.mod.common.api.pokedex.CaughtCount;
import com.cobblemon.mod.common.api.pokedex.SeenCount;
import kotlin.Unit;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.progression.ProgressionManager;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.UUID;

import static com.cobblemon.mod.common.util.PlayerExtensionsKt.getPlayer;

public final class DexProgressionListener {

    private DexProgressionListener() {}

    public static void register() {
        CobblemonEvents.POKEDEX_DATA_CHANGED_POST.subscribe(Priority.NORMAL, DexProgressionListener::onDexChanged);
    }

    private static Unit onDexChanged(PokedexDataChangedEvent event) {
        if (RegionRegistry.isActive()) {
            // Region tiers can also require Pokédex counts.
            ServerPlayer player = getPlayer(event.getPlayerUUID());
            if (player != null) RegionProgressTracker.refresh(player);
            return Unit.INSTANCE;
        }
        if(!ConfigLoader.DO_DEX_PROGRESSION.get()) {
            return Unit.INSTANCE;
        }
        AbstractPokedexManager manager = event.getPokedexManager();

        int caughtCount = manager.getGlobalCalculatedValue(CaughtCount.INSTANCE);
        int seenCount = manager.getGlobalCalculatedValue(SeenCount.INSTANCE);

        UUID playerUUID = event.getPlayerUUID();
        ServerPlayer player = getPlayer(playerUUID);
        if (player == null) return Unit.INSTANCE;

        ProgressionManager.attemptLevelUp(player, seenCount, caughtCount);

        return Unit.INSTANCE;
    }
}
