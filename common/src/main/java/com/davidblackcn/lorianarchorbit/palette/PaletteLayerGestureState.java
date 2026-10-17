package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;
import com.davidblackcn.lorianarchorbit.interaction.InputGestureEvent;

import java.util.Optional;

public final class PaletteLayerGestureState {
    private long lastShortRelease = -1;
    private long lastTimestamp = -1;
    private int presses;

    public Optional<Layer> accept(InputGestureEvent event) {
        long now = event.timestampMillis();
        if (now < lastTimestamp) reset();
        lastTimestamp = now;
        if (event.gesture() == InputGesture.PRESSED) {
            presses = lastShortRelease >= 0 && now - lastShortRelease <= 250
                    ? Math.min(3, presses + 1) : 1;
            lastShortRelease = -1;
            return Optional.of(switch (presses) {
                case 1 -> Layer.PRIMARY;
                case 2 -> Layer.SECONDARY;
                default -> Layer.TEMPORARY;
            });
        }
        if (event.gesture() == InputGesture.SHORT_PRESSED) lastShortRelease = now;
        if (event.gesture() == InputGesture.LONG_PRESSED || event.gesture() == InputGesture.CANCELLED) reset();
        // The shared input machine also emits DOUBLE_PRESSED after PRESSED; ignore it here.
        return Optional.empty();
    }

    public void reset() {
        lastShortRelease = -1;
        lastTimestamp = -1;
        presses = 0;
    }

    public enum Layer { PRIMARY, SECONDARY, TEMPORARY }
}
