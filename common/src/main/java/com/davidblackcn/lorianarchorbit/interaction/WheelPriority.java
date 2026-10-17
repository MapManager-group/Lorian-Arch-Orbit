package com.davidblackcn.lorianarchorbit.interaction;

public enum WheelPriority {
    REACH_ADJUSTMENT(100),
    SMART_PICK(200),
    PALETTE_WHEEL(300);

    private final int value;

    WheelPriority(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
