package com.davidblackcn.lorianarchorbit.palette.share;

import java.util.Locale;

public enum PaletteShareLayer {
    PRIMARY,
    SECONDARY;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static PaletteShareLayer parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
