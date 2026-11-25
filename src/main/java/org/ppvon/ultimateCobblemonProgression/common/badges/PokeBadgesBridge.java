package org.ppvon.ultimateCobblemonProgression.common.badges;

import net.levelscraft7.pokebadges.api.BadgeActionContext;
import net.levelscraft7.pokebadges.api.BadgeActionSource;
import net.levelscraft7.pokebadges.api.BadgeOperationResult;
import net.levelscraft7.pokebadges.api.PokeBadgesApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The only class that uses PokeBadges' API. Call it only when PokeBadges is loaded ({@link PokeBadgesCompat}). */
final class PokeBadgesBridge {
    private static final Logger LOG = LoggerFactory.getLogger("UCP/PokeBadges");
    private static final String ORIGIN = "Cobblemon Journey Progression";

    private PokeBadgesBridge() {}

    static void award(ServerPlayer player, ResourceLocation badge) {
        BadgeOperationResult result = PokeBadgesApi.awardBadge(player, badge,
                BadgeActionContext.of(BadgeActionSource.API, ORIGIN));
        if (result != BadgeOperationResult.SUCCESS && result != BadgeOperationResult.ALREADY_OWNED) {
            LOG.warn("Couldn't award {} to {}: {}", badge, player.getGameProfile().getName(), result);
        }
    }

    static void awardIfNeverOwned(ServerPlayer player, ResourceLocation badge) {
        if (PokeBadgesApi.getOwnedBadges(player).contains(badge) || PokeBadgesApi.getBackupBadges(player).contains(badge)) {
            return;
        }
        award(player, badge);
    }
}
