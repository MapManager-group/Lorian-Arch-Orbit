package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.davidblackcn.lorianarchorbit.client.connected.ChestConnectionCapModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
    @Shadow
    @Final
    private SpriteGetter sprites;

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
            )
    )
    private void lorianArchOrbit$submitConnectionCap(
            ChestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo callback
    ) {
        if (state.type == ChestType.SINGLE
                || !ConnectedTextureRuntime.enabled(ConnectionFixKind.CHEST)) {
            return;
        }

        float closed = 1.0F - state.open;
        float animatedOpen = 1.0F - closed * closed * closed;
        ChestType spriteType = ChestConnectionCapModel.spriteType(state.type);
        collector.submitModel(
                ChestConnectionCapModel.select(state.type),
                animatedOpen,
                poseStack,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                Sheets.chooseSprite(state.material, spriteType),
                sprites,
                0,
                state.breakProgress
        );
    }
}
