package com.davidblackcn.lorianarchorbit.client;

/** Shared bounds for rendering, pointer hit testing and pagination. */
record HueGradientLayout(int left, int width, int previewTop, int columns, int rows, int footerTop) {
    static final int CELL = 24;
    static HueGradientLayout calculate(int width, int height) {
        int panelWidth = Math.min(760, width - 24);
        int footerTop = height - 24;
        return new HueGradientLayout((width - panelWidth) / 2, panelWidth, 130,
                Math.max(1, panelWidth / CELL), Math.max(1, (footerTop - 134) / CELL), footerTop);
    }
    int pageSize() { return columns * rows; }
}
