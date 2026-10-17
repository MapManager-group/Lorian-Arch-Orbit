package com.davidblackcn.lorianarchorbit.smartpick;

public record SmartPickDirection(int x, int y, int z) {
    public SmartPickDirection {
        if (Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) {
            throw new IllegalArgumentException("direction must be one axis-aligned unit vector");
        }
    }
}
