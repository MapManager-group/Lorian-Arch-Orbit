package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.davidblackcn.lorianarchorbit.client.connected.SealedDoubleChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
    @Redirect(
            method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/SubmitNodeCollector;"
                    + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiblockChestResources;"
                            + "select(Lnet/minecraft/world/level/block/state/properties/ChestType;)"
                            + "Ljava/lang/Object;"
            )
    )
    private Object lorianArchOrbit$selectChestModel(
            MultiblockChestResources<?> vanillaModels,
            ChestType type
    ) {
        Object vanillaModel = vanillaModels.select(type);
        if (type == ChestType.SINGLE
                || !ConnectedTextureRuntime.enabled(ConnectionFixKind.CHEST)) {
            return vanillaModel;
        }
        return SealedDoubleChestModel.select(type);
    }
}
