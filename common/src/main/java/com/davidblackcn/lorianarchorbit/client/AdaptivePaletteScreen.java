package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Native widgets live in canvas coordinates; only this boundary converts input and rendering. */
public abstract class AdaptivePaletteScreen extends Screen {
    private PaletteViewport viewport;

    protected AdaptivePaletteScreen(Component title) { super(title); }

    @Override
    protected final void init() {
        var window = minecraft.getWindow();
        PaletteViewport next = PaletteViewport.calculate(Math.max(1, window.getWidth()),
                Math.max(1, window.getHeight()), window.getGuiScale());
        if (viewport != null && !viewport.equals(next)) onViewportChanged(viewport, next);
        viewport = next;
        width = next.width();
        height = next.height();
        setDragging(false);
        initContent();
    }

    protected abstract void initContent();
    protected void onViewportChanged(PaletteViewport previous, PaletteViewport next) { }

    @Override
    protected void repositionElements() {
        var focused = getFocused();
        Component message = focused instanceof AbstractWidget widget ? widget.getMessage() : null;
        int cursor = focused instanceof EditBox box ? box.getCursorPosition() : -1;
        rebuildWidgets();
        if (message != null) {
            for (var child : children()) {
                if (child instanceof AbstractWidget widget && child.getClass() == focused.getClass()
                        && widget.visible && widget.active && widget.getMessage().equals(message)) {
                    setInitialFocus(widget);
                    if (widget instanceof EditBox box && cursor >= 0) box.setCursorPosition(cursor);
                    break;
                }
            }
        }
    }

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        try (var ignored = PaletteRenderContext.open(graphics, viewport)) {
            graphics.enableScissor(0, 0, width, height);
            try {
                renderContent(graphics, (int) Math.floor(viewport.mouseX(mouseX)),
                        (int) Math.floor(viewport.mouseY(mouseY)), partialTick);
            } finally {
                graphics.disableScissor();
            }
        }
    }

    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    /** Called once by Screen's deferred-render bridge, after ordinary page rendering. */
    public final void extractPaletteDeferred(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick,
                                             Renderable deferred) {
        try (var ignored = PaletteRenderContext.open(graphics, viewport)) {
            graphics.enableScissor(0, 0, width, height);
            try {
                deferred.extractRenderState(graphics, (int) Math.floor(viewport.mouseX(mouseX)),
                        (int) Math.floor(viewport.mouseY(mouseY)), partialTick);
            } finally {
                graphics.disableScissor();
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int canvasWidth = width;
        int canvasHeight = height;
        try {
            width = graphics.guiWidth();
            height = graphics.guiHeight();
            super.extractBackground(graphics, mouseX, mouseY, partialTick);
        } finally {
            width = canvasWidth;
            height = canvasHeight;
        }
    }

    private MouseButtonEvent localEvent(MouseButtonEvent event) {
        return new MouseButtonEvent(viewport.mouseX(event.x()), viewport.mouseY(event.y()), event.buttonInfo());
    }

    @Override
    public final boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return viewport.containsMouse(event.x(), event.y())
                ? mouseClickedContent(localEvent(event), doubleClick) : mouseClickedOutsideCanvas();
    }

    protected boolean mouseClickedOutsideCanvas() { return false; }

    protected boolean mouseClickedContent(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public final boolean mouseReleased(MouseButtonEvent event) {
        // Release outside the canvas still ends an active drag.
        return mouseReleasedContent(localEvent(event));
    }

    protected boolean mouseReleasedContent(MouseButtonEvent event) { return super.mouseReleased(event); }

    @Override
    public final boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return super.mouseDragged(localEvent(event), viewport.mouseDeltaX(deltaX), viewport.mouseDeltaY(deltaY));
    }

    @Override
    public final void mouseMoved(double x, double y) { super.mouseMoved(viewport.mouseX(x), viewport.mouseY(y)); }

    @Override
    public final boolean mouseScrolled(double x, double y, double amountX, double amountY) {
        return viewport.containsMouse(x, y) && mouseScrolledContent(viewport.mouseX(x), viewport.mouseY(y), amountX, amountY);
    }

    protected boolean mouseScrolledContent(double x, double y, double amountX, double amountY) {
        return super.mouseScrolled(x, y, amountX, amountY);
    }

    protected final void boundedText(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth,
                                     int color, int mouseX, int mouseY) {
        String value = text.getString();
        boolean truncated = font.width(value) > maxWidth;
        String shown = truncated ? font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("…"))) + "…" : value;
        graphics.text(font, shown, x, y, color);
        if (truncated && mouseX >= x && mouseX < x + maxWidth && mouseY >= y && mouseY < y + font.lineHeight) {
            graphics.setTooltipForNextFrame(font, font.split(text, Math.max(40, width - 24)), mouseX, mouseY);
        }
    }
}
