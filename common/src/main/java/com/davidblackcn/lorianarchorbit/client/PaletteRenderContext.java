package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A scoped, extractor-specific override. Never changes Minecraft's window or GUI settings. */
public final class PaletteRenderContext implements AutoCloseable {
    private static final ThreadLocal<PaletteRenderContext> CURRENT = new ThreadLocal<>();
    private final PaletteRenderContext previous;
    private final GuiGraphicsExtractor graphics;
    private final PaletteViewport viewport;

    private PaletteRenderContext(GuiGraphicsExtractor graphics, PaletteViewport viewport) {
        this.previous = CURRENT.get();
        this.graphics = graphics;
        this.viewport = viewport;
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) viewport.offsetX(), (float) viewport.offsetY());
        graphics.pose().scale((float) viewport.scale());
        CURRENT.set(this);
    }

    public static PaletteRenderContext open(GuiGraphicsExtractor graphics, PaletteViewport viewport) {
        return new PaletteRenderContext(graphics, viewport);
    }

    public static PaletteViewport viewport(GuiGraphicsExtractor graphics) {
        PaletteRenderContext current = CURRENT.get();
        return current != null && current.graphics == graphics ? current.viewport : null;
    }

    @Override
    public void close() {
        try {
            graphics.pose().popMatrix();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
