package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class RadialWheelVisuals {
    public static final float ITEM_SCALE = 1.5F;
    public static final int ITEM_HALF_SIZE = 15;
    public static final int MINIMUM_RADIUS = 57;

    private RadialWheelVisuals() {
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
