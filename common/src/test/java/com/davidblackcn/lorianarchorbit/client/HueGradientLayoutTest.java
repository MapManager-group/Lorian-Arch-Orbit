package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HueGradientLayoutTest {
    @Test
    void previewsAndActionsRemainSeparateAcrossGuiScales() {
        for (int[] size : new int[][]{{320, 180}, {427, 240}, {512, 288}, {854, 480}, {1280, 720}}) {
            HueGradientLayout layout = HueGradientLayout.calculate(size[0], size[1]);
            assertTrue(layout.width() <= size[0] - 24);
            assertTrue(layout.columns() > 0 && layout.rows() > 0);
            assertTrue(layout.previewTop() + layout.rows() * HueGradientLayout.CELL <= layout.footerTop());
            PaletteEditorLayout editor = PaletteEditorLayout.calculate(size[0], size[1]);
            assertTrue(editor.memberTop() + editor.memberRows() * 18 <= editor.footerTop() - 14,
                    "Member rows should not overlap the status line at " + size[0]);
        }
    }
}
