package com.davidblackcn.lorianarchorbit.palette;

public final class PaletteWheelRules {
    private PaletteWheelRules() {
    }

    public static boolean canOpen(int resolvedDistinctMemberCount) {
        return resolvedDistinctMemberCount > 1;
    }
}
