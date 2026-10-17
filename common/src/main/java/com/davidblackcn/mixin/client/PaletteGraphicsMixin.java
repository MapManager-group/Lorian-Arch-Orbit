package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.PaletteRenderContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class PaletteGraphicsMixin {
    @Inject(method = "guiWidth()I", at = @At("HEAD"), cancellable = true)
    private void lorianArchOrbit$canvasWidth(CallbackInfoReturnable<Integer> callback) {
        var viewport = PaletteRenderContext.viewport((GuiGraphicsExtractor) (Object) this);
        if (viewport != null) callback.setReturnValue(viewport.width());
    }

    @Inject(method = "guiHeight()I", at = @At("HEAD"), cancellable = true)
    private void lorianArchOrbit$canvasHeight(CallbackInfoReturnable<Integer> callback) {
        var viewport = PaletteRenderContext.viewport((GuiGraphicsExtractor) (Object) this);
        if (viewport != null) callback.setReturnValue(viewport.height());
    }

    @WrapMethod(method = "containsPointInScissor(II)Z")
    private boolean lorianArchOrbit$canvasScissor(int x, int y, Operation<Boolean> original) {
        var viewport = PaletteRenderContext.viewport((GuiGraphicsExtractor) (Object) this);
        if (viewport != null) {
            x = (int) Math.floor(viewport.guiX(x));
            y = (int) Math.floor(viewport.guiY(y));
        }
        return original.call(x, y);
    }
}
