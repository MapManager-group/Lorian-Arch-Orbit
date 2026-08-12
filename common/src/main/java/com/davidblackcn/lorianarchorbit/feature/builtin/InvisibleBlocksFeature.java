package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

public final class InvisibleBlocksFeature extends EmptyBuiltinFeature {
    public static final String ID = "invisible_blocks";

    public InvisibleBlocksFeature() {
        super(ID, FeatureSide.CLIENT, true);
    }
}
