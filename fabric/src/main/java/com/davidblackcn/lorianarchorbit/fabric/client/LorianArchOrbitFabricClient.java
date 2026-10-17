package com.davidblackcn.lorianarchorbit.fabric.client;

import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureModelFixer;
import com.davidblackcn.lorianarchorbit.fabric.client.compat.radial.RadialInputCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.loader.api.FabricLoader;

public final class LorianArchOrbitFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(context -> {
            ConnectedTextureModelFixer.beginModelBake();
            context.modifyBlockModelAfterBake().register(
                    (model, modelContext) -> ConnectedTextureModelFixer.wrap(modelContext.state(), model)
            );
        });
        ClientConfigRuntime.initialize();
        if (FabricLoader.getInstance().isModLoaded("radial")) {
            RadialInputCompat.initialize();
        }
    }
}
