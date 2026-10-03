package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Position-specific editor; the parent always reveals the full sequence on return. */
final class GradientInspectorScreen extends AdaptivePaletteScreen {
    private final HueGradientScreen parent;
    private int index;
    private List<HueGradient.Candidate> alternatives = List.of();
    GradientInspectorScreen(HueGradientScreen parent, int index) {
        super(EditorSession.text("edit_results")); this.parent = parent; this.index = index;
    }
    private int panel() { return Math.min(600, width - 24); }
    private int left() { return (width - panel()) / 2; }
    private HueGradient.Target target() {
        return parent.model.valid() ? HueGradient.targets(parent.model.stops()).get(index) : parent.model.samples().get(index).target();
    }
    @Override protected void initContent() {
        int x = left(), w = panel();
        alternatives = HueGradient.alternatives(target(), parent.candidates(), parent.model.oklab).stream().limit(8).toList();
        button(x, 28, 24, Component.literal("<"), () -> { index = Math.max(0, index - 1); rebuildWidgets(); });
        button(x + w - 24, 28, 24, Component.literal(">"), () -> { index = Math.min(parent.model.samples().size() - 1, index + 1); rebuildWidgets(); });
        int cw = (w - 12) / 4;
        for (int slot = 0; slot < alternatives.size(); slot++) {
            var candidate = alternatives.get(slot);
            Component label = HueBlocksRuntime.stack(candidate).getHoverName();
            Button b = button(x + slot % 4 * (cw + 4), 76 + slot / 4 * 24, cw,
                    Component.literal(font.plainSubstrByWidth(label.getString(), Math.max(1, cw - 44))), () -> {
                        parent.model.replace(index, candidate); rebuildWidgets();
                    });
            b.setTooltip(Tooltip.create(label.copy().append("\n" + candidate.block().texture())));
        }
        int y = height - 26;
        button(x, y, 68, EditorSession.text("search_more"), () -> minecraft.setScreenAndShow(new HueBlockPickerScreen(
                this, HueGradient.alternatives(target(), parent.candidates(), parent.model.oklab), c -> parent.model.replace(index, c))));
        button(x + 72, y, 60, EditorSession.text(parent.model.samples().get(index).locked() ? "unlock" : "lock"), () -> {
            parent.model.toggleLock(index); rebuildWidgets();
        });
        button(x + 136, y, 84, EditorSession.text("set_node"), () -> {
            parent.model.setBlock(parent.model.samples().get(index).candidate()); onClose();
        });
        button(x + w - 64, y, 64, HueGradientScreen.text("back"), this::onClose);
    }
    private Button button(int x, int y, int w, Component label, Runnable action) {
        return addRenderableWidget(Button.builder(label, b -> action.run()).bounds(x, y, w, 20).build());
    }
    @Override protected void renderContent(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        super.renderContent(graphics, mx, my, tick);
        int x = left(), w = panel(); var sample = parent.model.samples().get(index);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        boundedText(graphics, EditorSession.text("sample_position", index + 1).copy().append(" · ")
                .append(HueBlocksRuntime.stack(sample.candidate()).getHoverName()), x + 28, 34, w - 56, 0xFFFFFFFF, mx, my);
        graphics.item(HueBlocksRuntime.stack(sample.candidate()), x, 52);
        graphics.fill(x + 22, 53, x + 38, 67, 0xFF000000 | target().displayRgb(parent.model.oklab));
        boundedText(graphics, EditorSession.text(sample.locked() ? "locked_target" : "unlocked_target"), x + 44, 56, w - 44,
                parent.model.conflicts(parent.candidates()).contains(index) ? 0xFFFF7777 : 0xFFBBBBBB, mx, my);
        int cw = (w - 12) / 4;
        for (int slot = 0; slot < alternatives.size(); slot++) graphics.item(HueBlocksRuntime.stack(alternatives.get(slot)),
                x + slot % 4 * (cw + 4) + 2, 78 + slot / 4 * 24);
        boundedText(graphics, parent.model.conflicts(parent.candidates()).contains(index)
                ? EditorSession.text("lock_conflicts") : EditorSession.text("alternatives_hint"), x, height - 40, w, 0xFFBBBBBB, mx, my);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
