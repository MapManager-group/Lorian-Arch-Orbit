package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PressGestureStateMachineTest {
    private static final Object WORLD = new Object();
    private static final Object KEY = "key-r";

    @Test
    public void reportsShortPressAndRelease() {
        PressGestureStateMachine state = state();

        assertGestures(state.update(0, false, KEY, true, true, WORLD));
        assertGestures(state.update(10, true, KEY, true, true, WORLD), InputGesture.PRESSED);
        assertGestures(
                state.update(80, false, KEY, true, true, WORLD),
                InputGesture.SHORT_PRESSED,
                InputGesture.RELEASED
        );
        assertFalse(state.isPressed());
    }

    @Test
    public void crossesLongPressThresholdAfterOneLongFrame() {
        PressGestureStateMachine state = state();

        assertGestures(state.update(0, true, KEY, true, true, WORLD), InputGesture.PRESSED);
        List<InputGestureEvent> events = state.update(500, true, KEY, true, true, WORLD);
        assertGestures(events, InputGesture.LONG_PRESSED, InputGesture.HELD);
        assertEquals(500, events.getFirst().heldMillis());
        assertTrue(state.isLongPressed());
        assertGestures(state.update(510, false, KEY, true, true, WORLD), InputGesture.RELEASED);
    }

    @Test
    public void recognizesDoublePressAtInclusiveBoundaryButNotBeyondIt() {
        PressGestureStateMachine state = state();
        state.update(0, false, KEY, true, true, WORLD);
        state.update(10, true, KEY, true, true, WORLD);
        state.update(20, false, KEY, true, true, WORLD);

        assertGestures(
                state.update(220, true, KEY, true, true, WORLD),
                InputGesture.PRESSED,
                InputGesture.DOUBLE_PRESSED
        );
        state.update(230, false, KEY, true, true, WORLD);
        state.update(440, true, KEY, true, true, WORLD);
        state.update(450, false, KEY, true, true, WORLD);
        assertGestures(state.update(651, true, KEY, true, true, WORLD), InputGesture.PRESSED);
    }

    @Test
    public void focusLossCancelsAndSuppressesUntilPhysicalRelease() {
        PressGestureStateMachine state = state();
        state.update(0, true, KEY, true, true, WORLD);

        assertGestures(state.update(20, true, KEY, true, false, WORLD), InputGesture.CANCELLED);
        assertGestures(state.update(30, true, KEY, true, true, WORLD));
        assertGestures(state.update(40, false, KEY, true, true, WORLD));
        assertGestures(state.update(50, true, KEY, true, true, WORLD), InputGesture.PRESSED);
    }

    @Test
    public void worldChangeBindingChangeAndDisableAllCancelHeldInput() {
        PressGestureStateMachine state = state();
        state.update(0, true, KEY, true, true, WORLD);
        assertGestures(state.update(10, true, KEY, true, true, new Object()), InputGesture.CANCELLED);
        state.update(20, false, KEY, true, true, WORLD);
        state.update(30, true, KEY, true, true, WORLD);
        assertGestures(state.update(40, true, "key-v", true, true, WORLD), InputGesture.CANCELLED);
        state.update(50, false, "key-v", true, true, WORLD);
        state.update(60, true, "key-v", true, true, WORLD);
        assertGestures(state.update(70, true, "key-v", false, true, WORLD), InputGesture.CANCELLED);
    }

    private static PressGestureStateMachine state() {
        return new PressGestureStateMachine(new PressTiming(180, 200));
    }

    private static void assertGestures(List<InputGestureEvent> actual, InputGesture... expected) {
        assertEquals(List.of(expected), actual.stream().map(InputGestureEvent::gesture).toList());
    }
}
