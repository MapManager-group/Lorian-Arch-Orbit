package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.invisible.InvisibleBlocksRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelBreakingEffectMixin {
    @Inject(
            method = "addBreakingBlockEffect(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lorianArchOrbit$skipEmptyMarkerBreakingEffect(
            BlockPos position, Direction direction, CallbackInfo callback
    ) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (InvisibleBlocksRuntime.isSupportedState(level.getBlockState(position))) callback.cancel();
    }
}
