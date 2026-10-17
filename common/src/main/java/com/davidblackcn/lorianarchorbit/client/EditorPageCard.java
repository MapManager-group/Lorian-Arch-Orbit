package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;

/** The whole translucent panel is a focusable, narrated page entry. */
final class EditorPageCard extends Button {
    private final EditorSession.Page page;

    EditorPageCard(EditorSession session, EditorSession.Page page, int x, int y, int width, int height) {
        super(x, y, width, height, EditorSession.text(page.name()), b -> session.show(page.id()), DEFAULT_NARRATION);
        this.page = page;
        setTooltip(Tooltip.create(EditorSession.text(page.description())));
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tick) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean highlighted = isHoveredOrFocused();
        graphics.fill(x, y, x + w, y + h, highlighted ? 0xC02B3035 : 0xA0161B20);
        WorkbenchFlatButton.border(graphics, x, y, w, h, highlighted ? 0xFFE0E0E0 : 0x707D858D);
        var font = Minecraft.getInstance().font;
        boolean roomy = h >= 160 && w >= 240;
        int titleTop = roomy ? 22 : 9;
        float scale = roomy ? 1.5F : 1;
        String label = getMessage().getString();
        int maximum = (int) ((w - 20) / scale);
        if (font.width(label) > maximum) label = font.plainSubstrByWidth(label, maximum - font.width("…")) + "…";
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x + w / 2.0F, y + titleTop);
            graphics.pose().scale(scale);
            graphics.centeredText(font, label, 0, 0, 0xFFF1F1F1);
        } finally { graphics.pose().popMatrix(); }
        int pictureTop = titleTop + (roomy ? 28 : 16);
        int pictureBottom = h - (roomy ? 50 : 8);
        graphics.enableScissor(x + 6, y + pictureTop, x + w - 6, y + Math.max(pictureTop, pictureBottom));
        try { page.illustration().render(graphics, x + 12, y + pictureTop, w - 24, pictureBottom - pictureTop); }
        finally { graphics.disableScissor(); }
        if (roomy) {
            var lines = font.split(EditorSession.text(page.description()), w - 40);
            for (int i = 0; i < Math.min(2, lines.size()); i++) graphics.centeredText(font, lines.get(i), x + w / 2,
                    y + h - 32 + i * (font.lineHeight + 2), 0xFFBEC3C8);
        }
    }
}
