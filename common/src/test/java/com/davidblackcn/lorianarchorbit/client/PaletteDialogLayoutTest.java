package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteDialogLayoutTest {
    @Test
    void dialogsKeepActionsStatusAndAtLeastOneRowSeparate() {
        for (int width : new int[]{320, 427, 569, 600, 768}) {
            for (int height : new int[]{180, 240, 320, 408}) {
                assertLayout(PaletteDialogLayout.calculate(width, height, 520, 70, 70, 104, 104, 80), width, height, 78, 20);
                assertLayout(PaletteDialogLayout.calculate(width, height, 560, 82, 150, 82, 84), width, height, 70, 24);
            }
        }
    }

    private static void assertLayout(PaletteDialogLayout layout, int width, int height, int top, int rowHeight) {
        assertTrue(layout.rows(top, rowHeight) >= 1);
        assertTrue(top + layout.rows(top, rowHeight) * rowHeight <= layout.footerTop() - 30);
        for (int i = 0; i < layout.actions().size(); i++) {
            var a = layout.actions().get(i);
            assertTrue(a.x() >= 0 && a.x() + a.width() <= width);
            assertTrue(a.y() >= layout.footerTop() && a.y() + 20 <= height);
            for (int j = i + 1; j < layout.actions().size(); j++) {
                var b = layout.actions().get(j);
                assertTrue(a.x() + a.width() <= b.x() || b.x() + b.width() <= a.x()
                        || a.y() + 20 <= b.y() || b.y() + 20 <= a.y());
            }
        }
    }
}
