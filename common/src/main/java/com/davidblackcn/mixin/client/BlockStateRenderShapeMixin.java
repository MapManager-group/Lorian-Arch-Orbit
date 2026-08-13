package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.invisible.InvisibleBlocksRuntime;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateRenderShapeMixin {
    @Inject(method = "getRenderShape()Lnet/minecraft/world/level/block/RenderShape;", at = @At("HEAD"), cancellable = true)
    private void lorianArchOrbit$showConfiguredInvisibleBlocks(CallbackInfoReturnable<RenderShape> callback) {
        BlockState state = (BlockState) (Object) this;
        if (InvisibleBlocksRuntime.shouldRender(state)) callback.setReturnValue(RenderShape.MODEL);
    }
}
