package com.davidblackcn.lorianarchorbit.interaction;

import java.util.Objects;

public final class RadialAnimationState {
    private static final double CLOCKWISE_DELAY_PORTION = 0.6;
    private final RadialAnimationMode mode;
    private final long startedAtMillis;
    private final long durationMillis;

    public RadialAnimationState(RadialAnimationMode mode, long startedAtMillis, long durationMillis) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (startedAtMillis < 0) {
            throw new IllegalArgumentException("startedAtMillis must not be negative");
        }
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
    }

    public RadialAnimationMode mode() {
        return mode;
    }

    public double entryProgress(int visibleIndex, int visibleCount, long nowMillis) {
        if (visibleIndex < 0 || visibleIndex >= visibleCount || visibleCount <= 0) {
            throw new IllegalArgumentException("visibleIndex must identify a visible entry");
        }
        if (mode == RadialAnimationMode.OFF) {
            return 1.0;
        }
        double global = clamp((double) (nowMillis - startedAtMillis) / durationMillis);
        if (mode == RadialAnimationMode.EXPAND || visibleCount == 1) {
            return easeOut(global);
        }
        double delay = ((double) visibleIndex / visibleCount) * CLOCKWISE_DELAY_PORTION;
        double local = clamp((global - delay) / (1.0 - delay));
        return easeOut(local);
    }

    public boolean complete(long nowMillis) {
        return mode == RadialAnimationMode.OFF || nowMillis - startedAtMillis >= durationMillis;
    }

    private static double easeOut(double progress) {
        double inverse = 1.0 - progress;
        return 1.0 - inverse * inverse * inverse;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
