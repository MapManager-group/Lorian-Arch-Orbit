package com.davidblackcn.lorianarchorbit.smartpick;

import java.util.Optional;

public record SmartPickSample<T>(boolean loaded, Optional<T> value) {
    public SmartPickSample {
        value = value == null ? Optional.empty() : value;
        if (!loaded && value.isPresent()) {
            throw new IllegalArgumentException("unloaded samples cannot contain a value");
        }
    }

    public static <T> SmartPickSample<T> unloaded() {
        return new SmartPickSample<>(false, Optional.empty());
    }

    public static <T> SmartPickSample<T> empty() {
        return new SmartPickSample<>(true, Optional.empty());
    }

    public static <T> SmartPickSample<T> value(T value) {
        return new SmartPickSample<>(true, Optional.of(value));
    }
}
