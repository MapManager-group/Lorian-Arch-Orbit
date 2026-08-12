package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

public final class ReachExtensionFeature extends EmptyBuiltinFeature {
    public static final String ID = "reach_extension";

    public ReachExtensionFeature() {
        super(ID, FeatureSide.BOTH, false);
    }
}
