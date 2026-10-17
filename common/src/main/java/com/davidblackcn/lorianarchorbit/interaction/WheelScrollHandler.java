package com.davidblackcn.lorianarchorbit.interaction;

@FunctionalInterface
public interface WheelScrollHandler {
    boolean onScroll(double amountX, double amountY);
}
