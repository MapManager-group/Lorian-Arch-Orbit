package com.davidblackcn.lorianarchorbit.interaction;

public final class RadialRotationState {
    private static final double FULL_TURN = Math.PI * 2.0;
    private final double initialOffsetRadians;
    private final long startedAtMillis;
    private final long durationMillis;

    public RadialRotationState(double initialOffsetRadians, long startedAtMillis, long durationMillis) {
        if (!Double.isFinite(initialOffsetRadians)) {
            throw new IllegalArgumentException("initialOffsetRadians must be finite");
        }
        if (startedAtMillis < 0) {
            throw new IllegalArgumentException("startedAtMillis must not be negative");
        }
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.initialOffsetRadians = clampTurn(initialOffsetRadians);
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
    }

    public static RadialRotationState idle(long nowMillis, long durationMillis) {
        return new RadialRotationState(0.0, nowMillis, durationMillis);
    }

    public RadialRotationState retarget(
            int selectionSteps,
            int entryCount,
            long nowMillis,
            long newDurationMillis
    ) {
        if (entryCount <= 0) {
            throw new IllegalArgumentException("entryCount must be positive");
        }
        double stepOffset = FULL_TURN * selectionSteps / entryCount;
        return new RadialRotationState(offsetRadians(nowMillis) + stepOffset, nowMillis, newDurationMillis);
    }

    public double offsetRadians(long nowMillis) {
        double progress = Math.max(0.0, Math.min(1.0,
                (double) (nowMillis - startedAtMillis) / durationMillis
        ));
        double remaining = 1.0 - easeOut(progress);
        return initialOffsetRadians * remaining;
    }

    private static double easeOut(double progress) {
        double inverse = 1.0 - progress;
        return 1.0 - inverse * inverse * inverse;
    }

    private static double clampTurn(double radians) {
        return Math.max(-FULL_TURN, Math.min(FULL_TURN, radians));
    }
}
