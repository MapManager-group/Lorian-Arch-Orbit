package com.davidblackcn.lorianarchorbit.interaction;

/** Shared palette ring geometry, independent of Minecraft and of the active GUI scale. */
public record PaletteRadialLayout(int radius, int visibleCount) {
    public static final int ITEM_HALF_SIZE = 10;
    public static final int MINIMUM_RADIUS = 57;
    // A 16-unit item scaled by 1.2, plus a little breathing room.
    public static final double SLOT_SPACING = 21.2;
    // Reserve a multi-line top HUD (e.g. Jade) without depending on another mod's layout API.
    public static final int TOP_HUD_SAFE_AREA = 48;
    // 26.2 Hud.extractSelectedItemName starts at height - 59 (creative: -45), plus backdrop/gap.
    public static final int BOTTOM_HUD_SAFE_AREA = 64;

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
        double hudScale = Math.max(1, nativeGuiToCanvas);
        int top = (int) Math.ceil(TOP_HUD_SAFE_AREA * hudScale);
        int bottom = (int) Math.ceil(BOTTOM_HUD_SAFE_AREA * hudScale);
        int vertical = Math.min(height / 2 - top, height - bottom - height / 2) - ITEM_HALF_SIZE;
        return calculate(count, Math.max(0, Math.min(horizontal, vertical)));
    }

    public static PaletteRadialLayout preview(int count, int width, int height) {
        return calculate(count, Math.max(0, Math.min(width / 2 - ITEM_HALF_SIZE - 6,
                height / 2 - ITEM_HALF_SIZE - 18)));
    }
}
