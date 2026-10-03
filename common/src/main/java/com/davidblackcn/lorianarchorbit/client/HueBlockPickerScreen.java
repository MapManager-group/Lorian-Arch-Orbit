package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Native buttons give every selectable block keyboard focus and narration as well as pointer access. */
final class HueBlockPickerScreen extends AdaptivePaletteScreen {
    private final Screen parent;
    private final List<HueGradient.Candidate> source;
    private final Consumer<HueGradient.Candidate> picked;
    private List<HueGradient.Candidate> filtered = List.of();
    private final java.util.ArrayList<Button> resultButtons = new java.util.ArrayList<>();
    private EditBox search;
    private String query = "";
    private int page;

    HueBlockPickerScreen(Screen parent, List<HueGradient.Candidate> source, Consumer<HueGradient.Candidate> picked) {
        super(HueGradientScreen.text("pick_block"));
        this.parent = parent;
        this.source = List.copyOf(source);
        this.picked = picked;
    }

    @Override
    protected void onViewportChanged(PaletteViewport previous, PaletteViewport next) {
        int anchor = page * Math.max(1, (previous.height() - 88) / 24) * 2;
        page = anchor / (Math.max(1, (next.height() - 88) / 24) * 2);
    }

    @Override
    protected void initContent() {
        resultButtons.clear();
        int panel = Math.min(680, width - 24);
        int left = (width - panel) / 2;
        search = new EditBox(font, left, 28, panel - 48, 20, HueGradientScreen.text("search"));
        search.setHint(HueGradientScreen.text("search"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; updateResults(); });
        addRenderableWidget(search);
        addRenderableWidget(Button.builder(HueGradientScreen.text("clear"), b -> { search.setValue(""); setInitialFocus(search); })
                .bounds(left + panel - 44, 28, 44, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1)).bounds(left, height - 24, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1)).bounds(left + 34, height - 24, 30, 20).build());
        addRenderableWidget(Button.builder(HueGradientScreen.text("back"), b -> onClose()).bounds(left + panel - 72, height - 24, 72, 20).build());
        updateResults();
        setInitialFocus(search);
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(maxPage(), page + delta));
        rebuildWidgets();
    }

    private void updateResults() {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = source.stream().filter(c -> c.itemId().contains(needle)
                || HueBlocksRuntime.stack(c).getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
        page = Math.min(page, maxPage());
        // Only result buttons change; keep the active text field and its cursor/focus.
        resultButtons.forEach(this::removeWidget);
        resultButtons.clear();
        int panel = Math.min(680, width - 24);
        int left = (width - panel) / 2;
        int cellWidth = (panel - 4) / 2;
        for (int slot = 0; slot < pageSize() && page * pageSize() + slot < filtered.size(); slot++) {
            HueGradient.Candidate candidate = filtered.get(page * pageSize() + slot);
            int x = left + (slot % 2) * (cellWidth + 4);
            int y = 56 + (slot / 2) * 24;
            Component name = HueBlocksRuntime.stack(candidate).getHoverName();
            Component visibleName = Component.literal(font.plainSubstrByWidth(name.getString(), cellWidth - 44));
            Button button = Button.builder(visibleName, b -> {
                picked.accept(candidate);
                minecraft.setScreenAndShow(parent);
            }).bounds(x, y, cellWidth, 20).build();
            button.setTooltip(Tooltip.create(Component.literal(name.getString() + " • " + candidate.itemId() + " • " + candidate.block().texture())));
            resultButtons.add(button);
            addRenderableWidget(button);
        }
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        int panel = Math.min(680, width - 24);
        int left = (width - panel) / 2;
        int cellWidth = (panel - 4) / 2;
        for (int slot = 0; slot < pageSize() && page * pageSize() + slot < filtered.size(); slot++) {
            graphics.item(HueBlocksRuntime.stack(filtered.get(page * pageSize() + slot)),
                    left + (slot % 2) * (cellWidth + 4) + 3, 58 + (slot / 2) * 24);
        }
        if (filtered.isEmpty()) graphics.centeredText(font, HueGradientScreen.text("no_candidates"), width / 2, 64, 0xFFFFC14D);
        graphics.centeredText(font, Component.literal((page + 1) + " / " + (maxPage() + 1)), width / 2, height - 18, 0xFFBBBBBB);
    }

    @Override
    protected boolean mouseScrolledContent(double x, double y, double ax, double ay) {
        changePage((ay != 0 ? ay : ax) < 0 ? 1 : -1);
        return true;
    }

    private int pageSize() { return Math.max(1, (height - 88) / 24) * 2; }
    private int maxPage() { return Math.max(0, (filtered.size() - 1) / pageSize()); }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }

}
