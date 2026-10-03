package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import com.davidblackcn.lorianarchorbit.config.PaletteArrowStyle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class RadialWheelVisuals {
    // Upright source sprites. Anchor the fingertip/arrow tip, not the bounding-box center.
    private static final ArrowSprite POINTER_ARROW = new ArrowSprite(3, new String[]{
            "..OOO....",
            "..OHO....",
            "..OHO....",
            "..OHO....",
            "..OHHOO..",
            ".OOHGHHO.",
            "OSGHGGGO.",
            "OSGGSSSO.",
            ".OSSSSSO.",
            "..OOOOO.."
    });
    private static final ArrowSprite SHIFT_ARROW = new ArrowSprite(4, new String[]{
            "....O....",
            "...OHO...",
            "..OHHHO..",
            ".OHGGGHO.",
            "OHGGGGGHO",
            "OHGGGGGHO",
            ".OGGGGGO.",
            ".OSSSSSO.",
            "..OOOOO.."
    });

    private record ArrowSprite(int tipX, String[] rows) { }
    public static final float ITEM_SCALE = 1.2F;
    public static final int ITEM_HALF_SIZE = com.davidblackcn.lorianarchorbit.interaction.PaletteRadialLayout.ITEM_HALF_SIZE;
    public static final int MINIMUM_RADIUS = com.davidblackcn.lorianarchorbit.interaction.PaletteRadialLayout.MINIMUM_RADIUS;

    private RadialWheelVisuals() {
    }

    public static void renderTargetArrow(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                         double radius, PaletteTargetPosition position, PaletteArrowStyle style,
                                         long nowMillis) {
        var indicator = com.davidblackcn.lorianarchorbit.interaction.PaletteTargetIndicator
                .at(centerX, centerY, radius, position, nowMillis);
        if (indicator.isEmpty()) return;
        var arrow = indicator.get();
        ArrowSprite sprite = switch (style) {
            case POINTER -> POINTER_ARROW;
            case SHIFT -> SHIFT_ARROW;
        };
        // Forest-green body and restrained bevel; each opaque pixel is drawn exactly once.
        for (int row = 0; row < sprite.rows().length; row++) {
            for (int column = 0; column < sprite.rows()[row].length(); column++) {
                int color = switch (sprite.rows()[row].charAt(column)) {
                    case 'O' -> 0xFF0B2618;
                    case 'H' -> 0xFF3D8955;
                    case 'G' -> 0xFF17633A;
                    case 'S' -> 0xFF104729;
                    default -> 0;
                };
                if (color == 0) continue;
                var point = arrow.pixel(sprite.tipX() - column, -row);
                graphics.fill(point.x(), point.y(), point.x() + 1, point.y() + 1, color);
            }
        }
    }

    public static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int centerX, int centerY) {
        if (stack.isEmpty()) {
            return;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.scaleAround(ITEM_SCALE, centerX, centerY);
            graphics.item(stack, centerX - 8, centerY - 8);
        } finally {
            pose.popMatrix();
        }
    }
}
