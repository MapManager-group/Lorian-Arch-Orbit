package com.davidblackcn.lorianarchorbit.interaction;

public final class ScrollAccumulator {
    private double accumulated;

    public int add(double amount) {
        if (!Double.isFinite(amount) || amount == 0.0) {
            return 0;
        }
        if (accumulated != 0.0 && Math.signum(accumulated) != Math.signum(amount)) {
            accumulated = 0.0;
        }
        accumulated += amount;
        int steps = accumulated > 0.0
                ? (int) Math.floor(accumulated)
                : (int) Math.ceil(accumulated);
        accumulated -= steps;
        return steps;
    }

    public void reset() {
        accumulated = 0.0;
    }

    public double remainder() {
        return accumulated;
    }
}
