package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.ppvon.ultimateCobblemonProgression.client.gui.layout.UiCheck;
import org.ppvon.ultimateCobblemonProgression.client.gui.layout.UiSweep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runs the {@link UiCheck layout check} around each frame of a UCP screen, and starts the {@link UiSweep}. */
@Mixin(Screen.class)
public abstract class UiCheckScreenMixin {
    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void ucp$checkStart(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (UiCheck.LIVE && !UiSweep.running() && UiCheck.isOurs(self)) UiCheck.begin(self);
    }

    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void ucp$checkEnd(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (UiCheck.recording() && !UiSweep.running()) UiCheck.showLive(self, g, UiCheck.end());
        // Inside the title screen's frame, so GUI drawing is set up for the sweep's screenshots.
        UiSweep.maybeStart(self);
    }

    // The sweep draws screens many times in one frame; the blurred background may only be drawn once.
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void ucp$skipBackground(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (UiSweep.running()) ci.cancel();
    }
}
