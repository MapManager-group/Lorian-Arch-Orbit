package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;

import java.util.List;

public record PaletteImportResult(
        List<PaletteGroup> primary,
        List<PaletteGroup> secondary,
        int imported,
        int renamed,
        int replaced,
        int skipped
) {
    public PaletteImportResult {
        primary = List.copyOf(primary);
        secondary = List.copyOf(secondary);
    }
}
