package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import com.davidblackcn.lorianarchorbit.interaction.InputGestureEvent;
import com.davidblackcn.lorianarchorbit.interaction.PressGestureStateMachine;
import com.davidblackcn.lorianarchorbit.interaction.PressTiming;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PaletteLayerGestureStateTest {
    @Test
    void actualInputStreamOpensPrimarySecondaryThenTemporaryWithoutDoubleEventDowngrading() {
        var input = new PressGestureStateMachine(new PressTiming(180, 250));
        var state = new PaletteLayerGestureState();
        List<PaletteLayerGestureState.Layer> opened = new ArrayList<>();
        for (int tap = 0; tap < 3; tap++) {
            input.update(tap * 100, true, "R", true, true, "world")
                    .forEach(event -> state.accept(event).ifPresent(opened::add));
            input.update(tap * 100 + 50, false, "R", true, true, "world")
                    .forEach(event -> state.accept(event).ifPresent(opened::add));
        }
        assertEquals(List.of(PaletteLayerGestureState.Layer.PRIMARY, PaletteLayerGestureState.Layer.SECONDARY,
                PaletteLayerGestureState.Layer.TEMPORARY), opened);
    }

    @Test
    void longThirdPressKeepsTemporaryOpenAndNextGestureStartsAtPrimary() {
        var input = new PressGestureStateMachine(new PressTiming(180, 250));
        var state = new PaletteLayerGestureState();
        for (int tap = 0; tap < 2; tap++) {
            input.update(tap * 100, true, "R", true, true, "world").forEach(state::accept);
            input.update(tap * 100 + 50, false, "R", true, true, "world").forEach(state::accept);
        }
        var third = input.update(200, true, "R", true, true, "world");
        assertEquals(PaletteLayerGestureState.Layer.TEMPORARY, state.accept(third.getFirst()).orElseThrow());
        third.subList(1, third.size()).forEach(event -> assertTrue(state.accept(event).isEmpty()));
        input.update(400, true, "R", true, true, "world").forEach(event -> assertTrue(state.accept(event).isEmpty()));
        input.update(450, false, "R", true, true, "world").forEach(state::accept);
        assertEquals(PaletteLayerGestureState.Layer.PRIMARY,
                state.accept(input.update(500, true, "R", true, true, "world").getFirst()).orElseThrow());
    }

    @Test
    void timeoutCancellationExplicitResetAndClockRegressionBreakTheChain() {
        for (String reason : List.of("timeout", "cancel", "reset", "clock")) {
            var state = new PaletteLayerGestureState();
            state.accept(event(InputGesture.PRESSED, 100));
            state.accept(event(InputGesture.SHORT_PRESSED, 150));
            state.accept(event(InputGesture.RELEASED, 150));
            if (reason.equals("cancel")) state.accept(event(InputGesture.CANCELLED, 160));
            if (reason.equals("reset")) state.reset();
            long next = reason.equals("timeout") ? 401 : reason.equals("clock") ? 50 : 200;
            assertEquals(PaletteLayerGestureState.Layer.PRIMARY,
                    state.accept(event(InputGesture.PRESSED, next)).orElseThrow(), reason);
        }
    }

    @Test
    void exactMultiPressDeadlineIsInclusiveAndRepeatedQuickTapsStayTemporary() {
        var state = new PaletteLayerGestureState();
        state.accept(event(InputGesture.PRESSED, 0));
        state.accept(event(InputGesture.SHORT_PRESSED, 10));
        assertEquals(PaletteLayerGestureState.Layer.SECONDARY, state.accept(event(InputGesture.PRESSED, 260)).orElseThrow());
        state.accept(event(InputGesture.SHORT_PRESSED, 270));
        assertEquals(PaletteLayerGestureState.Layer.TEMPORARY, state.accept(event(InputGesture.PRESSED, 280)).orElseThrow());
        state.accept(event(InputGesture.SHORT_PRESSED, 290));
        assertEquals(PaletteLayerGestureState.Layer.TEMPORARY, state.accept(event(InputGesture.PRESSED, 300)).orElseThrow());
    }

    private static InputGestureEvent event(InputGesture gesture, long time) {
        return new InputGestureEvent(gesture, time, 0);
    }
}
