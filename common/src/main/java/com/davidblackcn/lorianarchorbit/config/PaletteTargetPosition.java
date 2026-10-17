package com.davidblackcn.lorianarchorbit.config;

/** Display orientation only; selection and member ordering are unchanged. */
public enum PaletteTargetPosition {
    BOTTOM(0), TOP(Math.PI), LEFT(Math.PI / 2), RIGHT(-Math.PI / 2);

    private final double angleOffset;

    PaletteTargetPosition(double angleOffset) { this.angleOffset = angleOffset; }

    public double angleOffset() { return angleOffset; }
}
