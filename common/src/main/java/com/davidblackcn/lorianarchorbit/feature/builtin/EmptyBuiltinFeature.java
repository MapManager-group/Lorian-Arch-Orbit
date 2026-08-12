package com.davidblackcn.lorianarchorbit.feature.builtin;

import com.davidblackcn.lorianarchorbit.feature.EmptyFeatureConfig;
import com.davidblackcn.lorianarchorbit.feature.Feature;
import com.davidblackcn.lorianarchorbit.feature.FeatureConfigSpec;
import com.davidblackcn.lorianarchorbit.feature.FeatureSide;

import java.util.Objects;

public abstract class EmptyBuiltinFeature implements Feature<EmptyFeatureConfig> {
    private final String id;
    private final FeatureSide side;
    private final boolean enabledByDefault;
    private final FeatureConfigSpec<EmptyFeatureConfig> configSpec;

    protected EmptyBuiltinFeature(String id, FeatureSide side, boolean enabledByDefault) {
        this.id = Objects.requireNonNull(id, "id");
        this.side = Objects.requireNonNull(side, "side");
        this.enabledByDefault = enabledByDefault;
        this.configSpec = FeatureConfigSpec.empty(id);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final FeatureSide side() {
        return side;
    }

    @Override
    public final boolean enabledByDefault() {
        return enabledByDefault;
    }

    @Override
    public final FeatureConfigSpec<EmptyFeatureConfig> configSpec() {
        return configSpec;
    }
}
