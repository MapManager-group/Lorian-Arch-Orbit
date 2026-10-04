package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteArrowStyle;
import java.util.List;

/** Upright pixel shapes share the exact green ramp of 26.2's map frame marker. */
public record PaletteArrowSprite(int tipX, int pixelScale, List<String> rows) {
    private static final PaletteArrowSprite NONE = new PaletteArrowSprite(0, 1, List.of());
    private static final PaletteArrowSprite VANILLA = new PaletteArrowSprite(4, 2, List.of(
            "....O...",
            "...OSO..",
            "..OSGSO.",
            "..OGHGO.",
            "..OGHGO.",
            "..OSGSO.",
            "...OOO..",
            "........"));
    private static final PaletteArrowSprite ARROW = new PaletteArrowSprite(4, 1, List.of(
            "....O....",
            "...OHO...",
            "..OHHHO..",
            ".OHGGGHO.",
            "OHGGGGGHO",
            "OHGGGGGHO",
            ".OGGGGGO.",
            ".OSSSSSO.",
            "..OOOOO.."));
    private static final PaletteArrowSprite POINTER = new PaletteArrowSprite(3, 1, List.of(
            "...O.....",
            "..OHO....",
            "..OHO....",
            "..OHO....",
            "..OHHOO..",
            "..OHGHHO.",
            "OSGHGGGO.",
            "OSGGSSSO.",
            ".OSSSSSO.",
            "..OOOOO.."));

    public PaletteArrowSprite { rows = List.copyOf(rows); }
    public static PaletteArrowSprite of(PaletteArrowStyle style) {
        return switch (style) {
            case VANILLA -> VANILLA;
            case ARROW -> ARROW;
            case POINTER -> POINTER;
            case NONE -> NONE;
        };
    }
    public int color(int column, int row) {
        return switch (rows.get(row).charAt(column)) {
            case 'O' -> 0xFF000000;
            case 'H' -> 0xFF00FF4C;
            case 'G' -> 0xFF00E043;
            case 'S' -> 0xFF00BC38;
            default -> 0;
        };
    }
}
