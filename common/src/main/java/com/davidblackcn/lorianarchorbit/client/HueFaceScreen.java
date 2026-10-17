package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlockFaces;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.function.Consumer;

/** Face inspection is local until the explicit use action; it never rematches the gradient. */
final class HueFaceScreen extends AdaptivePaletteScreen {
    private final Screen parent, returnTo;
    private final HueGradient.Candidate original;
    private final List<HueGradient.Candidate> allowed;
    private final Consumer<HueGradient.Candidate> use;
    private String face;
    private int variant;
    private List<HueBlockFaces.Surface> surfaces;
    private Component status = Component.empty();
    HueFaceScreen(Screen parent, Screen returnTo, HueGradient.Candidate original,
                  List<HueGradient.Candidate> allowed, Consumer<HueGradient.Candidate> use) {
        super(HueGradientScreen.text("face_preview"));
        this.parent = parent; this.returnTo = returnTo; this.original = original; this.allowed = List.copyOf(allowed); this.use = use;
        face = HueBlockFaces.DIRECTIONS.stream().filter(original.faces()::contains).findFirst().orElse("top");
        update();
        for (int i = 0; i < surfaces.size(); i++) if (surfaces.get(i).texture().equals(original.block().texture())) variant = i;
    }
    private int panel() { return Math.min(560, width - 24); }
    private int left() { return (width - panel()) / 2; }
    private void update() {
        surfaces = HueBlocksRuntime.surfaces(original.itemId()).stream().filter(s -> s.faces().contains(face))
                .sorted(java.util.Comparator.comparing(s -> !s.defaultFaces().contains(face))).toList();
        variant = Math.clamp(variant, 0, Math.max(0, surfaces.size() - 1));
    }
    private HueGradient.Candidate selected() {
        return surfaces.isEmpty() ? null : allowed.stream().filter(c -> c.itemId().equals(original.itemId())
                && c.block().texture().equals(surfaces.get(variant).texture())).findFirst().orElse(null);
    }
    @Override protected void initContent() {
        update();
        int x = left(), w = panel(), cw = (w - 20) / 6;
        for (int i = 0; i < 6; i++) {
            String direction = HueBlockFaces.DIRECTIONS.get(i);
            button(x + i * (cw + 4), 28, cw, HueGradientScreen.text("face." + direction), () -> {
                face = direction; variant = 0; rebuildWidgets();
            }).active = !face.equals(direction);
        }
        button(x + w - 88, 56, 42, Component.literal("<"), () -> { variant--; rebuildWidgets(); }).active = variant > 0;
        button(x + w - 42, 56, 42, Component.literal(">"), () -> { variant++; rebuildWidgets(); }).active = variant + 1 < surfaces.size();
        button(x + w - 132, height - 26, 64, HueGradientScreen.text("back"), this::onClose);
        button(x + w - 64, height - 26, 64, HueGradientScreen.text("use_face"), () -> {
            var candidate = selected();
            if (candidate != null) { use.accept(candidate); minecraft.setScreenAndShow(returnTo); }
        }).active = selected() != null;
    }
    private Button button(int x, int y, int width, Component label, Runnable run) {
        return addRenderableWidget(Button.builder(label, b -> run.run()).bounds(x, y, width, 20).build());
    }
    @Override protected void renderContent(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        super.renderContent(graphics, mx, my, tick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        int x = left(), w = panel(), size = Math.clamp(height - 128, 32, 112);
        if (surfaces.isEmpty()) status = HueGradientScreen.text("face_unavailable");
        else {
            var surface = surfaces.get(variant);
            boolean present = HueTexture.draw(graphics, surface.texture(), x, 56, size);
            int tx = x + size + 8, tw = w - size - 8;
            boundedText(graphics, HueBlocksRuntime.stack(original).getHoverName(), tx, 84, tw, 0xFFFFFFFF, mx, my);
            boundedText(graphics, Component.literal(surface.texture()), tx, 98, tw, 0xFFBBBBBB, mx, my);
            String placement = surface.placements().get(face);
            boundedText(graphics, placement.isBlank() ? HueGradientScreen.text("placement_fixed") : HueGradientScreen.text("placement", placement),
                    tx, 112, tw, 0xFFBBBBBB, mx, my);
            status = !present ? EditorSession.text("missing_texture") : selected() == null ? HueGradientScreen.text("face_not_candidate")
                    : HueGradientScreen.text("face_use_hint", variant + 1, surfaces.size());
        }
        boundedText(graphics, status, x, height - 40, w, 0xFFFFC14D, mx, my);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
