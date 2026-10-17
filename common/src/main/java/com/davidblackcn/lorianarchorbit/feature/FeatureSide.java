package com.davidblackcn.lorianarchorbit.feature;

import java.util.Objects;

public enum FeatureSide {
    CLIENT,
    SERVER,
    BOTH;

    public boolean supports(RuntimeSide runtimeSide) {
        Objects.requireNonNull(runtimeSide, "runtimeSide");
        return switch (this) {
            case CLIENT -> runtimeSide == RuntimeSide.CLIENT;
            case SERVER -> runtimeSide == RuntimeSide.SERVER;
            case BOTH -> true;
        };
    }
}
