package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteViewportTest {
    private static final int[][] WINDOWS = {{854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440},
            {3840, 2036}, {3840, 2160}, {3440, 1440}, {5120, 1440}, {1024, 768}, {480, 270}, {320, 180}};

    @Test
    void canvasAndPixelPositionsAreIndependentOfVanillaGuiScale() {
        for (int[] size : WINDOWS) {
            PaletteViewport baseline = PaletteViewport.calculate(size[0], size[1], 1);
            // Includes both requested GUI scales and the maximum automatic scale.
            int auto = Math.max(1, Math.min(size[0] / 320, size[1] / 240));
            for (int scale : new int[]{1, 2, 3, 5, auto}) {
                PaletteViewport view = PaletteViewport.calculate(size[0], size[1], scale);
                assertEquals(baseline.width(), view.width());
                assertEquals(baseline.height(), view.height());
                assertTrue(view.width() >= 320 && view.width() <= 768);
                assertTrue(view.height() >= 180 && view.height() <= 408);
                assertTrue(view.offsetX() >= -1e-8 && view.offsetY() >= -1e-8);
                assertTrue(view.guiX(view.width()) * scale <= size[0] + 1e-8);
                assertTrue(view.guiY(view.height()) * scale <= size[1] + 1e-8);
                for (double x : new double[]{0, 12, 157.5, view.width() - 12}) {
                    assertEquals(x, view.localX(view.guiX(x)), 1e-8);
                    assertEquals(baseline.guiX(x), view.guiX(x) * scale, 1e-8);
                }
                for (double y : new double[]{0, 28, 78.5, view.height() - 6}) {
                    assertEquals(y, view.localY(view.guiY(y)), 1e-8);
                    assertEquals(baseline.guiY(y), view.guiY(y) * scale, 1e-8);
                }
                assertFalse(view.contains(view.guiX(-0.1), view.guiY(40)));
                assertFalse(view.contains(view.guiX(40), view.guiY(view.height() + 0.1)));
                assertTrue(view.contains(view.guiX(40), view.guiY(40)));
            }
        }
    }

    @Test
    void nativeMouseRoundingMatchesTheExactRenderProjection() {
        for (int[] size : WINDOWS) {
            for (int guiScale : new int[]{1, 2, 3, 5, 9}) {
                var view = PaletteViewport.calculate(size[0], size[1], guiScale);
                double nativeWidth = Math.ceil((double) size[0] / guiScale);
                double nativeHeight = Math.ceil((double) size[1] / guiScale);
                double mouseX = view.guiX(123.25) * guiScale / size[0] * nativeWidth;
                double mouseY = view.guiY(87.5) * guiScale / size[1] * nativeHeight;
                assertEquals(123.25, view.mouseX(mouseX), 1e-8);
                assertEquals(87.5, view.mouseY(mouseY), 1e-8);
                assertTrue(view.containsMouse(mouseX, mouseY));
                assertEquals(view.mouseX(mouseX + 3) - view.mouseX(mouseX), view.mouseDeltaX(3), 1e-8);
                assertEquals(view.mouseY(mouseY + 3) - view.mouseY(mouseY), view.mouseDeltaY(3), 1e-8);
            }
        }
    }

    @Test
    void referenceAndReadableFloorArePreserved() {
        PaletteViewport reference = PaletteViewport.calculate(3840, 2036, 5);
        assertEquals(768, reference.width());
        assertEquals(408, reference.height());
        assertEquals(5, reference.pixelScale(), 0.02);
        assertEquals(1.5, PaletteViewport.calculate(854, 480, 3).pixelScale());
        assertEquals(1, PaletteViewport.calculate(320, 180, 1).pixelScale());
        assertThrows(IllegalArgumentException.class, () -> PaletteViewport.calculate(0, 100, 1));
        assertThrows(IllegalArgumentException.class, () -> PaletteViewport.calculate(100, 100, Double.NaN));
    }
}
