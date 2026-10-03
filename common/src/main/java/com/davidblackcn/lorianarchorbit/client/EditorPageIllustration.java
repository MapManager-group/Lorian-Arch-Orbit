package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Small game-rendered illustrations, independent of a draft, world or downloaded palette data. */
enum EditorPageIllustration {
    WHEEL, GRADIENT;

    private static final String[] WHEEL_BLOCKS = {
            "red_concrete", "orange_concrete", "yellow_concrete", "lime_concrete", "green_concrete", "cyan_concrete",
            "light_blue_concrete", "blue_concrete", "purple_concrete", "magenta_concrete", "pink_concrete", "white_concrete"
    };
    private static final String[] GRADIENT_BLOCKS = {
            "white_concrete", "white_terracotta", "terracotta", "orange_terracotta",
            "red_terracotta", "brown_terracotta", "gray_terracotta", "black_terracotta"
    };

    void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) return;
        if (this == WHEEL) {
            int diameter = Math.min(width, height);
            float icon = Math.min(24, diameter * 0.12F);
            int radius = Math.max(1, (int) ((diameter - icon * 1.6F) / 2));
            for (int i = 0; i < WHEEL_BLOCKS.length; i++) {
                double angle = Math.PI / 2 + i * Math.PI * 2 / WHEEL_BLOCKS.length;
                item(graphics, WHEEL_BLOCKS[i], x + width / 2 + (int) Math.round(Math.cos(angle) * radius),
                        y + height / 2 + (int) Math.round(Math.sin(angle) * radius), icon);
            }
            item(graphics, "bricks", x + width / 2, y + height / 2, Math.min(38, diameter * 0.23F));
        } else {
            int cell = Math.max(1, Math.min(30, Math.min(width / GRADIENT_BLOCKS.length, height / 5)));
            int left = x + (width - cell * GRADIENT_BLOCKS.length) / 2, top = y + (height - cell * 4) / 2;
            for (int i = 0; i < GRADIENT_BLOCKS.length; i++) {
                var sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                        Identifier.withDefaultNamespace("block/" + GRADIENT_BLOCKS[i])));
                for (int row = 0; row < 4; row++) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite,
                        left + i * cell, top + row * cell, cell, cell);
            }
            WorkbenchFlatButton.border(graphics, left - 1, top - 1, cell * GRADIENT_BLOCKS.length + 2, cell * 4 + 2, 0x88777777);
        }
    }

    private static void item(GuiGraphicsExtractor graphics, String id, int centerX, int centerY, float size) {
        var stack = BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(id)).orElseThrow().getDefaultInstance();
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(centerX, centerY);
            graphics.pose().scale(size / 16);
            graphics.item(stack, -8, -8);
        } finally { graphics.pose().popMatrix(); }
    }
}
