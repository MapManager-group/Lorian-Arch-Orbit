package com.davidblackcn.lorianarchorbit;

import com.davidblackcn.lorianarchorbit.feature.FeatureRegistry;
import com.davidblackcn.lorianarchorbit.feature.builtin.BuiltinFeatures;
import com.davidblackcn.lorianarchorbit.config.CommonConfigRuntime;
import com.davidblackcn.lorianarchorbit.reach.ServerReachRuntime;

public final class LorianArchOrbit {
    public static final String MOD_ID = "lorian_arch_orbit";
    public static final String MOD_NAME = "Lorian’s Arch Orbit";
    private static final FeatureRegistry FEATURES = BuiltinFeatures.createRegistry();

    private LorianArchOrbit() {
    }

    public static void initialize() {
        CommonConfigRuntime.initialize();
        ServerReachRuntime.initialize();
    }

    public static FeatureRegistry features() {
        return FEATURES;
    }
}
