package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

public final class ConnectedTextureFixFeature extends EmptyBuiltinFeature {
    public static final String ID = "connected_texture_fix";

    public ConnectedTextureFixFeature() {
        super(ID, FeatureSide.CLIENT, true);
    }
}
