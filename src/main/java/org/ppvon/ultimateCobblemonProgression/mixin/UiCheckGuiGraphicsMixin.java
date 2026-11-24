package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;
import org.ppvon.ultimateCobblemonProgression.client.gui.layout.UiCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports what UCP's screens draw to the {@link UiCheck layout check}. Does nothing unless it's recording. */
@Mixin(GuiGraphics.class)
public abstract class UiCheckGuiGraphicsMixin {

    // Every other drawString (Component, centred, word-wrapped) ends in one of these two.
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"))
    private void ucp$checkString(Font font, String text, int x, int y, int colour, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (UiCheck.recording()) UiCheck.text((GuiGraphics) (Object) this, font, text, x, y);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)I", at = @At("HEAD"))
    private void ucp$checkSequence(Font font, FormattedCharSequence text, int x, int y, int colour, boolean shadow,
                                   CallbackInfoReturnable<Integer> cir) {
        if (UiCheck.recording()) UiCheck.text((GuiGraphics) (Object) this, font, text, x, y);
    }

    // Every fill ends here.
    @Inject(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIIII)V", at = @At("HEAD"))
    private void ucp$checkFill(RenderType type, int x0, int y0, int x1, int y1, int z, int colour, CallbackInfo ci) {
        if (UiCheck.recording()) UiCheck.fill((GuiGraphics) (Object) this, x0, y0, x1, y1, z, colour);
    }

    @Inject(method = "enableScissor(IIII)V", at = @At("HEAD"))
    private void ucp$checkScissor(int x0, int y0, int x1, int y1, CallbackInfo ci) {
        UiCheck.pushScissor(x0, y0, x1, y1);
    }

    @Inject(method = "disableScissor()V", at = @At("HEAD"))
    private void ucp$checkScissorEnd(CallbackInfo ci) {
        UiCheck.popScissor();
    }
}
