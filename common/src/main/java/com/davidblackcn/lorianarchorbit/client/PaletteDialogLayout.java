package com.davidblackcn.lorianarchorbit.client;

import java.util.ArrayList;
import java.util.List;

/** Shared action wrapping and content bounds for import/export dialogs. */
record PaletteDialogLayout(int left, int width, int footerTop, List<Action> actions) {
    record Action(int x, int y, int width) { }

    static PaletteDialogLayout calculate(int screenWidth, int screenHeight, int preferredWidth, int... widths) {
        int panel = Math.min(preferredWidth, screenWidth - 40);
        int left = (screenWidth - panel) / 2;
        List<Action> relative = new ArrayList<>();
        int x = 0;
        int row = 0;
        for (int preferred : widths) {
            int buttonWidth = Math.min(preferred, panel);
            if (x > 0 && x + buttonWidth > panel) { x = 0; row++; }
            relative.add(new Action(left + x, row * 24, buttonWidth));
            x += buttonWidth + 4;
        }
        int top = screenHeight - 28 - row * 24;
        return new PaletteDialogLayout(left, panel, top,
                relative.stream().map(a -> new Action(a.x(), a.y() + top, a.width())).toList());
    }

    int rows(int top, int rowHeight) { return Math.max(0, (footerTop - 30 - top) / rowHeight); }
}
