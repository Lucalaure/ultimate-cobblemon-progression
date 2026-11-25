package org.ppvon.ultimateCobblemonProgression.mixin;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.ppvon.ultimateCobblemonProgression.common.badges.PokeBadgesCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** Lets {@link PokeBadgesCompat} swap badge advancement icons for PokeBadges items before the JSON is parsed. */
@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void ucp$prepareBadges(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager,
                                   ProfilerFiller profiler, CallbackInfo ci) {
        PokeBadgesCompat.prepare(map);
    }
}
