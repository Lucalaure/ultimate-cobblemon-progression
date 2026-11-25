package org.ppvon.ultimateCobblemonProgression.common.badges;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Optional PokeBadges integration. A badge advancement names its PokeBadges item in an extra
 * "ultimate-cobblemon-progression:badge" field (written by scripts/generate-league-advancements.py). With PokeBadges
 * installed, that item replaces the advancement's icon, and earning the advancement awards the badge (it shows up in
 * the player's Badge Box). Without PokeBadges, the advancements keep their own icons and nothing else changes.
 *
 * <p>Only {@link PokeBadgesBridge} touches PokeBadges classes, and it's only called when the mod is loaded.
 */
public final class PokeBadgesCompat {
    public static final String MOD_ID = "pokebadges";
    private static final String BADGE_KEY = UltimateCobblemonProgression.MOD_ID + ":badge";
    private static final Logger LOG = LoggerFactory.getLogger("UCP/PokeBadges");

    /** Advancement id -> PokeBadges badge id, from the last advancement reload. */
    private static volatile Map<ResourceLocation, ResourceLocation> badges = Map.of();

    private PokeBadgesCompat() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /**
     * Called with the raw advancement JSON before it's parsed: records each advancement's badge and, if that badge
     * item exists, makes it the icon. Swapping the JSON (rather than shipping a second datapack) keeps one file per
     * advancement and works whatever the datapack order.
     */
    public static void prepare(Map<ResourceLocation, JsonElement> advancements) {
        boolean loaded = isLoaded();
        Map<ResourceLocation, ResourceLocation> found = new HashMap<>();
        advancements.forEach((id, json) -> {
            if (!json.isJsonObject()) return;
            JsonObject adv = json.getAsJsonObject();
            if (!adv.has(BADGE_KEY)) return;
            ResourceLocation badge = ResourceLocation.tryParse(adv.get(BADGE_KEY).getAsString());
            if (badge == null) {
                LOG.warn("Advancement {} has an invalid badge id: {}", id, adv.get(BADGE_KEY));
                return;
            }
            found.put(id, badge);
            if (!loaded) return;
            if (!BuiltInRegistries.ITEM.containsKey(badge)) {
                LOG.warn("Advancement {} names badge {}, which PokeBadges doesn't have", id, badge);
                return;
            }
            if (adv.get("display") instanceof JsonObject display && display.get("icon") instanceof JsonObject icon) {
                icon.addProperty("id", badge.toString());
            }
        });
        badges = Map.copyOf(found);
        if (loaded && !found.isEmpty()) {
            LOG.info("PokeBadges is installed: {} badge advancements use and award PokeBadges badges", found.size());
        }
    }

    /** After an advancement criterion is awarded: gives its badge once the advancement is done. */
    public static void onAdvancementAwarded(ServerPlayer player, AdvancementHolder advancement) {
        if (!isLoaded()) return;
        ResourceLocation badge = badges.get(advancement.id());
        if (badge == null || !player.getAdvancements().getOrStartProgress(advancement).isDone()) return;
        PokeBadgesBridge.award(player, badge);
    }

    /**
     * On join: gives the badges of badge advancements earned before PokeBadges was installed. Badges the player has
     * ever had are skipped, so one they took out of their Badge Box isn't forced back in.
     */
    public static void onJoin(ServerPlayer player) {
        if (!isLoaded()) return;
        badges.forEach((advancementId, badge) -> {
            AdvancementHolder advancement = player.server.getAdvancements().get(advancementId);
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                PokeBadgesBridge.awardIfNeverOwned(player, badge);
            }
        });
    }
}
