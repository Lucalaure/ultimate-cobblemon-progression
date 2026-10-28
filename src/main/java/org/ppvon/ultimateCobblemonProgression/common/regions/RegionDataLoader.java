package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Loads data/ultimate-cobblemon-progression/regions/&lt;region&gt;/region.json and tier_N.json files.
 */
public final class RegionDataLoader extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public RegionDataLoader() { super(GSON, "regions"); }

    public static void register() {
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new RegionDataLoader());
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        Map<String, RegionDef.Meta> metas = new HashMap<>();
        Map<String, SortedMap<Integer, TierDef>> tiers = new HashMap<>();

        for (var entry : files.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (!id.getNamespace().equals(MOD_ID)) continue;

            String[] parts = id.getPath().split("/");
            if (parts.length != 2) {
                UltimateCobblemonProgression.LOG.warn("Ignoring region file {} (expected regions/<region>/<file>.json)", id);
                continue;
            }
            String regionId = parts[0];
            String name = parts[1];

            try {
                if (name.equals("region")) {
                    RegionDef.Meta meta = RegionDef.Meta.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                            .getOrThrow(msg -> new IllegalArgumentException("Invalid region.json: " + msg));
                    metas.put(regionId, meta);
                    continue;
                }

                var idx = TierRegistry.tryParseTierIndex(name);
                if (idx.isEmpty()) {
                    UltimateCobblemonProgression.LOG.warn("Ignoring {} (not region.json or tier_N.json)", id);
                    continue;
                }
                TierDef parsed = TierDef.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(msg -> new IllegalArgumentException("Invalid tier: " + msg));
                int index = idx.getAsInt();
                tiers.computeIfAbsent(regionId, k -> new TreeMap<>())
                        .put(index, new TierDef(index, parsed.levelCap, parsed.species, parsed.requirements));
            } catch (Exception ex) {
                UltimateCobblemonProgression.LOG.error("Failed to load region file {}: {}", id, ex.getMessage());
            }
        }

        List<RegionDef> regions = new ArrayList<>();
        for (var e : tiers.entrySet()) {
            String regionId = e.getKey();
            RegionDef.Meta meta = metas.get(regionId);
            if (meta == null) {
                UltimateCobblemonProgression.LOG.error("Region '{}' has tier files but no region.json; skipping it", regionId);
                continue;
            }
            SortedMap<Integer, TierDef> regionTiers = e.getValue();
            if (regionTiers.firstKey() != 1 || regionTiers.lastKey() != regionTiers.size()) {
                UltimateCobblemonProgression.LOG.error("Region '{}' tiers must be numbered 1..N with no gaps (found {}); skipping it",
                        regionId, regionTiers.keySet());
                continue;
            }
            regions.add(new RegionDef(regionId, meta, regionTiers));
        }
        for (String regionId : metas.keySet()) {
            if (!tiers.containsKey(regionId)) {
                UltimateCobblemonProgression.LOG.error("Region '{}' has a region.json but no tier files; skipping it", regionId);
            }
        }

        RegionRegistry.set(regions);
        UltimateCobblemonProgression.LOG.info("Loaded regions: {}", RegionRegistry.ordered().stream()
                .map(r -> r.id + " (" + r.maxTier() + " tiers)").toList());
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, "region_data_loader");
    }
}
