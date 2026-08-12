package com.davidblackcn.lorianarchorbit.interaction;

public final class HudLayout {
    public static final int DEFAULT_MARGIN = 6;
    public static final int CROSSHAIR_GAP = 12;
    public static final int BOSS_BAR_BOTTOM_OFFSET = 30;
    public static final int HOTBAR_TOP_OFFSET = 22;
    public static final int HOTBAR_GAP = 8;
    private static final double RADIAL_RADIUS_PER_ADDITIONAL_ITEM = 1.8;

    private HudLayout() {
    }

    public static HudPoint crosshairText(
            int guiWidth,
            int guiHeight,
            int textWidth,
            int textHeight,
            int margin
    ) {
        validate(guiWidth, guiHeight, margin);
        int x = clamp((guiWidth - textWidth) / 2, margin, Math.max(margin, guiWidth - margin - textWidth));
        int desiredY = guiHeight / 2 + CROSSHAIR_GAP;
        int y = clamp(desiredY, margin, Math.max(margin, guiHeight - margin - textHeight));
        return new HudPoint(x, y);
    }

    public static HudPoint crosshairTextAbove(
            int guiWidth, int guiHeight, int textWidth, int textHeight, int margin
    ) {
        validate(guiWidth, guiHeight, margin);
        int x = clamp((guiWidth - textWidth) / 2, margin, Math.max(margin, guiWidth - margin - textWidth));
        int desiredY = guiHeight / 2 - CROSSHAIR_GAP - textHeight;
        int y = clamp(desiredY, margin, Math.max(margin, guiHeight - margin - textHeight));
        return new HudPoint(x, y);
    }

    public static HudPoint crosshairRadialCenter(int guiWidth, int guiHeight, int margin) {
        validate(guiWidth, guiHeight, margin);
        return new HudPoint(guiWidth / 2, guiHeight / 2);
    }

    public static int adaptiveRadialRadius(
            int guiWidth,
            int guiHeight,
            int itemCount,
            int minimumRadius,
            int itemHalfSize,
            int margin
    ) {
        validate(guiWidth, guiHeight, margin);
        if (itemCount < 0 || minimumRadius < 0 || itemHalfSize < 0) {
            throw new IllegalArgumentException("itemCount, minimumRadius and itemHalfSize must not be negative");
        }
        HudPoint center = crosshairRadialCenter(guiWidth, guiHeight, margin);
        int horizontal = Math.max(0, Math.min(
                center.x() - margin - itemHalfSize,
                guiWidth - margin - itemHalfSize - center.x()
        ));
        int vertical = Math.max(0, Math.min(
                center.y() - BOSS_BAR_BOTTOM_OFFSET - itemHalfSize,
                guiHeight - HOTBAR_TOP_OFFSET - HOTBAR_GAP - itemHalfSize - center.y()
        ));
        int preferredRadius = minimumRadius + (int) Math.round(
                Math.max(0, itemCount - 1) * RADIAL_RADIUS_PER_ADDITIONAL_ITEM
        );
        return Math.min(preferredRadius, Math.min(horizontal, vertical));
    }

    private static void validate(int guiWidth, int guiHeight, int margin) {
        if (guiWidth <= 0 || guiHeight <= 0) {
            throw new IllegalArgumentException("GUI dimensions must be positive");
        }
        if (margin < 0) {
            throw new IllegalArgumentException("margin must not be negative");
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
