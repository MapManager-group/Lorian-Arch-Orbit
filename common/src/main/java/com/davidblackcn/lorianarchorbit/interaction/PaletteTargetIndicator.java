package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import java.util.Optional;

/** Target-direction marker with an independent clock; scrolling never changes its animation. */
public record PaletteTargetIndicator(double tipX, double tipY, double forwardX, double forwardY) {
    public static Optional<PaletteTargetIndicator> at(int centerX, int centerY, double radius,
                                                     PaletteTargetPosition position, long nowMillis) {
        // During expansion/collapse or in extremely small rings, keep the center label readable.
        if (radius < 48) return Optional.empty();
        double angle = Math.PI / 2 + position.angleOffset();
        int dx = (int) Math.round(Math.cos(angle));
        int dy = (int) Math.round(Math.sin(angle));
        double bob = 1.5 * (1 - Math.cos(Math.floorMod(nowMillis, 900) * Math.PI * 2 / 900));
        double tipDistance = radius - PaletteRadialLayout.ITEM_HALF_SIZE - 6 - bob;
        return Optional.of(new PaletteTargetIndicator(centerX + dx * tipDistance,
                centerY + dy * tipDistance, dx, dy));
    }

    /** x crosses the triangle; y <= 0 extends inward from the tip. */
    public HudPoint pixel(int x, int y) {
        return new HudPoint((int) Math.round(tipX + forwardY * x + forwardX * y),
                (int) Math.round(tipY - forwardX * x + forwardY * y));
    }
}
