package com.davidblackcn.lorianarchorbit.palette.share;

import java.util.List;

public record PaletteShareBundle(String name, List<PaletteShareEntry> entries) {
    public PaletteShareBundle {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("share name must not be blank");
        }
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("share must contain at least one group");
        }
    }
}
