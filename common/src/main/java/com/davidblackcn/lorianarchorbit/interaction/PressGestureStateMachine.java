package com.davidblackcn.lorianarchorbit.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PressGestureStateMachine {
    private final PressTiming timing;
    private boolean initialized;
    private boolean pressed;
    private boolean longPressed;
    private boolean suppressedUntilRelease;
    private long pressedAt;
    private long lastShortRelease = -1;
    private long lastTimestamp = -1;
    private Object bindingToken;
    private Object worldToken;

    public PressGestureStateMachine(PressTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    public synchronized List<InputGestureEvent> update(
            long nowMillis,
            boolean down,
            Object currentBindingToken,
            boolean enabled,
            boolean focused,
            Object currentWorldToken
    ) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis must not be negative");
        }
        List<InputGestureEvent> events = new ArrayList<>();
        boolean timeRegressed = lastTimestamp >= 0 && nowMillis < lastTimestamp;
        boolean contextChanged = initialized && (
                !Objects.equals(bindingToken, currentBindingToken)
                        || !Objects.equals(worldToken, currentWorldToken)
        );
        initialized = true;
        bindingToken = currentBindingToken;
        worldToken = currentWorldToken;
        lastTimestamp = nowMillis;

        if (timeRegressed || contextChanged || !enabled || !focused || currentWorldToken == null) {
            cancel(nowMillis, events);
            suppressedUntilRelease = down;
            if (!down) {
                suppressedUntilRelease = false;
            }
            return List.copyOf(events);
        }

        if (suppressedUntilRelease) {
            if (!down) {
                suppressedUntilRelease = false;
            }
            return List.of();
        }

        if (down && !pressed) {
            pressed = true;
            longPressed = false;
            pressedAt = nowMillis;
            events.add(event(InputGesture.PRESSED, nowMillis));
            if (lastShortRelease >= 0 && nowMillis - lastShortRelease <= timing.doublePressMillis()) {
                events.add(event(InputGesture.DOUBLE_PRESSED, nowMillis));
                lastShortRelease = -1;
            }
        }

        if (down && pressed) {
            long held = nowMillis - pressedAt;
            if (!longPressed && held >= timing.longPressMillis()) {
                longPressed = true;
                lastShortRelease = -1;
                events.add(event(InputGesture.LONG_PRESSED, nowMillis));
            }
            if (longPressed) {
                events.add(event(InputGesture.HELD, nowMillis));
            }
        } else if (!down && pressed) {
            if (!longPressed) {
                events.add(event(InputGesture.SHORT_PRESSED, nowMillis));
                lastShortRelease = nowMillis;
            }
            events.add(event(InputGesture.RELEASED, nowMillis));
            pressed = false;
            longPressed = false;
        }
        return List.copyOf(events);
    }

    public synchronized List<InputGestureEvent> reset(long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis must not be negative");
        }
        List<InputGestureEvent> events = new ArrayList<>();
        cancel(nowMillis, events);
        suppressedUntilRelease = false;
        lastShortRelease = -1;
        lastTimestamp = nowMillis;
        return List.copyOf(events);
    }

    public synchronized boolean isPressed() {
        return pressed;
    }

    public synchronized boolean isLongPressed() {
        return longPressed;
    }

    private void cancel(long nowMillis, List<InputGestureEvent> events) {
        if (pressed) {
            events.add(event(InputGesture.CANCELLED, nowMillis));
        }
        pressed = false;
        longPressed = false;
        lastShortRelease = -1;
    }

    private InputGestureEvent event(InputGesture gesture, long nowMillis) {
        return new InputGestureEvent(gesture, nowMillis, pressed ? Math.max(0, nowMillis - pressedAt) : 0);
    }
}
