package com.davidblackcn.lorianarchorbit.fabric;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import net.fabricmc.api.ModInitializer;

public final class LorianArchOrbitFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LorianArchOrbit.initialize();
    }
}
