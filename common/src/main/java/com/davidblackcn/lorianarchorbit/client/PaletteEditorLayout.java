package com.davidblackcn.lorianarchorbit.client;

/**
 * Calculates the palette editor's logical GUI bounds without depending on the
 * current window resolution.  Keeping this separate from the screen makes the
 * narrow-screen fallbacks deterministic and testable.
 */
final class PaletteEditorLayout {
    static final int OUTER_MARGIN = 12;
    static final int COLUMN_GAP = 8;
    static final int GRID_CELL = 20;
    static final int GRID_TOP = 78;
    private static final int MIN_GROUP_WIDTH = 84;
    private static final int MAX_GROUP_WIDTH = 125;
    private static final int MIN_MEMBER_WIDTH = 108;
    private static final int MAX_MEMBER_WIDTH = 280;
    private static final int MIN_BROWSER_WIDTH = GRID_CELL * 4 + 8;
    private static final int MIN_PREVIEW_WIDTH = 100;
    private static final int NORMAL_FOOTER_WIDTH = 720;
    private static final int WIDE_GROUP_BROWSER_GAP = 20;
    private static final int[] FOOTER_BUTTON_WIDTHS = {74, 54, 54, 54, 64, 54, 54, 54, 74, 78};

    private final int groupLeft;
    private final int groupWidth;
    private final int browserLeft;
    private final int browserWidth;
    private final int previewLeft;
    private final int previewWidth;
    private final int memberLeft;
    private final int memberWidth;
    private final int gridColumns;
    private final int footerTop;
    private final int contentBottom;
    private final boolean compactFooter;

    private PaletteEditorLayout(
            int groupLeft,
            int groupWidth,
            int browserLeft,
            int browserWidth,
            int previewLeft,
            int previewWidth,
            int memberLeft,
            int memberWidth,
            int gridColumns,
            int footerTop,
            int contentBottom,
            boolean compactFooter
    ) {
        this.groupLeft = groupLeft;
        this.groupWidth = groupWidth;
        this.browserLeft = browserLeft;
        this.browserWidth = browserWidth;
        this.previewLeft = previewLeft;
        this.previewWidth = previewWidth;
        this.memberLeft = memberLeft;
        this.memberWidth = memberWidth;
        this.gridColumns = gridColumns;
        this.footerTop = footerTop;
        this.contentBottom = contentBottom;
        this.compactFooter = compactFooter;
    }

    static PaletteEditorLayout calculate(int screenWidth, int screenHeight) {
        if (screenWidth < 320 || screenHeight < 180) {
            throw new IllegalArgumentException("palette editor requires at least a 320x180 logical GUI");
        }
        boolean compactFooter = screenWidth < NORMAL_FOOTER_WIDTH;
        int footerRows = compactFooter ? compactFooterRows(screenWidth) : 1;
        int footerTop = screenHeight - 6 - footerRows * 20 - (footerRows - 1) * 4;
        int contentBottom = footerTop - 20;

        int groupLeft = OUTER_MARGIN;
        int groupWidth = clamp(screenWidth / 5, MIN_GROUP_WIDTH, MAX_GROUP_WIDTH);
        int groupBrowserGap = screenWidth >= 480 ? WIDE_GROUP_BROWSER_GAP : COLUMN_GAP;
        int browserLeft = groupLeft + groupWidth + groupBrowserGap;
        int desiredMemberWidth = clamp(screenWidth / 4, MIN_MEMBER_WIDTH, MAX_MEMBER_WIDTH);
        int maximumMemberWidth = screenWidth - OUTER_MARGIN - browserLeft - COLUMN_GAP - MIN_BROWSER_WIDTH;
        int memberWidth = Math.min(desiredMemberWidth, Math.max(MIN_MEMBER_WIDTH, maximumMemberWidth));
        int memberLeft = screenWidth - OUTER_MARGIN - memberWidth;
        int available = memberLeft - browserLeft - COLUMN_GAP;

        int preferredBrowserWidth;
        int previewWidth;
        if (available >= MIN_BROWSER_WIDTH + COLUMN_GAP + MIN_PREVIEW_WIDTH) {
            preferredBrowserWidth = clamp(available * 45 / 100, MIN_BROWSER_WIDTH,
                    Math.min(204, available - COLUMN_GAP - MIN_PREVIEW_WIDTH));
        } else {
            preferredBrowserWidth = available;
        }
        int gridColumns = Math.max(1, Math.min(9, (preferredBrowserWidth - 8) / GRID_CELL));
        int browserWidth = gridColumns * GRID_CELL + 8;
        previewWidth = available >= MIN_BROWSER_WIDTH + COLUMN_GAP + MIN_PREVIEW_WIDTH
                ? available - browserWidth - COLUMN_GAP
                : 0;
        int previewLeft = browserLeft + browserWidth + COLUMN_GAP;
        return new PaletteEditorLayout(
                groupLeft, groupWidth, browserLeft, browserWidth, previewLeft, previewWidth,
                memberLeft, memberWidth, gridColumns, footerTop, contentBottom, compactFooter
        );
    }

    private static int compactFooterRows(int screenWidth) {
        int rows = 1;
        int x = OUTER_MARGIN;
        for (int buttonWidth : FOOTER_BUTTON_WIDTHS) {
            if (x + buttonWidth > screenWidth - OUTER_MARGIN) {
                rows++;
                x = OUTER_MARGIN;
            }
            x += buttonWidth + 4;
        }
        return rows;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    int groupLeft() { return groupLeft; }
    int groupWidth() { return groupWidth; }
    int browserLeft() { return browserLeft; }
    int browserWidth() { return browserWidth; }
    int previewLeft() { return previewLeft; }
    int previewWidth() { return previewWidth; }
    int memberLeft() { return memberLeft; }
    int memberWidth() { return memberWidth; }
    int gridColumns() { return gridColumns; }
    int footerTop() { return footerTop; }
    int contentBottom() { return contentBottom; }
    boolean compactFooter() { return compactFooter; }

    int browserRight() { return browserLeft + browserWidth; }
    int memberRight() { return memberLeft + memberWidth; }
    int visibleTabs() { return Math.max(1, (browserWidth - 40) / 20); }
    int gridRows() { return Math.max(1, (contentBottom - GRID_TOP) / GRID_CELL); }
    int groupRows() { return Math.max(1, (contentBottom - 50) / 18); }
    int memberRows() { return Math.max(1, (contentBottom - 94) / 18); }
}
