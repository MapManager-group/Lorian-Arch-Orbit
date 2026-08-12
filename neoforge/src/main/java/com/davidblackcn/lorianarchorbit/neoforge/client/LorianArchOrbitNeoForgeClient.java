package com.davidblackcn.lorianarchorbit.neoforge.client;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.davidblackcn.lorianarchorbit.client.connected.ConnectedTextureModelFixer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ModelEvent;

@Mod(value = LorianArchOrbit.MOD_ID, dist = Dist.CLIENT)
public final class LorianArchOrbitNeoForgeClient {
    public LorianArchOrbitNeoForgeClient(ModContainer container, IEventBus modBus) {
        modBus.addListener(LorianArchOrbitNeoForgeClient::modifyModels);
        ClientConfigRuntime.initialize();
        IConfigScreenFactory factory = (ignored, parent) -> ClientConfigRuntime.createScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, factory);
    }

    private static void modifyModels(ModelEvent.ModifyBakingResult event) {
        ConnectedTextureModelFixer.beginModelBake();
        event.getBakingResult().blockStateModels().replaceAll(ConnectedTextureModelFixer::wrap);
    }
}
