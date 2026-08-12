package com.davidblackcn.lorianarchorbit.fabric.client;

import com.davidblackcn.lorianarchorbit.client.ClientConfigRuntime;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class LorianArchOrbitModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClientConfigRuntime::createScreen;
    }
}
