package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.AdaptivePaletteScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Screen.class)
public abstract class PaletteScreenMixin {
    @WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V"))
    private void lorianArchOrbit$paletteDeferred(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                                float partialTick, Operation<Void> original) {
        if ((Object) this instanceof AdaptivePaletteScreen screen) {
            screen.extractPaletteDeferred(graphics, mouseX, mouseY, partialTick,
                    (g, x, y, tick) -> original.call(g, x, y, tick));
        } else {
            original.call(graphics, mouseX, mouseY, partialTick);
        }
    }
}
