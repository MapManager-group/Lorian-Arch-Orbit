package com.davidblackcn.mixin.client.compat;

import com.davidblackcn.lorianarchorbit.client.connected.ChestConnectionCapModel;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectionFixKind;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** OBE 1.1.49 bakes closed chests without calling ChestRenderer.submit. */
@Pseudo
@Mixin(targets = "fr.madu59.obe.client.model.BlockEntityStateModel", remap = false)
public abstract class ObeChestModelMixin {
    @Shadow
    private void getBakedQuads(List<BakedQuad> output, ModelPart part, PoseStack poses,
                               TextureAtlasSprite sprite, String partName, BlockState state) {
        throw new AssertionError();
    }

    @Inject(
            method = "generateModel(Lnet/minecraft/client/model/geom/ModelLayerLocation;"
                    + "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;Z"
                    + "Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At(value = "INVOKE", target = "Lfr/madu59/obe/client/model/BlockEntityStateModel;"
                    + "createQuadCollection(Ljava/util/List;)"
                    + "Lnet/minecraft/client/resources/model/geometry/QuadCollection;")
    )
    private void lorianArchOrbit$bakeClosedChestCap(
            ModelLayerLocation layer, TextureAtlasSprite sprite, PoseStack poses,
            boolean ambientOcclusion, BlockState state, CallbackInfo callback,
            @Local List<BakedQuad> quads
    ) {
        if (state == null || !(state.getBlock() instanceof ChestBlock)
                || !ConnectedTextureRuntime.enabled(ConnectionFixKind.CHEST)) return;
        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.SINGLE) return;
        Identifier opposite = ChestConnectionCapModel.oppositeTexture(sprite.contents().name(), type);
        if (opposite == null) return;

        // OBE stitches entity textures into the block atlas. Reuse that atlas and its
        // own quad conversion so transforms, lighting and terrain materials match.
        TextureAtlasSprite capSprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(opposite);
        ModelPart cap = ChestConnectionCapModel.closedRoot(type);
        for (String partName : List.of("bottom", "lid", "lock")) {
            getBakedQuads(quads, cap.getChild(partName), poses, capSprite, partName, state);
        }
    }
}
