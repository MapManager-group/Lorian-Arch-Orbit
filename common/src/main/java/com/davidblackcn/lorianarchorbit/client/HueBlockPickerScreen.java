package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlockFaces;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueFaceSelection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
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
    private final List<Button> faceButtons = new ArrayList<>();
    private HueFaceSelection selection;
    private Button useButton;
    private EditBox search;
    private String query = "";
    private boolean textures = true;
    private com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench custom;
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

    HueBlockPickerScreen(HueGradientScreen parent, List<HueGradient.Candidate> source,
                         com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench custom) {
        this(parent, source, c -> {}); this.custom = custom;
    }

    private HueBlockPickerLayout layout() { return HueBlockPickerLayout.calculate(width, height, custom == null); }

    @Override protected void onViewportChanged(PaletteViewport previous, PaletteViewport next) {
        int anchor = scrollRow * HueBlockPickerLayout.calculate(previous.width(), previous.height(), custom == null).columns();
        scrollRow = HueBlockPickerLayout.calculate(next.width(), next.height(), custom == null).rowForAnchor(anchor);
    }

    @Override protected void initContent() {
        resultButtons.clear();
        faceButtons.clear();
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
                .bounds(layout.left() + layout.width() - (custom == null ? 132 : 64), layout.footerTop(), 64, 20).build());
        if (custom == null) useButton = addRenderableWidget(Button.builder(HueGradientScreen.text("use_face"), b -> {
            if (selection != null && selection.selected() != null) {
                picked.accept(selection.selected());
                minecraft.setScreenAndShow(parent);
            }
        }).bounds(layout.left() + layout.width() - 64, layout.footerTop(), 64, 20).build());
        addRenderableWidget(Button.builder(HueGradientScreen.text(textures ? "show_items" : "show_textures"), b -> {
            textures = !textures; rebuildWidgets();
        }).bounds(layout.left(), layout.footerTop(), 72, 20).build());
        if (custom != null) addRenderableWidget(Button.builder(HueGradientScreen.text("include_all"), b -> {
            custom.excluded.clear(); custom.invalidate(); updateGrid();
        }).bounds(layout.left() + 76, layout.footerTop(), 68, 20).build());
        if (custom != null) addRenderableWidget(Button.builder(HueGradientScreen.text("exclude_visible"), b -> {
            filtered.forEach(c -> custom.excluded.add(c.key())); custom.invalidate(); updateGrid();
        }).bounds(layout.left() + 148, layout.footerTop(), 68, 20).build());
        updateResults();
        updateFaceControls();
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
        int focusedSlot = resultButtons.indexOf(getFocused());
        if (getFocused() instanceof HueBlockPickerCell) setFocused(null);
        resultButtons.forEach(this::removeWidget);
        resultButtons.clear();
        var layout = layout();
        int start = scrollRow * layout.columns();
        for (int slot = 0; slot < layout.visibleSlots() && start + slot < filtered.size(); slot++) {
            HueGradient.Candidate candidate = filtered.get(start + slot);
            var button = new HueBlockPickerCell(candidate, layout.cellX(slot), layout.cellY(slot), b -> {
                if (custom != null) {
                    if (custom.allows(candidate)) custom.exclude(candidate); else custom.include(candidate);
                    updateGrid();
                } else {
                    selection = new HueFaceSelection(candidate, source, HueBlocksRuntime.surfaces(candidate.itemId()));
                    preview = candidate;
                    updateFaceControls();
                }
            });
            button.textures = textures; button.excluded = custom != null && !custom.allows(candidate);
            button.selected = selection != null && selection.original().key().equals(candidate.key());
            resultButtons.add(button);
            addRenderableWidget(button);
        }
        preview = selection != null ? selection.original() : resultButtons.isEmpty() ? null : resultButtons.getFirst().candidate;
        if (focusedSlot >= 0 && !resultButtons.isEmpty()) setInitialFocus(resultButtons.get(Math.min(focusedSlot, resultButtons.size() - 1)));
    }

    private void updateFaceControls() {
        if (custom != null) return;
        int focusedFaceControl = faceButtons.indexOf(getFocused());
        if (focusedFaceControl >= 0) setFocused(null);
        faceButtons.forEach(this::removeWidget);
        faceButtons.clear();
        useButton.active = selection != null && selection.selected() != null;
        for (var cell : resultButtons) cell.selected = selection != null && selection.original().key().equals(cell.candidate.key());
        var l = layout();
        for (int i = 0; i < HueBlockFaces.DIRECTIONS.size(); i++) {
            String direction = HueBlockFaces.DIRECTIONS.get(i);
            var button = Button.builder(HueGradientScreen.text("face." + direction), b -> {
                selection.face(direction); updateFaceControls();
            }).bounds(l.faceX(i), l.faceY(i), l.faceWidth(), 20).build();
            button.active = selection != null && selection.hasFace(direction);
            if (selection != null && selection.face().equals(direction)) button.setMessage(
                    Component.literal("[ ").append(button.getMessage()).append(" ]"));
            faceButtons.add(addRenderableWidget(button));
        }
        var variants = Button.builder(HueGradientScreen.text("face_variant", selection == null ? 0 : selection.variant() + 1,
                selection == null ? 0 : selection.count()), b -> { selection.advance(); updateFaceControls(); })
                .bounds(l.previewLeft() + 4, l.faceTop() + 48, l.previewWidth() - 8, 18).build();
        variants.active = selection != null && selection.count() > 1;
        variants.setTooltip(Tooltip.create(HueGradientScreen.text("face_variant_hint")));
        faceButtons.add(addRenderableWidget(variants));
        if (focusedFaceControl >= 0) setInitialFocus(faceButtons.get(focusedFaceControl));
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
            int bottom = custom == null ? Math.max(layout.gridBottom(), layout.faceTop() + 66) : layout.gridBottom();
            graphics.fill(layout.previewLeft(), 28, layout.previewLeft() + layout.previewWidth(), bottom, 0x55202020);
            WorkbenchFlatButton.border(graphics, layout.previewLeft(), 28, layout.previewWidth(), bottom - 28, 0x555F5F5F);
        }
        super.renderContent(graphics, mx, my, tick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        boundedText(graphics, HueGradientScreen.text("picker_counts", filtered.stream().map(HueGradient.Candidate::itemId).distinct().count(), filtered.size()), layout.left(), 56,
                layout.gridWidth(), 0xFFBBBBBB, mx, my);
        if (filtered.isEmpty()) boundedText(graphics, HueGradientScreen.text("picker_empty"), layout.left() + 4,
                HueBlockPickerLayout.GRID_TOP + 8, layout.gridWidth() - 8, 0xFFFFC14D, mx, my);

        if (selection == null) {
            if (keyboardPreview && getFocused() instanceof HueBlockPickerCell cell) preview = cell.candidate;
            else for (var button : resultButtons) if (button.isHovered()) preview = button.candidate;
        }
        if (layout.previewWidth() > 0) renderPreview(graphics, layout, mx, my);
        boundedText(graphics, pickerStatus(), layout.left(), layout.footerTop() - 14,
                layout.width(), 0xFFBBBBBB, mx, my);
    }

    private Component pickerStatus() {
        if (custom != null) return HueGradientScreen.text("custom_hint");
        if (selection == null) return HueGradientScreen.text("picker_choose_hint");
        if (selection.surface() == null) return HueGradientScreen.text("face_unavailable");
        if (selection.selected() == null) return HueGradientScreen.text("face_not_candidate");
        String placement = selection.surface().placements().get(selection.face());
        return placement.isBlank() ? HueGradientScreen.text("placement_fixed") : HueGradientScreen.text("placement", placement);
    }

    private void renderPreview(GuiGraphicsExtractor graphics, HueBlockPickerLayout layout, int mx, int my) {
        if (preview == null) return;
        int x = layout.previewLeft(), w = layout.previewWidth();
        int bottom = custom == null ? layout.faceTop() - 4 : layout.gridBottom() - 4;
        var stack = HueBlocksRuntime.stack(preview);
        String texture = selection != null && selection.surface() != null
                ? selection.surface().texture() : preview.block().texture();
        int textureY, textureSize;
        if (bottom - 28 >= 180) {
            graphics.centeredText(font, HueGradientScreen.text("picker_preview"), x + w / 2, 36, 0xFFBBBBBB);
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(x + w / 2.0F - 32, 54);
                graphics.pose().scale(4.0F);
                graphics.item(stack, 0, 0);
            } finally { graphics.pose().popMatrix(); }
            boundedText(graphics, stack.getHoverName(), x + 8, 126, w - 16, 0xFFFFFFFF, mx, my);
            boundedText(graphics, Component.literal(preview.itemId()), x + 8, 140, w - 16, 0xFFAAAAAA, mx, my);
            textureY = 158;
            textureSize = Math.min(48, bottom - textureY - 16);
        } else {
            boundedText(graphics, stack.getHoverName(), x + 4, 32, w - 8, 0xFFFFFFFF, mx, my);
            textureY = 46;
            textureSize = Math.min(32, bottom - textureY);
        }
        boolean present = HueTexture.draw(graphics, texture, x + 4, textureY, textureSize);
        var candidate = selection == null ? preview : selection.selected();
        Component caption = !present ? EditorSession.text("missing_texture") : candidate == null
                ? HueGradientScreen.text("face_not_candidate") : Component.literal(String.format(Locale.ROOT, "#%06X", candidate.block().rgb()));
        boundedText(graphics, caption, x + textureSize + 8, textureY, w - textureSize - 12, 0xFFBBBBBB, mx, my);
        boundedText(graphics, Component.literal(texture), x + textureSize + 8, textureY + 10,
                w - textureSize - 12, 0xFFAAAAAA, mx, my);
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
