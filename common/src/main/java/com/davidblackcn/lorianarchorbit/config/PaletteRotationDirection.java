package com.davidblackcn.lorianarchorbit.config;

/** Wheel motion for positive (upward) scrolling; negative scrolling reverses it. */
public enum PaletteRotationDirection {
    CLOCKWISE(-1),
    COUNTERCLOCKWISE(1);

    private final int selectionSign;

    PaletteRotationDirection(int selectionSign) {
        this.selectionSign = selectionSign;
    }

    public int selectionSteps(int scrollSteps) {
        return selectionSign * scrollSteps;
    }
}
