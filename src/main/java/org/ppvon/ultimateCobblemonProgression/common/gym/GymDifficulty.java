package org.ppvon.ultimateCobblemonProgression.common.gym;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionCommands;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Gym leader difficulty (region mode, with Radical Cobblemon Trainers). Each league trainer can have a Hard version:
 * an RCT trainer with the same id plus {@value #HARD_SUFFIX}, listed next to it in the region's tier file
 * (e.g. {@code ["leader_brock_019e", "leader_brock_019e_hard"]}). Gyms summon the version matching the player's
 * difficulty, and beating either earns the badge.
 *
 * <p>Hard trainers are marked optional in RCT, so RCT's series and level cap only follow the Normal trainers. Beating
 * a Hard trainer also records its Normal version as beaten in RCT, so the level cap rises and the next leaders'
 * {@code requiredDefeats} are met, whichever version a player fought.
 *
 * <p>Players pick their difficulty on the Trainer Card (or with {@code /trainerlevel difficulty}) and can change it
 * at any time; it applies from the next leader a gym summons. The {@code gymDifficulty} config can set it for everyone.
 */
public final class GymDifficulty {
    public static final String HARD_SUFFIX = "_hard";
    private static final String PLAYER_CHOICE = "player_choice";

    public enum Level {
        NORMAL("normal", "Normal"),
        HARD("hard", "Hard");

        public final String id;
        public final String title;

        Level(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Level byId(String id) {
            if (id == null) return null;
            String lower = id.toLowerCase(Locale.ROOT);
            for (Level l : values()) if (l.id.equals(lower)) return l;
            return null;
        }
    }

    private GymDifficulty() {}

    /** Whether each player picks their own difficulty (otherwise the server config sets it for everyone). */
    public static boolean playerChooses() {
        return forced() == null;
    }

    /** The difficulty the server config sets for everyone, or null when players choose. */
    private static Level forced() {
        String mode = ConfigLoader.GYM_DIFFICULTY.get();
        if (PLAYER_CHOICE.equalsIgnoreCase(mode)) return null;
        Level level = Level.byId(mode);
        return level; // an unknown value counts as player_choice
    }

    /** The difficulty the player's next gym leader is summoned at. Normal until they pick. */
    public static Level of(ServerPlayer player) {
        Level forced = forced();
        if (forced != null) return forced;
        Level picked = Level.byId(TrainerLevelComponents.KEY.get(player).getGymDifficulty().orElse(null));
        return picked != null ? picked : Level.NORMAL;
    }

    /** Whether any loaded league trainer has a Hard version, i.e. whether there's a difficulty to pick at all. */
    /** Whether any of the region's trainers has a Hard version. */
    public static boolean availableIn(RegionDef region) {
        if (region == null || !RctBridge.isLoaded()) return false;
        for (TierDef tier : region.tiers.values()) {
            for (List<String> group : tier.trainers) {
                if (hardVersion(group) != null) return true;
            }
        }
        return false;
    }

    public static boolean available() {
        if (!RegionRegistry.isActive() || !RctBridge.isLoaded()) return false;
        for (RegionDef region : RegionRegistry.ordered()) {
            for (TierDef tier : region.tiers.values()) {
                for (List<String> group : tier.trainers) {
                    if (hardVersion(group) != null) return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether the Trainer Card should ask the player to pick a difficulty: players choose, there's a Hard version to
     * choose, they've picked where their journey starts, and they haven't picked a difficulty yet.
     */
    public static boolean needsChoice(ServerPlayer player) {
        return playerChooses() && available() && !RegionProgress.needsHomeRegion(player)
                && TrainerLevelComponents.KEY.get(player).getGymDifficulty().isEmpty();
    }

    /** The trainer a gym summons for this slot of a tier: the Hard version on Hard, if the slot has one. */
    public static String trainerFor(ServerPlayer player, List<String> group) {
        if (of(player) == Level.HARD) {
            String hard = hardVersion(group);
            if (hard != null) return hard;
        }
        return group.get(0);
    }

    /** The Hard version listed in this slot (the first trainer's id plus "_hard"), or null. */
    public static String hardVersion(List<String> group) {
        String hard = group.get(0) + HARD_SUFFIX;
        return group.contains(hard) ? hard : null;
    }

    public static boolean isHard(String trainerId) {
        return trainerId != null && trainerId.endsWith(HARD_SUFFIX);
    }

    /**
     * Called for every win against an RCT trainer. After a Hard league trainer, RCT also records its Normal version as
     * beaten, which is what RCT's level cap and the next trainers' requirements look at.
     */
    public static void onTrainerDefeated(ServerPlayer player, String trainerId) {
        if (!isHard(trainerId) || !RctBridge.isLoaded() || !RegionRegistry.isLeagueTrainer(trainerId)) return;
        String normal = trainerId.substring(0, trainerId.length() - HARD_SUFFIX.length());
        if (!RegionRegistry.isLeagueTrainer(normal)) return;
        // Only in the player's RCT series: with two leagues, the other league's progress is CJP's alone.
        RegionDef region = GymRematch.regionOf(normal);
        String series = RctBridge.currentSeries(player);
        if (region != null && region.series.isPresent() && series != null && !region.series.get().equals(series)) return;
        RctBridge.addProgress(player, normal);
    }

    /** Sets the player's difficulty, from the Trainer Card or the command. */
    public static RegionCommands.Choice choose(ServerPlayer player, String difficultyId) {
        Level level = Level.byId(difficultyId);
        if (level == null) {
            return new RegionCommands.Choice(false, Component.literal("'" + difficultyId + "' isn't a difficulty. Options: "
                    + String.join(", ", Arrays.stream(Level.values()).map(l -> l.id).toList())));
        }
        Level forced = forced();
        if (forced != null) {
            return new RegionCommands.Choice(false, Component.literal("This server sets gym leaders to " + forced.title + " for everyone."));
        }
        if (!available()) {
            return new RegionCommands.Choice(false, Component.literal("No gym leaders have a Hard version here, so there's no difficulty to pick."));
        }
        boolean first = TrainerLevelComponents.KEY.get(player).getGymDifficulty().isEmpty();
        Level before = of(player);
        TrainerLevelComponents.KEY.get(player).setGymDifficulty(level.id);
        String msg = first || before != level
                ? "Gym leaders are now on " + level.title + ". It applies from the next leader a gym summons; you can change it any time."
                : "Gym leaders are already on " + level.title + ".";
        return new RegionCommands.Choice(true, Component.literal(msg).withStyle(ChatFormatting.GREEN));
    }

    /** /trainerlevel difficulty [normal|hard]. */
    public static LiteralArgumentBuilder<CommandSourceStack> commandNode() {
        return Commands.literal("difficulty")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Level level = of(player);
                    String how = playerChooses() ? " Change it with /trainerlevel difficulty <normal|hard>, or on your Trainer Card's Road tab."
                            : " The server sets it for everyone.";
                    ctx.getSource().sendSuccess(() -> Component.literal("Gym leaders: " + level.title + "." + how), false);
                    return 1;
                })
                .then(Commands.argument("difficulty", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                Arrays.stream(Level.values()).map(l -> l.id), builder))
                        .executes(ctx -> {
                            RegionCommands.Choice result = choose(ctx.getSource().getPlayerOrException(),
                                    StringArgumentType.getString(ctx, "difficulty"));
                            if (!result.ok()) {
                                ctx.getSource().sendFailure(result.message());
                                return 0;
                            }
                            ctx.getSource().sendSuccess(result::message, false);
                            return 1;
                        }));
    }
}
