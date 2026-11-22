package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.gym.RctBridge;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Radical Cobblemon Trainers' wild trainers: the ones that spawn on their own, as opposed to the league trainers CJP's
 * gyms summon. Each region's wild_trainers.json lists its own (see {@link RegionDef#wildTrainers}).
 *
 * <ul>
 *   <li>In region mode a listed trainer only spawns for players who have its region open, and none spawn before a
 *       player picks where their journey starts. Trainers no region lists (like Pokémon Unbound's) follow
 *       {@code blockUnlistedTrainers}.</li>
 *   <li>CJP keeps its own record of the wild trainers each player beats, for the Trainer Card's Trainers tab. It
 *       starts empty, whatever RCT remembers: RCT respawns ordinary trainers a player has beaten, so a trainer beaten
 *       before the record existed counts once it's beaten again.</li>
 * </ul>
 */
public final class WildTrainers {
    /** What the Trainer Card shows about a trainer: its name and the level of its strongest Pokémon. */
    public record Info(String name, int level) {}

    /** Read from RCT's trainer files, which only change on a datapack reload. */
    private static final Map<String, Optional<Info>> INFO = new ConcurrentHashMap<>();

    private WildTrainers() {}

    /** Called on datapack reload. */
    static void clearCache() {
        INFO.clear();
    }

    /** Whether RCT may spawn this trainer near the player on its own. League trainers are left to RCT's own data. */
    public static boolean maySpawnFor(ServerPlayer player, String trainerId) {
        if (!RegionRegistry.isActive() || RegionRegistry.isLeagueTrainer(trainerId)) return true;
        int region = RegionRegistry.wildTrainerRegion(trainerId);
        if (region < 0) return !ConfigLoader.BLOCK_UNLISTED_TRAINERS.get();
        if (RegionProgress.needsHomeRegion(player)) return false;
        return RegionProgressTracker.get(player).isUnlocked(region);
    }

    /** Records a win against an RCT trainer, unless it's a league trainer (the Road tab covers those). */
    public static void recordWin(ServerPlayer player, String trainerId) {
        if (trainerId == null || RegionRegistry.isLeagueTrainer(trainerId)) return;
        TrainerLevelComponents.KEY.get(player).addBeatenWildTrainer(trainerId);
    }

    /** The trainer's name and level from its RCT trainer file, or empty if RCT doesn't have it. */
    public static Optional<Info> info(MinecraftServer server, String trainerId) {
        return INFO.computeIfAbsent(trainerId, id -> read(server, id));
    }

    private static Optional<Info> read(MinecraftServer server, String trainerId) {
        ResourceLocation file = ResourceLocation.tryBuild(RctBridge.MOD_ID, "trainers/" + trainerId + ".json");
        if (file == null) return Optional.empty();
        Optional<Resource> resource = server.getResourceManager().getResource(file);
        if (resource.isEmpty()) return Optional.empty();
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement name = json.get("name");
            String title = name != null && name.isJsonPrimitive() ? name.getAsString() : RctBridge.trainerName(trainerId).getString();
            int level = 0;
            JsonElement team = json.get("team");
            if (team != null && team.isJsonArray()) {
                for (JsonElement pokemon : team.getAsJsonArray()) {
                    JsonElement l = pokemon.isJsonObject() ? pokemon.getAsJsonObject().get("level") : null;
                    if (l != null && l.isJsonPrimitive()) level = Math.max(level, l.getAsInt());
                }
            }
            return Optional.of(new Info(title, level));
        } catch (IOException | RuntimeException e) {
            UltimateCobblemonProgression.LOG.warn("Couldn't read RCT trainer {}: {}", trainerId, e.getMessage());
            return Optional.empty();
        }
    }
}
