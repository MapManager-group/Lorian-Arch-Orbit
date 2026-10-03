package com.davidblackcn.lorianarchorbit.client;

/** Shared bounds for controls, clipping, scrolling and hit testing. */
record HueGradientLayout(int left, int width, boolean compact, int settingsWidth, int bodyTop,
                         int bodyBottom, int resultLeft, int resultWidth, int footerTop) {
    static final int CELL = 24;
    static final int SETTINGS_HEIGHT = 302;
    static HueGradientLayout calculate(int width, int height) {
        int panel = Math.min(744, width - 24), left = (width - panel) / 2;
        boolean compact = width < 600 || height < 270;
        int settings = compact ? panel : 224;
        return new HueGradientLayout(left, panel, compact, settings, compact ? 76 : 52, height - 46,
                compact ? left : left + settings + 12, compact ? panel : panel - settings - 12, height - 26);
    }
    int previewTop() { return bodyTop + 24; }
    int columns() { return Math.max(1, resultWidth / CELL); }
    int rows() { return Math.max(1, (bodyBottom - previewTop()) / CELL); }
    int pageSize() { return columns() * rows(); }
    int settingsScrollMax() { return Math.max(0, SETTINGS_HEIGHT - (bodyBottom - bodyTop)); }
    int applyLeft() { return left + width - 64; }
    int backLeft() { return applyLeft() - 68; }
    int targetWidth() { return Math.min(148, width - 208); }
    int targetLeft() { return backLeft() - 4 - targetWidth(); }
    static boolean nodeOverflow(int width, int count) { return count > width / 28; }
    static int visibleNodes(int width, int count) { return Math.max(1, (width - (nodeOverflow(width, count) ? 40 : 0)) / 28); }
}
