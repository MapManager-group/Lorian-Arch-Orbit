package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ChestConnectionCapModel;
import com.davidblackcn.lorianarchorbit.client.connected.ChestConnectionFaceFix;
import com.davidblackcn.lorianarchorbit.client.connected.ChestRenderStateExtension;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
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
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;"
                    + "Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;F"
                    + "Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL")
    )
    private void lorianArchOrbit$extractConnectionFaceState(
            BlockEntity blockEntity,
            ChestRenderState renderState,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
            CallbackInfo callback
    ) {
        ChestRenderStateExtension extension = (ChestRenderStateExtension) renderState;
        extension.lorianArchOrbit$setConnectionFaceExposed(false);
        if (!ConnectedTextureRuntime.enabled(ConnectionFixKind.CHEST)) return;

        Level level = blockEntity.getLevel();
        if (level == null) return;
        extension.lorianArchOrbit$setConnectionFaceExposed(
                ChestConnectionFaceFix.shouldRenderConnectionFace(
                        level, blockEntity.getBlockPos(), blockEntity.getBlockState()
                )
        );
    }

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
    private void lorianArchOrbit$submitConnectionFace(
            ChestRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            CallbackInfo callback
    ) {
        if (!((ChestRenderStateExtension) state).lorianArchOrbit$connectionFaceExposed()
                || state.type == ChestType.SINGLE) {
            return;
        }

        float closed = 1.0F - state.open;
        float animatedOpen = 1.0F - closed * closed * closed;
        collector.submitModel(
                ChestConnectionCapModel.select(state.type),
                animatedOpen,
                poseStack,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                Sheets.chooseSprite(state.material, state.type),
                sprites,
                0,
                state.breakProgress
        );
    }
}
