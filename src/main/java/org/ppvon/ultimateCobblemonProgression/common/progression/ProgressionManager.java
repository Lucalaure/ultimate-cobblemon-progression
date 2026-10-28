package org.ppvon.ultimateCobblemonProgression.common.progression;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.progression.dex.DexProgressionApi;
import org.ppvon.ultimateCobblemonProgression.common.progression.dex.DexProgressionListener;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.DexRequirements;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Optional;

public final class ProgressionManager {
    private ProgressionManager() {}

    public static void register() {
        DexProgressionListener.register();
    }

    public static Component buildChatMessage(ServerPlayer player, int newTier) {
        Optional<TierDef> tierDefOpt = TierRegistry.get(newTier);
        if (tierDefOpt.isEmpty()) {
            return Component.literal("Trainer Level: " + newTier)
                    .withStyle(ChatFormatting.GREEN);
        }

        TierDef tierDef = tierDefOpt.get();
        int newLevelCap = tierDef.levelCap;

        int newlyUnlockedSpecies = TierRegistry.getRealSpecies(newTier);

        Optional<TierDef> nextTierOpt = TierRegistry.get(newTier + 1);
        Optional<DexRequirements> nextTierDexReq =
                nextTierOpt.map(t -> t.requirements.dex);

        boolean max = nextTierOpt.isEmpty();

        MutableComponent header = Component.literal("Congrats! Your trainer level is now ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(newTier)).withStyle(ChatFormatting.BOLD));

        Component levelCapLine = Component.literal("  • New level cap: ")
                .append(Component.literal(String.valueOf(newLevelCap)).withStyle(ChatFormatting.AQUA));

        Component unlockedLine = Component.literal("  • ")
                .append(Component.literal(String.valueOf(newlyUnlockedSpecies)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" " + pluralize(newlyUnlockedSpecies, "new species", "new species") + " unlocked"));

        if (max || nextTierDexReq.isEmpty() || nextTierDexReq.get() == null || !ConfigLoader.DO_DEX_PROGRESSION.get()) {
            return header
                    .append(CommonComponents.NEW_LINE).append(levelCapLine)
                    .append(CommonComponents.NEW_LINE).append(unlockedLine);
        }

        DexRequirements dexRequirements = nextTierDexReq.get();

        boolean hasSeen = dexRequirements.seen > 0;
        boolean hasCaught = dexRequirements.caught > 0;

        Component reqHeader = Component.literal("  Requirements for next level:")
                .withStyle(ChatFormatting.YELLOW);

        Component reqBody;
        if (!hasSeen && !hasCaught) {
            reqBody = Component.literal("    • None (progress via other goals)")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            MutableComponent seenLine = hasSeen
                    ? Component.literal("    • Seen ").append(number(dexRequirements.seen))
                    : Component.empty();
            MutableComponent caughtLine = hasCaught
                    ? Component.literal("    • Caught ").append(number(dexRequirements.caught))
                    : Component.empty();

            if (hasSeen && hasCaught) {
                reqBody = seenLine.append(CommonComponents.NEW_LINE).append(caughtLine);
            } else {
                reqBody = hasSeen ? seenLine : caughtLine;
            }
        }

        return header
                .append(CommonComponents.NEW_LINE).append(levelCapLine)
                .append(CommonComponents.NEW_LINE).append(unlockedLine)
                .append(CommonComponents.NEW_LINE).append(reqHeader)
                .append(CommonComponents.NEW_LINE).append(reqBody);
    }

    public static Component buildChatMessage(int newTier, int newLevelCap, int newlyUnlockedSpecies,
                                             Optional<DexRequirements> nextTierDexReq, Boolean max) {

        MutableComponent header = Component.literal("Congrats! Your trainer level is now ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(String.valueOf(newTier)).withStyle(ChatFormatting.BOLD));


        Component levelCapLine = Component.literal("  • New level cap: ")
                .append(Component.literal(String.valueOf(newLevelCap)).withStyle(ChatFormatting.AQUA));

        Component unlockedLine = Component.literal("  • ")
                .append(Component.literal(String.valueOf(newlyUnlockedSpecies)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" " + pluralize(newlyUnlockedSpecies, "new species", "new species") + " unlocked"));

        if(max || nextTierDexReq.isEmpty()) {
            return header
                    .append(CommonComponents.NEW_LINE).append(levelCapLine)
                    .append(CommonComponents.NEW_LINE).append(unlockedLine);
        }

        DexRequirements dexRequirements = nextTierDexReq.get();

        boolean hasSeen = dexRequirements != null && dexRequirements.seen > 0;
        boolean hasCaught = dexRequirements != null && dexRequirements.caught > 0;

        Component reqHeader = Component.literal("  Requirements for next level:")
                .withStyle(ChatFormatting.YELLOW);

        Component reqBody;
        if (!hasSeen && !hasCaught) {
            reqBody = Component.literal("    • None (progress via other goals)")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            MutableComponent seenLine = hasSeen
                    ? Component.literal("    • Seen ").append(number(dexRequirements.seen))
                    : Component.empty();
            MutableComponent caughtLine = hasCaught
                    ? Component.literal("    • Caught ").append(number(dexRequirements.caught))
                    : Component.empty();

            if (hasSeen && hasCaught) {
                reqBody = seenLine.append(CommonComponents.NEW_LINE).append(caughtLine);
            } else {
                reqBody = hasSeen ? seenLine : caughtLine;
            }
        }

        return header
                .append(CommonComponents.NEW_LINE).append(levelCapLine)
                .append(CommonComponents.NEW_LINE).append(unlockedLine)
                .append(CommonComponents.NEW_LINE).append(reqHeader)
                .append(CommonComponents.NEW_LINE).append(reqBody);
    }

    private static Component number(int n) {
        return Component.literal(String.valueOf(n)).withStyle(ChatFormatting.AQUA);
    }

    private static String pluralize(int n, String singular, String plural) {
        return n == 1 ? singular : plural;
    }


    /** Called whenever one of the player's advancements is awarded or revoked. */
    public static void onAdvancementsChanged(ServerPlayer player) {
        if (RegionRegistry.isActive()) {
            RegionProgressTracker.refresh(player);
            return;
        }
        if (!ConfigLoader.DO_DEX_PROGRESSION.get()) return;
        DexProgressionApi.DexCounts counts = DexProgressionApi.get(player);
        attemptLevelUp(player, counts.seen(), counts.caught());
    }

    public static void attemptLevelUp(ServerPlayer player, int seen, int caught) {
        var comp = TrainerLevelComponents.KEY.get(player);
        int current = comp.getLevel();
        int next = current + 1;

        var nextReqsOpt = TierRegistry.getRequirements(next);
        if (nextReqsOpt == null || !nextReqsOpt.hasAny()) return;

        DexRequirements req = nextReqsOpt.dex;
        boolean seenOK = req.seen <= 0 || seen >= req.seen;
        boolean caughtOK = req.caught <= 0 || caught >= req.caught;

        if (seenOK && caughtOK && RequirementCheck.areAdvancementsDone(player, nextReqsOpt)) {
            comp.setLevel(next);
            int newLevel = comp.getLevel();
            Optional<TierDef> newTier = TierRegistry.get(next);
            if(newTier.isEmpty() || current == newLevel){
                return;
            }

            Optional<TierDef> nextTier = TierRegistry.get(next+1);
            if(nextTier.isEmpty()) {
                player.displayClientMessage(
                    buildChatMessage(next, newTier.get().levelCap, newTier.get().species.size(), Optional.empty(), true),
                    false
                );
                return;
            }

            player.displayClientMessage(
                buildChatMessage(next, newTier.get().levelCap, TierRegistry.getRealSpecies(newLevel), Optional.ofNullable(nextTier.get().requirements.dex), false),
                false
            );
        }
    }

}
