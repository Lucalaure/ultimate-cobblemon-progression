package org.ppvon.ultimateCobblemonProgression.common.levelcap;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Set;

public final class CandyEntityBlock {
    private static final Set<ResourceLocation> CANDIES = Set.of(
            id("cobblemon","exp_candy_xs"), id("cobblemon","exp_candy_s"),
            id("cobblemon","exp_candy_m"),  id("cobblemon","exp_candy_l"),
            id("cobblemon","exp_candy_xl"), id("cobblemon","rare_candy")
    );
    private static ResourceLocation id(String ns,String p){ return ResourceLocation.fromNamespaceAndPath(ns,p); }

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if(!ConfigLoader.DO_LEVEL_CAP.get() || RegionRegistry.isActive()) {
                return InteractionResult.PASS;
            }

            if (!(player instanceof ServerPlayer sp) || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            ItemStack held = sp.getMainHandItem();

            if (!isCandy(held.getItem())) {
                return InteractionResult.PASS;
            }

            if (!(entity instanceof PokemonEntity pe)) {
                return InteractionResult.PASS;
            }

            Pokemon mon = pe.getPokemon();
            if (mon == null) {
                return InteractionResult.PASS;
            }

            int trainerTier = TrainerLevelComponents.KEY.get(sp).getLevel();
            int cap = TierRegistry.getCapForTier(trainerTier);
            if (mon.getLevel() >= cap) {
                sp.displayClientMessage(Component.literal(
                        mon.getDisplayName(false).getString() + " is at your Trainer cap (" + cap + "). Candy blocked."
                ), true);
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isCandy(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null && CANDIES.contains(key);
    }
}
