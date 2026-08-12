package com.davidblackcn.lorianarchorbit.smartpick;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SmartPickGestureStateTest {
    @Test
    void shortPressNeverOpensWheelAfterVanillaAlreadyHandledThePick() {
        SmartPickGestureState state = new SmartPickGestureState();
        state.armForVanillaPick();

        assertEquals(0, state.accept(InputGesture.PRESSED).size());
        assertEquals(0, state.accept(InputGesture.SHORT_PRESSED).size());
        assertEquals(0, state.accept(InputGesture.RELEASED).size());
    }

    @Test
    void intentDelayOpensOnceAndReleaseConfirmsWithoutReplayingOriginal() {
        SmartPickGestureState state = new SmartPickGestureState();
        state.armForVanillaPick();

        assertEquals(0, state.accept(InputGesture.PRESSED).size());
        assertEquals(SmartPickGestureState.Action.OPEN_SMART_PICK,
                state.accept(InputGesture.LONG_PRESSED).getFirst());
        state.smartOpened(true);
        assertEquals(SmartPickGestureState.Action.CONFIRM_SMART_PICK,
                state.accept(InputGesture.RELEASED).getFirst());
        assertEquals(0, state.accept(InputGesture.RELEASED).size());
    }

    @Test
    void failedDelayedOpenDoesNotRepeatVanillaPickAndCancellationDoesNotConfirm() {
        SmartPickGestureState failed = new SmartPickGestureState();
        failed.armForVanillaPick();
        assertEquals(0, failed.accept(InputGesture.PRESSED).size());
        assertEquals(SmartPickGestureState.Action.OPEN_SMART_PICK,
                failed.accept(InputGesture.LONG_PRESSED).getFirst());
        failed.smartOpened(false);
        assertEquals(0, failed.accept(InputGesture.RELEASED).size());

        SmartPickGestureState cancelled = new SmartPickGestureState();
        cancelled.armForVanillaPick();
        assertEquals(SmartPickGestureState.Action.CANCEL, cancelled.accept(InputGesture.CANCELLED).getFirst());
        assertEquals(0, cancelled.accept(InputGesture.RELEASED).size());
    }
}
