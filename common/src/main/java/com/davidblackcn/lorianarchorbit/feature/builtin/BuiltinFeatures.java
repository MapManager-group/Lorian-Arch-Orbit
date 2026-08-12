package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureRegistry;

public final class BuiltinFeatures {
    private BuiltinFeatures() {
    }

    public static FeatureRegistry createRegistry() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(new ReachExtensionFeature());
        registry.register(new PaletteWheelFeature());
        registry.register(new SmartPickFeature());
        registry.register(new ConnectedTextureFixFeature());
        registry.register(new InvisibleBlocksFeature());
        registry.freeze();
        return registry;
    }
}
