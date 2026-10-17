package com.davidblackcn.lorianarchorbit.reach;

public final class ReachProtocol {
    public static final int VERSION = 1;
    public static final int MINIMUM_DISTANCE = 5;
    public static final int MAXIMUM_DISTANCE = 128;

    private ReachProtocol() {
    }

    public static int clamp(int requested, int serverMaximum) {
        return Math.max(MINIMUM_DISTANCE, Math.min(Math.min(MAXIMUM_DISTANCE, serverMaximum), requested));
    }
}
