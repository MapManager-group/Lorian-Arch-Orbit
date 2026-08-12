package com.davidblackcn.lorianarchorbit.fabric.client;

import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureModelFixer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;

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
    }
}
