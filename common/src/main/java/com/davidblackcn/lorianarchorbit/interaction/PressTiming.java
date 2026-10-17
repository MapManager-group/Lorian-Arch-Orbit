package com.davidblackcn.lorianarchorbit.interaction;

public record PressTiming(long longPressMillis, long doublePressMillis) {
    public PressTiming {
        if (longPressMillis <= 0) {
            throw new IllegalArgumentException("longPressMillis must be positive");
        }
        if (doublePressMillis <= 0) {
            throw new IllegalArgumentException("doublePressMillis must be positive");
        }
    }
}
