package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Same item-grid language as the wheel browser, with the matched texture visible beside it. */
final class HueBlockPickerScreen extends AdaptivePaletteScreen {
    private final Screen parent;
    private final List<HueGradient.Candidate> source;
    private final Consumer<HueGradient.Candidate> picked;
    private List<HueGradient.Candidate> filtered = List.of();
    private final List<HueBlockPickerCell> resultButtons = new ArrayList<>();
    private EditBox search;
    private String query = "";
    private int scrollRow, scrollbarGrab;
    private boolean scrollbarDragging;
    private HueGradient.Candidate preview;
    private boolean keyboardPreview;
    private int lastPointerX = Integer.MIN_VALUE, lastPointerY = Integer.MIN_VALUE;

    HueBlockPickerScreen(Screen parent, List<HueGradient.Candidate> source, Consumer<HueGradient.Candidate> picked) {
        super(HueGradientScreen.text("pick_block"));
        this.parent = parent;
        this.source = List.copyOf(source);
        this.picked = picked;
    }

    private HueBlockPickerLayout layout() { return HueBlockPickerLayout.calculate(width, height); }

    @Override protected void onViewportChanged(PaletteViewport previous, PaletteViewport next) {
        int anchor = scrollRow * HueBlockPickerLayout.calculate(previous.width(), previous.height()).columns();
        scrollRow = HueBlockPickerLayout.calculate(next.width(), next.height()).rowForAnchor(anchor);
    }

    @Override protected void initContent() {
        resultButtons.clear();
        scrollbarDragging = false;
        var layout = layout();
        search = new EditBox(font, layout.left(), 28, layout.gridWidth() - 48, 20, HueGradientScreen.text("search"));
        search.setHint(HueGradientScreen.text("search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; scrollRow = 0; updateResults(); });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(HueGradientScreen.text("clear"), b -> { search.setValue(""); setInitialFocus(search); })
                .bounds(layout.left() + layout.gridWidth() - 44, 28, 44, 20).build());
        addRenderableWidget(Button.builder(HueGradientScreen.text("back"), b -> onClose())
                .bounds(layout.left() + layout.width() - 64, layout.footerTop(), 64, 20).build());
        updateResults();
        setInitialFocus(search);
    }

    private void scrollTo(int row) {
        int next = Math.clamp(row, 0, layout().maxScrollRow(filtered.size()));
        if (next == scrollRow) return;
        int focusedSlot = resultButtons.indexOf(getFocused());
        scrollRow = next;
        updateGrid();
        if (focusedSlot >= 0 && !resultButtons.isEmpty()) setInitialFocus(resultButtons.get(Math.min(focusedSlot, resultButtons.size() - 1)));
    }

    private void updateResults() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        // Keep source order (including distance-sorted alternatives) and distinct matching textures.
        filtered = source.stream().filter(c -> c.itemId().contains(needle)
                || c.block().texture().toLowerCase(Locale.ROOT).contains(needle)
                || HueBlocksRuntime.stack(c).getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)).toList();
        scrollRow = Math.min(scrollRow, layout().maxScrollRow(filtered.size()));
        updateGrid();
    }

    private void updateGrid() {
        if (getFocused() instanceof HueBlockPickerCell) setFocused(null);
        resultButtons.forEach(this::removeWidget);
        resultButtons.clear();
        var layout = layout();
        int start = scrollRow * layout.columns();
        for (int slot = 0; slot < layout.visibleSlots() && start + slot < filtered.size(); slot++) {
            HueGradient.Candidate candidate = filtered.get(start + slot);
            var button = new HueBlockPickerCell(candidate, layout.cellX(slot), layout.cellY(slot), b -> {
                picked.accept(candidate);
                minecraft.setScreenAndShow(parent);
            });
            resultButtons.add(button);
            addRenderableWidget(button);
        }
        preview = resultButtons.isEmpty() ? null : resultButtons.getFirst().candidate;
    }

    @Override protected void renderContent(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        if (lastPointerX != Integer.MIN_VALUE && (mx != lastPointerX || my != lastPointerY)) keyboardPreview = false;
        lastPointerX = mx; lastPointerY = my;
        var layout = layout();
        if (scrollbarDragging) scrollTo(layout.rowAt(my, scrollbarGrab, filtered.size()));
        graphics.fill(layout.left(), HueBlockPickerLayout.GRID_TOP, layout.left() + layout.gridWidth(), layout.gridBottom(), 0x55202020);
        int track = layout.scrollbarLeft();
        graphics.fill(track, HueBlockPickerLayout.GRID_TOP, track + 6, layout.gridBottom(), 0x55202020);
        if (layout.maxScrollRow(filtered.size()) > 0) {
            int top = layout.thumbTop(filtered.size(), scrollRow);
            graphics.fill(track, top, track + 6, top + layout.thumbHeight(filtered.size()), 0xFFAAAAAA);
        }
        if (layout.previewWidth() > 0) {
            graphics.fill(layout.previewLeft(), 28, layout.previewLeft() + layout.previewWidth(), layout.gridBottom(), 0x55202020);
            WorkbenchFlatButton.border(graphics, layout.previewLeft(), 28, layout.previewWidth(), layout.gridBottom() - 28, 0x555F5F5F);
        }
        super.renderContent(graphics, mx, my, tick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        boundedText(graphics, HueGradientScreen.text("picker_results", filtered.size()), layout.left(), 56,
                layout.gridWidth(), 0xFFBBBBBB, mx, my);
        if (filtered.isEmpty()) boundedText(graphics, HueGradientScreen.text("picker_empty"), layout.left() + 4,
                HueBlockPickerLayout.GRID_TOP + 8, layout.gridWidth() - 8, 0xFFFFC14D, mx, my);

        if (keyboardPreview && getFocused() instanceof HueBlockPickerCell cell) preview = cell.candidate;
        else for (var button : resultButtons) if (button.isHovered()) preview = button.candidate;
        if (layout.previewWidth() > 0) renderPreview(graphics, layout, mx, my);
        boundedText(graphics, HueGradientScreen.text("picker_choose_hint"), layout.left(), layout.footerTop() - 14,
                layout.width(), 0xFFBBBBBB, mx, my);
    }

    private void renderPreview(GuiGraphicsExtractor graphics, HueBlockPickerLayout layout, int mx, int my) {
        int x = layout.previewLeft(), w = layout.previewWidth(), center = x + w / 2;
        graphics.centeredText(font, HueGradientScreen.text("picker_preview"), center, 36, 0xFFBBBBBB);
        if (preview == null) return;
        int available = layout.gridBottom() - 28;
        int size = Math.min(64, Math.max(32, available / 4));
        int itemTop = 58;
        var stack = HueBlocksRuntime.stack(preview);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(center - size / 2.0F, itemTop);
            graphics.pose().scale(size / 16.0F);
            graphics.item(stack, 0, 0);
        } finally { graphics.pose().popMatrix(); }
        int nameY = itemTop + size + 8;
        boundedText(graphics, stack.getHoverName(), x + 8, nameY, w - 16, 0xFFFFFFFF, mx, my);
        boundedText(graphics, Component.literal(preview.itemId()), x + 8, nameY + 14, w - 16, 0xFFAAAAAA, mx, my);
        int textureY = nameY + 36;
        int textureSize = Math.min(48, layout.gridBottom() - textureY - 24);
        String texture = preview.block().texture();
        var sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                Identifier.withDefaultNamespace("block/" + texture.substring(0, texture.length() - 4))));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x + 8, textureY, textureSize, textureSize);
        boundedText(graphics, HueGradientScreen.text("picker_texture"), x + textureSize + 16, textureY + 2,
                w - textureSize - 24, 0xFFBBBBBB, mx, my);
        boundedText(graphics, Component.literal(String.format(Locale.ROOT, "#%06X", preview.block().rgb())),
                x + textureSize + 16, textureY + 16, w - textureSize - 24, 0xFFBBBBBB, mx, my);
        boundedText(graphics, Component.literal(texture), x + 8, textureY + textureSize + 6, w - 16, 0xFFAAAAAA, mx, my);
    }

    @Override protected boolean mouseScrolledContent(double x, double y, double ax, double ay) {
        var layout = layout();
        if ((ay != 0 || ax != 0) && x >= layout.left() && x < layout.scrollbarLeft() + 6
                && y >= HueBlockPickerLayout.GRID_TOP && y < layout.gridBottom()) {
            scrollTo(scrollRow + ((ay != 0 ? ay : ax) < 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolledContent(x, y, ax, ay);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        boolean handled;
        if (event.key() == 266 || event.key() == 267) {
            scrollTo(scrollRow + (event.key() == 266 ? -layout().rows() : layout().rows())); handled = true;
        }
        else if (getFocused() instanceof HueBlockPickerCell && (event.key() == 264 || event.key() == 265)) {
            int slot = resultButtons.indexOf(getFocused()), columns = layout().columns();
            if (event.key() == 265 && slot < columns && scrollRow > 0) { scrollTo(scrollRow - 1); handled = true; }
            else if (event.key() == 264 && slot + columns >= resultButtons.size() && scrollRow < layout().maxScrollRow(filtered.size())) {
                scrollTo(scrollRow + 1); handled = true;
            } else handled = super.keyPressed(event);
        }
        else handled = super.keyPressed(event);
        keyboardPreview = getFocused() instanceof HueBlockPickerCell;
        return handled;
    }

    @Override protected boolean mouseClickedContent(MouseButtonEvent event, boolean twice) {
        var layout = layout();
        if (event.button() == 0 && event.x() >= layout.scrollbarLeft() && event.x() < layout.scrollbarLeft() + 6
                && event.y() >= HueBlockPickerLayout.GRID_TOP && event.y() < layout.gridBottom()) {
            if (layout.maxScrollRow(filtered.size()) > 0) {
                int top = layout.thumbTop(filtered.size(), scrollRow), thumb = layout.thumbHeight(filtered.size());
                scrollbarGrab = event.y() >= top && event.y() < top + thumb ? (int) event.y() - top : thumb / 2;
                scrollbarDragging = true; scrollTo(layout.rowAt(event.y(), scrollbarGrab, filtered.size()));
            }
            return true;
        }
        return super.mouseClickedContent(event, twice);
    }
    @Override protected boolean mouseReleasedContent(MouseButtonEvent event) {
        if (scrollbarDragging) { scrollbarDragging = false; return true; }
        return super.mouseReleasedContent(event);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
