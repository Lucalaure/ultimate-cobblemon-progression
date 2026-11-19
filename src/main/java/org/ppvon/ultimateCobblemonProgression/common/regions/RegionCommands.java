package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.progression.RequirementCheck;
import org.ppvon.ultimateCobblemonProgression.common.gym.RctBridge;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.DexRequirements;

import java.util.List;

/**
 * /trainerlevel output in region mode.
 */
public final class RegionCommands {
    private RegionCommands() {}

    public static Component describe(ServerPlayer player) {
        RegionProgress progress = RegionProgressTracker.get(player);
        List<RegionDef> regions = RegionRegistry.ordered();
        MutableComponent out = Component.literal("Region progress:").withStyle(ChatFormatting.YELLOW);

        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);
            out.append(CommonComponents.NEW_LINE)
                    .append(Component.literal("• " + region.title + ": ").withStyle(ChatFormatting.AQUA));

            if (!progress.isUnlocked(r)) {
                out.append(Component.literal("locked (" + lockedReason(player, progress, r) + ")")
                        .withStyle(ChatFormatting.GRAY));
                continue;
            }

            int step = progress.step(r);
            out.append(Component.literal("tier " + step + " / " + region.maxTier()));
            if (progress.isCompleted(r)) {
                out.append(Component.literal(" (complete)").withStyle(ChatFormatting.GOLD));
            }

            if (region.id.equals(RegionProgress.chosenStartRegion(player))) {
                out.append(Component.literal(" (your starting pick)").withStyle(ChatFormatting.GRAY));
            }
            if (r == progress.current()) {
                out.append(Component.literal(" (current region)").withStyle(ChatFormatting.GRAY));
            }

            TierDef next = region.tiers.get(step + 1);
            if (next != null) {
                out.append(CommonComponents.NEW_LINE)
                        .append(Component.literal("    Next: " + describeMissing(player, region, next)).withStyle(ChatFormatting.GRAY));
            }
        }

        Component prompt = startChoicePrompt(player);
        if (prompt != null) out.append(CommonComponents.NEW_LINE).append(prompt);
        return out;
    }

    /** Why a locked region is locked, e.g. "complete Kanto first". */
    private static String lockedReason(ServerPlayer player, RegionProgress progress, int r) {
        List<RegionDef> regions = RegionRegistry.ordered();
        RegionDef region = regions.get(r);
        List<Integer> journey = progress.journey();
        if (journey.isEmpty()) {
            return r > 0 ? "complete " + regions.get(r - 1).title + " first" : "pick your starting region first";
        }
        if (region.isLeague()) {
            int current = progress.current();
            return current >= 0
                    ? "travel here after completing " + regions.get(current).title
                    : "pick it as your next region on your Trainer Card";
        }
        // Regions without a series only open as the extra starting region.
        return RegionProgress.openableStartChoices(player).contains(region)
                ? "you can pick it as your extra starting region"
                : "not on your journey";
    }

    /** /trainerlevel region choose|next &lt;region&gt; and /trainerlevel region reset &lt;player&gt;. */
    public static LiteralArgumentBuilder<CommandSourceStack> regionNode() {
        return Commands.literal("region")
                .then(Commands.literal("choose")
                        .then(Commands.argument("region", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getPlayer() != null
                                                ? RegionProgress.openableStartChoices(ctx.getSource().getPlayer()).stream().map(r -> r.id)
                                                : RegionRegistry.startChoices().stream().map(r -> r.id), builder))
                                .executes(ctx -> choose(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "region"), ctx.getSource()))))
                .then(Commands.literal("next")
                        .then(Commands.argument("region", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getPlayer() != null
                                                ? RegionProgress.nextChoices(ctx.getSource().getPlayer()).stream().map(r -> r.id)
                                                : RegionRegistry.homeChoices().stream().map(r -> r.id), builder))
                                .executes(ctx -> next(ctx.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(ctx, "region"), ctx.getSource()))))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> reset(EntityArgument.getPlayer(ctx, "player"), ctx.getSource()))));
    }

    /** Tells a player who hasn't picked an extra starting region yet how to pick one. */
    public static void promptStartChoice(ServerPlayer player) {
        Component prompt = startChoicePrompt(player);
        if (prompt != null) player.displayClientMessage(prompt, false);
    }

    private static Component startChoicePrompt(ServerPlayer player) {
        if (RegionProgress.needsHomeRegion(player)) {
            return Component.literal("Open your Trainer Card to choose the region your journey starts in.")
                    .withStyle(ChatFormatting.YELLOW);
        }
        List<RegionDef> next = RegionProgress.nextChoices(player);
        if (!next.isEmpty()) {
            String ids = String.join(", ", next.stream().map(r -> r.id).toList());
            return Component.literal("Choose where your journey goes next: open your Trainer Card, or use /trainerlevel region next <"
                    + ids + ">").withStyle(ChatFormatting.YELLOW);
        }
        List<RegionDef> choices = RegionProgress.openableStartChoices(player);
        if (choices.isEmpty()) return null;
        String ids = String.join(", ", choices.stream().map(r -> r.id).toList());
        return Component.literal("Pick a second region to explore from the start: open your Trainer Card's Regions tab, or use /trainerlevel region choose <" + ids + ">")
                .withStyle(ChatFormatting.YELLOW);
    }

    private static int choose(ServerPlayer player, String regionId, CommandSourceStack src) {
        Choice result = chooseStartRegion(player, regionId);
        if (!result.ok()) {
            src.sendFailure(result.message());
            return 0;
        }
        src.sendSuccess(result::message, false);
        return 1;
    }

    private static int next(ServerPlayer player, String regionId, CommandSourceStack src) {
        Choice result = chooseNextRegion(player, regionId);
        if (!result.ok()) {
            src.sendFailure(result.message());
            return 0;
        }
        src.sendSuccess(result::message, false);
        return 1;
    }

    /** The outcome of picking a starting region: whether it worked, and what to tell the player. */
    public record Choice(boolean ok, Component message) {}

    /** Picks the region the player's journey starts in (from the Trainer Card), and joins its trainer series. */
    public static Choice chooseHomeRegion(ServerPlayer player, String regionId) {
        if (!RegionRegistry.isActive()) {
            return new Choice(false, Component.literal("No regions are loaded, so there's nothing to choose."));
        }
        String current = RegionProgress.chosenHomeRegion(player);
        if (current != null) {
            return new Choice(false, Component.literal("Your journey already started in " + RegionRegistry.byId(current).title + "."));
        }
        RegionDef region = RegionRegistry.byId(regionId);
        if (region == null || region.series.isEmpty()) {
            return new Choice(false, Component.literal("'" + regionId + "' can't be a starting region."));
        }
        if (RctBridge.isLoaded() && !RctBridge.joinSeries(player, region.series.get())) {
            return new Choice(false, Component.literal("Couldn't start the " + region.title
                    + " trainer series in Radical Cobblemon Trainers. Check the server log."));
        }
        TrainerLevelComponents.KEY.get(player).setHomeRegion(region.id);
        RegionProgressTracker.refresh(player);
        return new Choice(true, Component.literal("Your journey begins in " + region.title + "! Find a gym in a village to challenge its first leader.")
                .withStyle(ChatFormatting.GREEN));
    }

    /**
     * Picks the region the player's journey continues in, once every region on it so far is complete, and joins
     * its trainer series. Used by the command and the Trainer Card.
     */
    public static Choice chooseNextRegion(ServerPlayer player, String regionId) {
        if (!RegionRegistry.isActive()) {
            return new Choice(false, Component.literal("No regions are loaded, so there's nothing to choose."));
        }
        if (RegionProgress.chosenHomeRegion(player) == null) {
            return new Choice(false, Component.literal("Choose where your journey starts first: open your Trainer Card."));
        }
        RegionProgress progress = RegionProgressTracker.get(player);
        int current = progress.current();
        if (current >= 0) {
            return new Choice(false, Component.literal("Complete " + RegionRegistry.get(current).title
                    + " before travelling to a new region."));
        }
        RegionDef region = RegionRegistry.byId(regionId);
        List<RegionDef> choices = RegionProgress.nextChoices(player);
        if (region == null || !choices.contains(region)) {
            String ids = String.join(", ", choices.stream().map(r -> r.id).toList());
            return new Choice(false, Component.literal("'" + regionId + "' isn't a region you can travel to. Options: "
                    + (ids.isEmpty() ? "none" : ids)));
        }
        if (RctBridge.isLoaded() && !RctBridge.joinSeries(player, region.series.get())) {
            return new Choice(false, Component.literal("Couldn't start the " + region.title
                    + " trainer series in Radical Cobblemon Trainers. Check the server log."));
        }
        TrainerLevelComponents.KEY.get(player).addNextRegion(region.id);
        RegionProgressTracker.refresh(player);
        return new Choice(true, Component.literal("Your journey continues in " + region.title
                + "! Find a gym in a village to challenge its first leader.").withStyle(ChatFormatting.GREEN));
    }

    /** Picks the player's extra starting region, used by the command and the Trainer Card. */
    public static Choice chooseStartRegion(ServerPlayer player, String regionId) {
        if (!RegionRegistry.isActive()) {
            return new Choice(false, Component.literal("No regions are loaded, so there's nothing to choose."));
        }
        String current = RegionProgress.chosenStartRegion(player);
        if (current != null) {
            return new Choice(false, Component.literal("You already picked " + RegionRegistry.byId(current).title
                    + ". An operator can undo it with /trainerlevel region reset <player>."));
        }
        RegionDef region = RegionRegistry.byId(regionId);
        if (region != null && region.id.equals(RegionProgress.chosenHomeRegion(player))) {
            return new Choice(false, Component.literal(region.title + " is already where your journey started."));
        }
        if (region != null && RegionRegistry.startChoices().contains(region)
                && !RegionProgress.openableStartChoices(player).contains(region)) {
            return new Choice(false, Component.literal(region.title + " is already open."));
        }
        if (region == null || !RegionRegistry.startChoices().contains(region)) {
            String ids = String.join(", ", RegionProgress.openableStartChoices(player).stream().map(r -> r.id).toList());
            return new Choice(false, Component.literal("'" + regionId + "' isn't a starting choice. Options: "
                    + (ids.isEmpty() ? "none" : ids)));
        }

        TrainerLevelComponents.KEY.get(player).setStartRegion(region.id);
        RegionProgressTracker.refresh(player);
        String home = RegionProgress.chosenHomeRegion(player);
        String alongside = home != null ? RegionRegistry.byId(home).title : RegionRegistry.ordered().getFirst().title;
        return new Choice(true, Component.literal(region.title + " is now open alongside " + alongside + ".")
                .withStyle(ChatFormatting.GREEN));
    }

    private static int reset(ServerPlayer target, CommandSourceStack src) {
        if (!RegionRegistry.isActive()) return unavailableOutsideRegionMode(src);
        TrainerLevelComponents.KEY.get(target).setStartRegion(null);
        RegionProgressTracker.refreshSilently(target);
        src.sendSuccess(() -> Component.literal("Cleared " + target.getGameProfile().getName() + "'s starting region choice."), true);
        promptStartChoice(target);
        return 1;
    }

    private static int unavailableOutsideRegionMode(CommandSourceStack src) {
        src.sendFailure(Component.literal("No regions are loaded, so there's nothing to choose."));
        return 0;
    }

    public static int unavailable(CommandSourceStack src) {
        src.sendFailure(Component.literal(
                "Trainer levels aren't used in region mode. Progress comes from advancements, e.g. /advancement grant."));
        return 0;
    }

    /**
     * What's still needed to unlock the region's tier, including (with catchWholeTier) the Pokémon of the tier before
     * it still to catch, e.g. "catch Pidgey and Rattata, Boulder Badge".
     */
    public static String describeMissing(ServerPlayer player, RegionDef region, TierDef tier) {
        List<Species> toCatch = TierCatches.missing(player, region, tier.index - 1);
        String rest = describeMissing(player, tier);
        if (toCatch.isEmpty()) return rest;
        String catches = "catch " + TierCatches.names(toCatch, 5);
        return rest.equals("no requirements") ? catches : catches + ", " + rest;
    }

    /** What's still needed to unlock the tier, e.g. "Leader Brock, catch 10 species". */
    public static String describeMissing(ServerPlayer player, TierDef tier) {
        StringBuilder sb = new StringBuilder();
        for (List<ResourceLocation> group : tier.requirements.advancements) {
            if (RequirementCheck.isAnyAdvancementDone(player, group)) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(String.join(" or ", group.stream().map(id -> RequirementCheck.advancementTitle(player, id)).toList()));
        }
        DexRequirements dex = tier.requirements.dex;
        if (dex.seen > 0) sb.append(sb.isEmpty() ? "" : ", ").append("see ").append(dex.seen).append(" species");
        if (dex.caught > 0) sb.append(sb.isEmpty() ? "" : ", ").append("catch ").append(dex.caught).append(" species");
        return sb.isEmpty() ? "no requirements" : sb.toString();
    }
}
