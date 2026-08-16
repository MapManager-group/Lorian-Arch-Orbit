package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PaletteEditorLayoutTest {
    @Test
    void wideEditorsKeepThePreviewAndSingleRowFooter() {
        PaletteEditorLayout layout = PaletteEditorLayout.calculate(1280, 720);

        assertFalse(layout.compactFooter());
        assertTrue(layout.previewWidth() >= 100);
        assertTrue(layout.browserLeft() - (layout.groupLeft() + layout.groupWidth()) >= 20);
        assertEquals(7, layout.visibleTabs());
        assertEquals(694, layout.footerTop());
        assertColumnBounds(layout);
    }

    @Test
    void compactEditorsWrapControlsBeforeTheyCanOverlap() {
        PaletteEditorLayout layout = PaletteEditorLayout.calculate(512, 288);

        assertTrue(layout.compactFooter());
        assertTrue(layout.footerTop() < 262);
        assertTrue(layout.contentBottom() < layout.footerTop());
        assertTrue(layout.groupRows() >= 1);
        assertTrue(layout.gridRows() >= 1);
        assertTrue(layout.memberRows() >= 1);
        assertColumnBounds(layout);
    }

    @Test
    void narrowEditorsHideOnlyTheOptionalPreview() {
        PaletteEditorLayout layout = PaletteEditorLayout.calculate(320, 180);

        assertEquals(0, layout.previewWidth());
        assertTrue(layout.browserWidth() >= 88);
        assertTrue(layout.memberWidth() >= 108);
        assertColumnBounds(layout);
    }

    private static void assertColumnBounds(PaletteEditorLayout layout) {
        assertTrue(layout.groupLeft() + layout.groupWidth() < layout.browserLeft());
        assertEquals(layout.gridColumns() * PaletteEditorLayout.GRID_CELL + 8, layout.browserWidth());
        assertTrue(layout.browserRight() <= layout.memberLeft());
        assertTrue(layout.memberRight() > layout.memberLeft());
        if (layout.previewWidth() > 0) {
            assertTrue(layout.previewLeft() >= layout.browserRight());
            assertTrue(layout.previewLeft() + layout.previewWidth() <= layout.memberLeft());
        }
    }
}
