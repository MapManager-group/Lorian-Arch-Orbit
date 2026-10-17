package com.davidblackcn.lorianarchorbit.feature;

import java.util.Objects;
import java.util.function.Function;

public final class FeatureContext {
    private final RuntimeSide runtimeSide;
    private final FeatureServices services;
    private final Function<String, FeatureState> stateLookup;

    FeatureContext(
            RuntimeSide runtimeSide,
            FeatureServices services,
            Function<String, FeatureState> stateLookup
    ) {
        this.runtimeSide = Objects.requireNonNull(runtimeSide, "runtimeSide");
        this.services = Objects.requireNonNull(services, "services");
        this.stateLookup = Objects.requireNonNull(stateLookup, "stateLookup");
    }

    public RuntimeSide runtimeSide() {
        return runtimeSide;
    }

    public FeatureServices.ConfigAccess config() {
        return services.config();
    }

    public FeatureServices.NetworkAccess network() {
        return services.network();
    }

    public FeatureServices.InputAccess input() {
        return services.input();
    }

    public FeatureServices.HudAccess hud() {
        return services.hud();
    }

    public FeatureServices.PlatformAccess platform() {
        return services.platform();
    }

    public System.Logger logger() {
        return services.logger();
    }

    public FeatureState state(String featureId) {
        return stateLookup.apply(featureId);
    }

    public boolean isEnabled(String featureId) {
        return state(featureId) == FeatureState.ENABLED;
    }
}
