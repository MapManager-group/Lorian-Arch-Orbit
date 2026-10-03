package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.BooleanSupplier;

/** Flat controls retain vanilla input, sound, focus and narration without the stone button texture. */
final class WorkbenchFlatButton extends Button {
    private final int row;
    private final BooleanSupplier expanded;

    WorkbenchFlatButton(int x, int y, int width, Component label, OnPress press, int row, BooleanSupplier expanded) {
        super(x, y, width, 20, label, press, DEFAULT_NARRATION);
        this.row = row;
        this.expanded = expanded;
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean highlighted = active && isHoveredOrFocused();
        int background = row < 0 ? 0xEE111111 : (row % 2 == 0 ? 0xFF353535 : 0xFF292929);
        graphics.fill(x, y, x + w, y + h, highlighted && row >= 0 ? 0xFF666666 : background);
        if (row < 0) border(graphics, x, y, w, h, highlighted ? 0xFFFFFFFF : 0xFFAAAAAA);
        var font = Minecraft.getInstance().font;
        int textWidth = w - (expanded == null ? 12 : 24);
        String label = getMessage().getString();
        if (font.width(label) > textWidth) label = font.plainSubstrByWidth(label, Math.max(0, textWidth - font.width("…"))) + "…";
        graphics.text(font, label, x + 6, y + (h - font.lineHeight) / 2, active ? 0xFFEEEEEE : 0xFF999999);
        if (expanded != null) {
            boolean up = expanded.getAsBoolean();
            for (int line = 0; line < 4; line++) {
                int half = up ? line : 3 - line;
                graphics.fill(x + w - 10 - half, y + 8 + line, x + w - 9 + half, y + 9 + line, 0xFFCCCCCC);
            }
        }
    }

    static void border(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }
}
