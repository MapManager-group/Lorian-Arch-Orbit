package com.davidblackcn.lorianarchorbit.client;

/** Popup bounds shared by input, rendering and keyboard scrolling. */
record WorkbenchMenuLayout(int x, int y, int width, int rows) {
    static final int ROW_HEIGHT = 20;
    static int preferredWidth(int longestLabel, boolean scrollable) {
        return Math.clamp(longestLabel + 16 + (scrollable ? 8 : 0), 60, 240);
    }
    static WorkbenchMenuLayout calculate(int width, int height, int x, int y, int preferred, int count) {
        int w = Math.min(preferred, width - 16);
        int rows = Math.max(1, Math.min(count, (height - 18) / ROW_HEIGHT));
        return new WorkbenchMenuLayout(Math.clamp(x, 8, width - w - 8),
                Math.clamp(y, 8, height - rows * ROW_HEIGHT - 10), w, rows);
    }
    static WorkbenchMenuLayout anchored(int width, int height, int x, int y, int anchorWidth, int anchorHeight,
                                         int count) {
        int below = Math.max(0, height - 8 - y - anchorHeight);
        int above = Math.max(0, y - 8);
        boolean down = below >= count * ROW_HEIGHT + 2 || below >= above;
        int rows = Math.max(1, Math.min(count, ((down ? below : above) - 2) / ROW_HEIGHT));
        return calculate(width, height, x, down ? y + anchorHeight : y - rows * ROW_HEIGHT - 2, anchorWidth, rows);
    }
    int height() { return rows * ROW_HEIGHT + 2; }
    int thumbHeight(int count) { return Math.max(10, (height() - 2) * rows / count); }
    int thumbY(int start, int count) { return y + 1 + (count <= rows ? 0 : (height() - 2 - thumbHeight(count)) * start / (count - rows)); }
    int scrollAt(double mouseY, int grab, int count) {
        int travel = height() - 2 - thumbHeight(count);
        return travel <= 0 ? 0 : Math.clamp((int) Math.round((mouseY - y - 1 - grab) * (count - rows) / travel), 0, count - rows);
    }
}
