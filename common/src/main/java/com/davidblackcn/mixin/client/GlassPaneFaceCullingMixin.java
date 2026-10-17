package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.davidblackcn.lorianarchorbit.client.connected.GlassPaneConnectionFix;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IronBarsBlock.class)
public abstract class GlassPaneFaceCullingMixin {
    @Inject(
            method = "skipRendering(Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;"
                    + "Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lorianArchOrbit$keepExposedGlassPaneFace(
            BlockState state,
            BlockState adjacentState,
            Direction direction,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (GlassPaneConnectionFix.shouldRenderSharedFace(state, adjacentState, direction)
                && ConnectedTextureRuntime.enabled(ConnectionFixKind.GLASS_PANE)) {
            callback.setReturnValue(false);
        }
    }
}
