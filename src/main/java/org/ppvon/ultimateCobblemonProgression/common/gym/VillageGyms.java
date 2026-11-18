package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;
import org.ppvon.ultimateCobblemonProgression.mixin.StructureTemplatePoolAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Adds the gyms to each vanilla village style's town-centre pool. A village has exactly one centre,
 * so it can never get more than one gym, and gyms aren't placed anywhere else.
 * The gym buildings come from Radical Gyms & Structures (MIT); see scripts/convert-rgs-gyms.py.
 */
public final class VillageGyms {
    private static final List<String> STYLES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    private static final List<String> GYMS = List.of(
            "pewter", "cerulean", "vermilion", "celadon", "fuchsia", "saffron", "cinnabar", "blackthorn");
    private static final double MAX_CHANCE = 0.95;
    private static final Set<StructureTemplatePoolAccessor> DONE = Collections.newSetFromMap(new WeakHashMap<>());
    /** The gym pool elements UCP added, so village generation can recognise a gym centre. */
    private static final Set<StructurePoolElement> GYM_ELEMENTS = Collections.newSetFromMap(new WeakHashMap<>());

    private VillageGyms() {}

    /** Whether a village piece is one of UCP's gyms. */
    public static boolean isGym(StructurePoolElement element) {
        return GYM_ELEMENTS.contains(element);
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(VillageGyms::addGyms);
    }

    private static void addGyms(MinecraftServer server) {
        if (!FabricLoader.getInstance().isModLoaded("cobblefurnies")) {
            UltimateCobblemonProgression.LOG.warn("CobbleFurnies isn't installed, so no gyms will generate in villages (the gym buildings use its furniture)");
            return;
        }
        double chance = Math.min(MAX_CHANCE, ConfigLoader.GYM_VILLAGE_CHANCE.get());
        if (!(chance > 0)) {
            UltimateCobblemonProgression.LOG.info("Village gyms are turned off (gymVillageChance is 0)");
            return;
        }

        Registry<StructureTemplatePool> pools = server.registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        Holder<StructureProcessorList> noProcessors = server.registryAccess()
                .registryOrThrow(Registries.PROCESSOR_LIST).getHolderOrThrow(ProcessorLists.EMPTY);

        for (String style : STYLES) {
            ResourceLocation poolId = ResourceLocation.withDefaultNamespace("village/" + style + "/town_centers");
            StructureTemplatePool pool = pools.get(poolId);
            if (pool == null) {
                UltimateCobblemonProgression.LOG.warn("Village pool {} is missing; no {} gyms", poolId, style);
                continue;
            }
            add((StructureTemplatePoolAccessor) pool, style, noProcessors, chance);
        }
        UltimateCobblemonProgression.LOG.info("Added gyms to village centres ({}% of new villages)", Math.round(chance * 100));
    }

    private static void add(StructureTemplatePoolAccessor pool, String style,
                            Holder<StructureProcessorList> processors, double chance) {
        // A pool lives as long as its world's registries; don't add to it twice.
        if (!DONE.add(pool)) return;
        List<Pair<StructurePoolElement, Integer>> raw = pool.ucp$getRawTemplates();

        int total = raw.stream().mapToInt(Pair::getSecond).sum();
        // chance = gymWeight / (total + gymWeight), shared equally between the gyms
        double gymWeight = total * chance / (1 - chance);
        int weight = Math.max(1, (int) Math.round(gymWeight / GYMS.size()));

        List<Pair<StructurePoolElement, Integer>> newRaw = new ArrayList<>(raw);
        for (String name : GYMS) {
            // A "single" element places the template's air too, clearing the hillside or trees inside the gym.
            // ("legacy" ones, like vanilla's village pieces, skip air and leave the terrain standing.)
            StructurePoolElement gym = StructurePoolElement.single(MOD_ID + ":village/" + style + "/" + name + "_gym", processors)
                    .apply(StructureTemplatePool.Projection.RIGID);
            GYM_ELEMENTS.add(gym);
            newRaw.add(Pair.of(gym, weight));
            for (int i = 0; i < weight; i++) pool.ucp$getTemplates().add(gym);
        }
        pool.ucp$setRawTemplates(newRaw);
    }
}
