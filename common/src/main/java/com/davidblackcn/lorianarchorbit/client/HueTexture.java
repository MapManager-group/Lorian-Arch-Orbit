package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/** Fetch each frame so resource reloads never leave a stale atlas sprite. */
final class HueTexture {
    private HueTexture() { }
    static boolean draw(GuiGraphicsExtractor graphics, String texture, int x, int y, int size) {
        var sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                Identifier.withDefaultNamespace("block/" + texture.substring(0, texture.length() - 4))));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size);
        return !sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
    }
}
