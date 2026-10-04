package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import com.davidblackcn.lorianarchorbit.config.PaletteArrowStyle;
import com.davidblackcn.lorianarchorbit.interaction.PaletteArrowSprite;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class RadialWheelVisuals {
    public static final float ITEM_SCALE = 1.2F;
    // Non-palette HUDs retain the original bounds; palette layout reserves its own emphasis margin.
    public static final int ITEM_HALF_SIZE = 10;
    public static final int MINIMUM_RADIUS = com.davidblackcn.lorianarchorbit.interaction.PaletteRadialLayout.MINIMUM_RADIUS;

    private RadialWheelVisuals() {
    }

    public static void renderTargetArrow(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                         double radius, PaletteTargetPosition position, PaletteArrowStyle style,
                                         long nowMillis) {
        if (style == PaletteArrowStyle.NONE) return;
        var sprite = PaletteArrowSprite.of(style);
        // The larger sprite extends inward; delay it in tiny/opening rings to protect the name.
        if (radius < 48 + (sprite.pixelScale() - 1) * (sprite.rows().size() - 1)) return;
        var indicator = com.davidblackcn.lorianarchorbit.interaction.PaletteTargetIndicator
                .at(centerX, centerY, radius, position, nowMillis);
        if (indicator.isEmpty()) return;
        var arrow = indicator.get();
        // Anchor each tip, including the off-center fingertip, independently of wheel rotation.
        for (int row = 0; row < sprite.rows().size(); row++) {
            for (int column = 0; column < sprite.rows().get(row).length(); column++) {
                int color = sprite.color(column, row);
                if (color == 0) continue;
                for (int y = 0; y < sprite.pixelScale(); y++) for (int x = 0; x < sprite.pixelScale(); x++) {
                    var point = arrow.pixel((sprite.tipX() - column) * sprite.pixelScale() + x,
                            -row * sprite.pixelScale() - y);
                    graphics.fill(point.x(), point.y(), point.x() + 1, point.y() + 1, color);
                }
            }
        }
    }

    public static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int centerX, int centerY) {
        renderItem(graphics, stack, centerX, centerY, 1);
    }

    public static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int centerX, int centerY, float emphasis) {
        if (stack.isEmpty()) return;
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.scaleAround(ITEM_SCALE * emphasis, centerX, centerY);
            graphics.item(stack, centerX - 8, centerY - 8);
        } finally {
            pose.popMatrix();
        }
    }
}
