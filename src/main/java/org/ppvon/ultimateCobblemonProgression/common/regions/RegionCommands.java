package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
                out.append(Component.literal("locked (complete " + regions.get(r - 1).title + " first)")
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

            TierDef next = region.tiers.get(step + 1);
            if (next != null) {
                out.append(CommonComponents.NEW_LINE)
                        .append(Component.literal("    Next: " + describeMissing(player, next)).withStyle(ChatFormatting.GRAY));
            }
        }

        Component prompt = startChoicePrompt(player);
        if (prompt != null) out.append(CommonComponents.NEW_LINE).append(prompt);
        return out;
    }

    /** /trainerlevel region choose &lt;region&gt; and /trainerlevel region reset &lt;player&gt;. */
    public static LiteralArgumentBuilder<CommandSourceStack> regionNode() {
        return Commands.literal("region")
                .then(Commands.literal("choose")
                        .then(Commands.argument("region", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        RegionRegistry.startChoices().stream().map(r -> r.id), builder))
                                .executes(ctx -> choose(ctx.getSource().getPlayerOrException(),
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
        List<RegionDef> choices = RegionRegistry.startChoices();
        if (choices.isEmpty() || RegionProgress.chosenStartRegion(player) != null) return null;
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
        if (region == null || !RegionRegistry.startChoices().contains(region)) {
            String ids = String.join(", ", RegionRegistry.startChoices().stream().map(r -> r.id).toList());
            return new Choice(false, Component.literal("'" + regionId + "' isn't a starting choice. Options: "
                    + (ids.isEmpty() ? "none" : ids)));
        }

        TrainerLevelComponents.KEY.get(player).setStartRegion(region.id);
        RegionProgressTracker.refresh(player);
        return new Choice(true, Component.literal(region.title + " is now open alongside "
                + RegionRegistry.ordered().getFirst().title + ".").withStyle(ChatFormatting.GREEN));
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

    /** What's still needed to unlock the tier, e.g. "Leader Brock, catch 10 species". */
    public static String describeMissing(ServerPlayer player, TierDef tier) {
        StringBuilder sb = new StringBuilder();
        for (ResourceLocation id : tier.requirements.advancements) {
            if (RequirementCheck.isAdvancementDone(player, id)) continue;
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(RequirementCheck.advancementTitle(player, id));
        }
        DexRequirements dex = tier.requirements.dex;
        if (dex.seen > 0) sb.append(sb.isEmpty() ? "" : ", ").append("see ").append(dex.seen).append(" species");
        if (dex.caught > 0) sb.append(sb.isEmpty() ? "" : ", ").append("catch ").append(dex.caught).append(" species");
        return sb.isEmpty() ? "no requirements" : sb.toString();
    }
}
