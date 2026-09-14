package com.davidblackcn.mixin.client;

import com.davidblackcn.lorianarchorbit.client.connected.ChestRenderStateExtension;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ChestRenderState.class)
public abstract class ChestRenderStateMixin implements ChestRenderStateExtension {
    @Unique
    private boolean lorianArchOrbit$connectionFaceExposed;

    @Override
    public boolean lorianArchOrbit$connectionFaceExposed() {
        return lorianArchOrbit$connectionFaceExposed;
    }

    @Override
    public void lorianArchOrbit$setConnectionFaceExposed(boolean exposed) {
        lorianArchOrbit$connectionFaceExposed = exposed;
    }
}
