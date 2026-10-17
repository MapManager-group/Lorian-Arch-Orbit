package com.davidblackcn.lorianarchorbit.feature;

import java.util.Objects;

public record FeatureServices(
        ConfigAccess config,
        NetworkAccess network,
        InputAccess input,
        HudAccess hud,
        PlatformAccess platform,
        System.Logger logger
) {
    public interface ConfigAccess {
    }

    public interface NetworkAccess {
    }

    public interface InputAccess {
    }

    public interface HudAccess {
    }

    public interface PlatformAccess {
    }

    private enum NoOpService implements ConfigAccess, NetworkAccess, InputAccess, HudAccess, PlatformAccess {
        INSTANCE
    }

    public FeatureServices {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(hud, "hud");
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(logger, "logger");
    }

    public static FeatureServices noOp(System.Logger logger) {
        Objects.requireNonNull(logger, "logger");
        return new FeatureServices(
                NoOpService.INSTANCE,
                NoOpService.INSTANCE,
                NoOpService.INSTANCE,
                NoOpService.INSTANCE,
                NoOpService.INSTANCE,
                logger
        );
    }
}
