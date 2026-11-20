package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Talks to Radical Cobblemon Trainers through its own commands, so UCP needs no compile-time dependency
 * on it and keeps working when it isn't installed.
 */
public final class RctBridge {
    public static final String MOD_ID = "rctmod";

    private RctBridge() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** How many times the player has beaten the trainer, or -1 if RCT doesn't know that trainer id. */
    public static int defeats(ServerPlayer player, String trainerId) {
        // Run as the player: RCT's player argument rejects UUIDs, and without one it uses the command's caller.
        return run(source(player.serverLevel(), player.position()).withEntity(player),
                "rctmod player get defeats " + trainerId);
    }

    /** The RCT series the player is currently in ("" if none), or null if it couldn't be read. */
    public static String currentSeries(ServerPlayer player) {
        StringBuilder out = new StringBuilder();
        CommandSource capture = new CommandSource() {
            @Override public void sendSystemMessage(Component message) { out.append(message.getString()); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return false; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        CommandSourceStack source = new CommandSourceStack(capture, player.position(), player.getRotationVector(),
                player.serverLevel(), 4, "UCP", Component.literal("UCP"), player.server, player);
        try {
            player.server.getCommands().getDispatcher().execute("rctmod player get series", source);
            return out.toString().trim();
        } catch (CommandSyntaxException | RuntimeException e) {
            UltimateCobblemonProgression.LOG.warn("Couldn't read {}'s RCT series: {}", player.getGameProfile().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Puts the player into an RCT series, unless they're already in it (switching series again would reset their
     * RCT progress). Returns false if RCT refused.
     */
    public static boolean joinSeries(ServerPlayer player, String seriesId) {
        if (seriesId.equals(currentSeries(player))) return true;
        return run(source(player.serverLevel(), player.position()).withEntity(player),
                "rctmod player set series " + seriesId) >= 0;
    }

    /**
     * Records the trainer (and any trainers it requires that the player hasn't beaten) as beaten in the player's RCT
     * series progress, which RCT's level cap and trainer requirements follow. Returns false if RCT refused.
     */
    public static boolean addProgress(ServerPlayer player, String trainerId) {
        return run(source(player.serverLevel(), player.position()).withEntity(player),
                "rctmod player add progress after " + trainerId) >= 0;
    }

    /** The player's RCT level cap, or -1 if it couldn't be read. */
    public static int levelCap(ServerPlayer player) {
        return run(source(player.serverLevel(), player.position()).withEntity(player), "rctmod player get level_cap");
    }

    /** How many Pokémon the trainer's team has, read from its RCT trainer file, or -1 if that can't be read. */
    public static int teamSize(MinecraftServer server, String trainerId) {
        ResourceLocation file = ResourceLocation.tryBuild(MOD_ID, "trainers/" + trainerId + ".json");
        if (file == null) return -1;
        Optional<Resource> resource = server.getResourceManager().getResource(file);
        if (resource.isEmpty()) return -1;
        try (Reader reader = resource.get().openAsReader()) {
            JsonElement team = JsonParser.parseReader(reader).getAsJsonObject().get("team");
            return team != null && team.isJsonArray() ? team.getAsJsonArray().size() : -1;
        } catch (IOException | RuntimeException e) {
            UltimateCobblemonProgression.LOG.warn("Couldn't read RCT trainer {}: {}", trainerId, e.getMessage());
            return -1;
        }
    }

    /** RCT's translated trainer name, falling back to one made from the id. */
    public static Component trainerName(String trainerId) {
        return Component.translatableWithFallback("trainer.rctmod." + trainerId + ".name", plainName(trainerId));
    }

    /** "leader_lt_surge_01a0" becomes "Leader Lt Surge". */
    static String plainName(String trainerId) {
        String base = trainerId.replaceFirst("_[0-9a-f]{4}$", "");
        StringBuilder sb = new StringBuilder();
        for (String word : base.split("_")) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /**
     * Summons a trainer standing on {@code pos} and returns it, or null if RCT refused.
     * The trainer is tagged so it can be found again after the command, plus any extra tags given.
     */
    public static Entity summon(ServerLevel level, BlockPos pos, String trainerId, String... extraTags) {
        String tag = "ucp_gym_" + UUID.randomUUID().toString().substring(0, 8);
        StringBuilder tags = new StringBuilder("\"" + tag + "\",\"" + GymChallengeBlockEntity.LEADER_TAG + "\"");
        for (String extra : extraTags) tags.append(",\"").append(extra).append('"');
        int result = run(source(level, Vec3.atCenterOf(pos)),
                "rctmod trainer summon " + trainerId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                        + " {Tags:[" + tags + "]}");
        if (result < 0) return null;

        List<Entity> found = level.getEntities((Entity) null, new AABB(pos).inflate(2), e -> e.getTags().contains(tag));
        if (found.isEmpty()) return null;
        Entity trainer = found.get(0);
        trainer.removeTag(tag);
        return trainer;
    }

    private static CommandSourceStack source(ServerLevel level, Vec3 pos) {
        return level.getServer().createCommandSourceStack()
                .withLevel(level)
                .withPosition(pos)
                .withPermission(4)
                .withSuppressedOutput();
    }

    private static int run(CommandSourceStack source, String command) {
        try {
            return source.getServer().getCommands().getDispatcher().execute(command, source);
        } catch (CommandSyntaxException e) {
            UltimateCobblemonProgression.LOG.warn("RCT command failed ({}): {}", command, e.getMessage());
            return -1;
        } catch (RuntimeException e) {
            UltimateCobblemonProgression.LOG.error("RCT command crashed ({})", command, e);
            return -1;
        }
    }
}
