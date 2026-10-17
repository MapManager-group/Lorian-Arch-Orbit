package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.invisible.InvisibleBlocksRuntime;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelBreakingEffectMixin {
    @Inject(
            method = "addBreakingParticles(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lorianArchOrbit$skipEmptyMarkerBreakingEffect(
            BlockPos position, Direction direction, BlockState state, CallbackInfo callback
    ) {
        // The 26.3 outer effects method also plays sounds; suppress only the marker particles.
        if (InvisibleBlocksRuntime.isSupportedState(state)) callback.cancel();
    }

}
