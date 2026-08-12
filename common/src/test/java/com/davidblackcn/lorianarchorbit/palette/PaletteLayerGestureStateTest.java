package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteLayerGestureStateTest {
    @Test
    void firstPressOpensPrimaryImmediately() {
        PaletteLayerGestureState state = new PaletteLayerGestureState();
        assertEquals(PaletteLayerGestureState.Layer.PRIMARY,
                state.accept(InputGesture.PRESSED).orElseThrow());
        assertTrue(state.accept(InputGesture.LONG_PRESSED).isEmpty());
    }

    @Test
    void doublePressSwitchesImmediatelyToSecondary() {
        PaletteLayerGestureState state = new PaletteLayerGestureState();
        assertEquals(PaletteLayerGestureState.Layer.SECONDARY,
                state.accept(InputGesture.DOUBLE_PRESSED).orElseThrow());
    }

    @Test
    void releaseAndCancellationDoNotOpenAWheel() {
        PaletteLayerGestureState state = new PaletteLayerGestureState();
        assertTrue(state.accept(InputGesture.RELEASED).isEmpty());
        assertTrue(state.accept(InputGesture.CANCELLED).isEmpty());
    }
}
