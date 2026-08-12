package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

public final class PaletteWheelFeature extends EmptyBuiltinFeature {
    public static final String ID = "palette_wheel";

    public PaletteWheelFeature() {
        super(ID, FeatureSide.CLIENT, true);
    }
}
