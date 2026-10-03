package com.davidblackcn.lorianarchorbit.client;

/** Picker cells match the wheel browser's 20-unit item grid. */
record HueBlockPickerLayout(int left, int width, int columns, int rows, int previewLeft, int previewWidth,
                            int footerTop) {
    static final int CELL = PaletteEditorLayout.GRID_CELL;
    static final int GRID_TOP = 72;
    static HueBlockPickerLayout calculate(int width, int height) {
        if (width < 320 || height < 180) throw new IllegalArgumentException("Picker requires a 320x180 canvas");
        int panel = Math.min(744, width - 24), left = (width - panel) / 2;
        boolean compact = width < 600 || height < 270;
        int columns = Math.max(1, (panel - (compact ? 8 : 220)) / CELL);
        int previewLeft = left + columns * CELL + 12;
        return new HueBlockPickerLayout(left, panel, columns, Math.max(1, (height - 48 - GRID_TOP) / CELL),
                previewLeft, compact ? 0 : left + panel - previewLeft, height - 26);
    }
    int gridWidth() { return columns * CELL; }
    int gridBottom() { return GRID_TOP + rows * CELL; }
    int visibleSlots() { return columns * rows; }
    int cellX(int slot) { return left + slot % columns * CELL; }
    int cellY(int slot) { return GRID_TOP + slot / columns * CELL; }
    int rowForAnchor(int index) { return Math.max(0, index) / columns; }
    int maxScrollRow(int count) { return Math.max(0, (count + columns - 1) / columns - rows); }
    int scrollbarLeft() { return left + gridWidth() + 2; }
    int thumbHeight(int count) {
        int totalRows = Math.max(1, (count + columns - 1) / columns);
        return Math.min(rows * CELL, Math.max(12, rows * CELL * rows / totalRows));
    }
    int thumbTop(int count, int row) {
        int maximum = maxScrollRow(count);
        return GRID_TOP + (maximum == 0 ? 0 : (rows * CELL - thumbHeight(count)) * row / maximum);
    }
    int rowAt(double mouseY, int grab, int count) {
        int travel = rows * CELL - thumbHeight(count);
        return travel <= 0 ? 0 : Math.clamp((int) Math.round((mouseY - GRID_TOP - grab) * maxScrollRow(count) / travel), 0, maxScrollRow(count));
    }
}
