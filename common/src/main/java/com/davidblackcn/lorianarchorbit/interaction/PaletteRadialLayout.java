package com.davidblackcn.lorianarchorbit.interaction;

/** Shared palette ring geometry, independent of Minecraft and of the active GUI scale. */
public record PaletteRadialLayout(int radius, int visibleCount) {
    public static final int ITEM_HALF_SIZE = 10;
    public static final int MINIMUM_RADIUS = 57;
    // A 16-unit item scaled by 1.2, plus a little breathing room.
    public static final double SLOT_SPACING = 21.2;

    public static PaletteRadialLayout calculate(int count, int maximumRadius) {
        if (count < 0 || maximumRadius < 0) throw new IllegalArgumentException("negative ring dimensions");
        int preferred = MINIMUM_RADIUS + (int) Math.round(Math.max(0, count - 1) * 1.8);
        int radius = Math.min(preferred, maximumRadius);
        int capacity = radius < SLOT_SPACING / 2 ? 1
                : Math.max(1, (int) Math.floor(Math.PI / Math.asin(SLOT_SPACING / (2 * radius))));
        return new PaletteRadialLayout(radius, Math.min(count, capacity));
    }

    public static PaletteRadialLayout hud(int count, int width, int height, double nativeGuiToCanvas) {
        int horizontal = width / 2 - ITEM_HALF_SIZE - HudLayout.DEFAULT_MARGIN;
        int vertical = height / 2 - ITEM_HALF_SIZE - (int) Math.ceil(Math.max(
                HudLayout.BOSS_BAR_BOTTOM_OFFSET,
                (HudLayout.HOTBAR_TOP_OFFSET + HudLayout.HOTBAR_GAP) * nativeGuiToCanvas));
        return calculate(count, Math.max(0, Math.min(horizontal, vertical)));
    }

    public static PaletteRadialLayout preview(int count, int width, int height) {
        return calculate(count, Math.max(0, Math.min(width / 2 - ITEM_HALF_SIZE - 6,
                height / 2 - ITEM_HALF_SIZE - 18)));
    }
}
