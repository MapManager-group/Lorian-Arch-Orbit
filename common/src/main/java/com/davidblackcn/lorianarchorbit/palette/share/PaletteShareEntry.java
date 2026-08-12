package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;

import java.util.Objects;

public record PaletteShareEntry(PaletteShareLayer layer, PaletteGroup group) {
    public PaletteShareEntry {
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(group, "group");
    }
}
