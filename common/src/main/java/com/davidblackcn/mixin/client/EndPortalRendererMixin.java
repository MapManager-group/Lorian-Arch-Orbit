package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.client.renderer.blockentity.state.EndPortalRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * End portals use a block-entity renderer rather than the ordinary baked block-model path.
 * Reuse the vanilla End Portal shader for the four sides and bottom, leaving its top face intact.
 */
@Mixin(TheEndPortalRenderer.class)
public abstract class EndPortalRendererMixin {
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/EndPortalRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void lorianArchOrbit$addConnectionFaces(
            EndPortalRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
            CameraRenderState camera, CallbackInfo callback
    ) {
        if (!ConnectedTextureRuntime.enabled(ConnectionFixKind.END_PORTAL)) return;
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP) state.facesToShow.add(direction);
        }
    }
}
