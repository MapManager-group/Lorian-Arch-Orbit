package com.davidblackcn.lorianarchorbit.interaction;

public record InputGestureEvent(InputGesture gesture, long timestampMillis, long heldMillis) {
    public InputGestureEvent {
        if (gesture == null) {
            throw new NullPointerException("gesture");
        }
        if (heldMillis < 0) {
            throw new IllegalArgumentException("heldMillis must not be negative");
        }
    }
}
