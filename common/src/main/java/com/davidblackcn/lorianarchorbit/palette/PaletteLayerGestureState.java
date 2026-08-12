package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.interaction.InputGesture;

import java.util.Optional;

public final class PaletteLayerGestureState {
    public Optional<Layer> accept(InputGesture gesture) {
        return switch (gesture) {
            case PRESSED -> Optional.of(Layer.PRIMARY);
            case DOUBLE_PRESSED -> Optional.of(Layer.SECONDARY);
            default -> Optional.empty();
        };
    }

    public enum Layer { PRIMARY, SECONDARY }
}
