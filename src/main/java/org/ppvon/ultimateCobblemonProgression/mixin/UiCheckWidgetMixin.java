package org.ppvon.ultimateCobblemonProgression.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.ppvon.ultimateCobblemonProgression.client.gui.layout.UiCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells the {@link UiCheck layout check} which widget is drawing, so a widget's own text may sit inside it. */
@Mixin(AbstractWidget.class)
public abstract class UiCheckWidgetMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void ucp$widgetStart(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        UiCheck.pushWidget((AbstractWidget) (Object) this);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ucp$widgetEnd(GuiGraphics g, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        UiCheck.popWidget();
    }
}
