package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Draw the swatch in the same render pass as its background so it cannot be covered by the button skin. */
final class GradientNodeButton extends Button {
    private final GradientWorkbench.Node node;
    private final boolean selected;

    GradientNodeButton(int x, int y, int index, GradientWorkbench.Node node, boolean selected, OnPress onPress) {
        super(x, y, 26, 24, Component.literal(Integer.toString(index + 1)), onPress, DEFAULT_NARRATION);
        this.node = node;
        this.selected = selected;
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        int x = getX(), y = getY();
        graphics.fill(x, y, x + width, y + height, isHoveredOrFocused() ? 0xAA4D6A7D : 0x66202020);
        var font = Minecraft.getInstance().font;
        if (node.pinned != null) {
            HueTexture.draw(graphics, node.pinned.block().texture(), x + 5, y + 2, 16);
            graphics.text(font, getMessage(), x + 1, y + 1, 0xFFFFFFFF);
        } else graphics.centeredText(font, getMessage(), x + width / 2, y + 6, 0xFFFFFFFF);
        if (selected || isFocused()) WorkbenchFlatButton.border(graphics, x, y, width, height,
                isFocused() ? 0xFFFFFFFF : 0xFF94B9CD);
        int rgb;
        try { rgb = Integer.parseInt(node.hex.replace("#", ""), 16); }
        catch (NumberFormatException ex) { rgb = 0; }
        graphics.fill(x + 2, y + height - 4, x + width - 2, y + height - 1, 0xFF000000 | rgb);
    }
}
