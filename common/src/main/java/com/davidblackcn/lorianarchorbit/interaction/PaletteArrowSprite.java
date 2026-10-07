package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteArrowStyle;
import java.util.List;

/** One cell is one logical pixel for every style; colors match 26.2's map frame marker. */
public record PaletteArrowSprite(int tipX, List<String> rows) {
    private static final PaletteArrowSprite NONE = new PaletteArrowSprite(0, List.of());
    // Preserve the original map marker exactly at 2x, without smoothing its silhouette.
    private static final PaletteArrowSprite VANILLA = new PaletteArrowSprite(4, List.of(
            "....OO....",
            "....OO....",
            "..OOSSOO..",
            "..OOSSOO..",
            "OOSSGGSSOO",
            "OOSSGGSSOO",
            "OOGGHHGGOO",
            "OOGGHHGGOO",
            "OOGGHHGGOO",
            "OOGGHHGGOO",
            "OOSSGGSSOO",
            "OOSSGGSSOO",
            "..OOOOOO..",
            "..OOOOOO.."));
    private static final PaletteArrowSprite ARROW = new PaletteArrowSprite(5, List.of(
            ".....O.....",
            "....OHO....",
            "...OHGHO...",
            "..OHGGGHO..",
            ".OHGGGGGHO.",
            "OHGGGGGGGHO",
            "OGGGGGGGGSO",
            "OOOOGGSOOOO",
            "...OHGSO...",
            "...OHGSO...",
            "...OHGSO...",
            "...OHGSO...",
            "...OHGSO...",
            "...OOOOO..."));
    private static final PaletteArrowSprite POINTER = new PaletteArrowSprite(3, List.of(
            "...O.......",
            "..OHO......",
            "..OHO......",
            "..OHO......",
            "..OHOOO....",
            "..OHOGHOO..",
            "..OHOGOGHO.",
            ".OOHGGOGGHO",
            "OHGHGGGGGSO",
            "OHGGGGGGGSO",
            ".OGGGGGGGSO",
            "..OGGGGGSO.",
            "...OSSSSO..",
            "...OOOOOO.."));

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
