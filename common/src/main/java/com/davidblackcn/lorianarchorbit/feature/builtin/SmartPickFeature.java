package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

public final class SmartPickFeature extends EmptyBuiltinFeature {
    public static final String ID = "smart_pick";

    public SmartPickFeature() {
        super(ID, FeatureSide.CLIENT, true);
    }
}
