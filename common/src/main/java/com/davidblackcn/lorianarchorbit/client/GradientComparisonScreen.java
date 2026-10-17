package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Read-only snapshots with a shared position offset and original repeated positions. */
final class GradientComparisonScreen extends AdaptivePaletteScreen {
    private final HueGradientScreen parent;
    private final GradientWorkbench.Comparison before, after;
    private int offset;
    GradientComparisonScreen(HueGradientScreen parent) {
        super(HueGradientScreen.text("compare")); this.parent = parent;
        before = parent.model.comparison(); after = parent.model.snapshot();
    }
    private int columns() { return Math.max(1, (width - 24) / 24); }
    private int rows() { return Math.max(1, (height - 100) / 48); }
    private int capacity() { return columns() * rows(); }
    private void scroll(int delta) { offset = Math.clamp(offset + delta, 0, Math.max(0, Math.max(before.samples().size(), after.samples().size()) - capacity())); }
    @Override protected void initContent() {
        scroll(0);
        addRenderableWidget(Button.builder(HueGradientScreen.text("back"), b -> onClose()).bounds(width - 76, height - 26, 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("<"), b -> scroll(-capacity())).bounds(12, height - 26, 24, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> scroll(capacity())).bounds(40, height - 26, 24, 20).build());
    }
    @Override protected void renderContent(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        super.renderContent(graphics, mx, my, tick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        draw(graphics, before, 28, "comparison_saved", mx, my);
        draw(graphics, after, 48 + rows() * 24, "comparison_current", mx, my);
        boundedText(graphics, HueGradientScreen.text("comparison_hint", offset + 1), 72, height - 20, width - 156, 0xFFBBBBBB, mx, my);
    }
    private void draw(GuiGraphicsExtractor graphics, GradientWorkbench.Comparison snapshot, int top, String label, int mx, int my) {
        boundedText(graphics, HueGradientScreen.text(label, snapshot.samples().size(), snapshot.oklab() ? "OkLAB" : "RGB")
                .copy().append(" · ").append(HueGradientScreen.text("face." + snapshot.face())), 12, top, width - 24, 0xFFBBBBBB, mx, my);
        List<GradientWorkbench.Sample> samples = snapshot.samples();
        for (int i = offset; i < Math.min(samples.size(), offset + capacity()); i++) {
            var sample = samples.get(i); int slot = i - offset, x = 12 + slot % columns() * 24, y = top + 14 + slot / columns() * 24;
            HueTexture.draw(graphics, sample.candidate().block().texture(), x, y, 22);
            if (mx >= x && mx < x + 22 && my >= y && my < y + 22) graphics.setTooltipForNextFrame(font,
                    HueGradientScreen.text("block_hint", HueBlocksRuntime.stack(sample.candidate()).getHoverName(), sample.candidate().itemId(), sample.candidate().block().texture()), mx, my);
        }
    }
    @Override protected boolean mouseScrolledContent(double x, double y, double ax, double ay) { scroll((ay < 0 ? 1 : -1) * columns()); return true; }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == 266 || event.key() == 267) { scroll(event.key() == 266 ? -capacity() : capacity()); return true; }
        return super.keyPressed(event);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
