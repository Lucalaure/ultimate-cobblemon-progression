package org.ppvon.ultimateCobblemonProgression.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.ppvon.ultimateCobblemonProgression.client.RctTrainerLevels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Shows a Radical Cobblemon Trainers trainer's level next to the name over its head, e.g. "Ace Trainer Kendra Lv 22":
 * the level of its strongest Pokémon (see {@link RctTrainerLevels}). Only the drawn name tag changes; the trainer's
 * name in chat and battle messages stays as it was.
 */
@Mixin(EntityRenderer.class)
public abstract class RctTrainerNameTagMixin {
    @ModifyArg(method = "render", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;renderNameTag(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V"))
    private Component ucp$addTrainerLevel(Entity entity, Component name, PoseStack poseStack, MultiBufferSource buffers, int light, float partialTick) {
        int level = RctTrainerLevels.strongestLevel(entity);
        if (level <= 0) return name;
        return name.copy().append(Component.literal(" Lv " + level).withStyle(ChatFormatting.GRAY));
    }
}
