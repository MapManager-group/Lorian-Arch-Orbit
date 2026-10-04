package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientRecipe;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientRecipeFiles;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Explicit parameter/palette exchange, separate from wheel files and editor session lifetime. */
final class GradientLibraryScreen extends WorkbenchScreen {
    private final HueGradientScreen parent;
    private Component status = Component.empty();
    GradientLibraryScreen(HueGradientScreen parent) {
        super(parent.session, null, HueGradientScreen.text("plans")); this.parent = parent;
    }
    private int left() { return (width - Math.min(440, width - 24)) / 2; }
    private int panel() { return Math.min(440, width - 24); }
    private java.nio.file.Path directory() { return ClientConfigRuntime.configManager().directory(); }
    @Override protected void initWorkbench() {
        int x = left(), w = panel(), half = (w - 8) / 2;
        dropdown(x, 32, half, HueGradientScreen.text("save_plan"), () -> saveMenu(false));
        dropdown(x + half + 8, 32, half, HueGradientScreen.text("save_palette"), () -> saveMenu(true));
        action(x, 60, half, HueGradientScreen.text("import_clipboard"), () -> {
            try { preview(GradientRecipe.decode(minecraft.keyboardHandler.getClipboard())); }
            catch (IllegalArgumentException ex) { failure(ex); }
        });
        dropdown(x + half + 8, 60, half, HueGradientScreen.text("load_file"), () -> {
            try {
                var entries = new ArrayList<MenuEntry>();
                for (var file : GradientRecipeFiles.list(directory())) entries.add(new MenuEntry(Component.literal(file.getFileName().toString()), () -> {
                    try { preview(GradientRecipeFiles.read(directory(), file)); }
                    catch (IOException | IllegalArgumentException ex) { failure(ex); }
                }));
                if (entries.isEmpty()) status = HueGradientScreen.text("no_plans"); else menu(x, 84, entries);
            } catch (IOException ex) { failure(ex); }
        });
        action(x + w - 64, height - 26, 64, HueGradientScreen.text("back"), this::onClose);
    }
    private void saveMenu(boolean paletteOnly) {
        menu(left(), 54, List.of(
                new MenuEntry(HueGradientScreen.text("save_file"), () -> save(paletteOnly, false)),
                new MenuEntry(HueGradientScreen.text("copy_json"), () -> save(paletteOnly, true))));
    }
    private void save(boolean paletteOnly, boolean clipboard) {
        try {
            String text = GradientRecipe.encode(parent.model, paletteOnly);
            if (clipboard) { minecraft.keyboardHandler.setClipboard(text); status = HueGradientScreen.text("copied_plan"); }
            else status = HueGradientScreen.text("saved_plan", GradientRecipeFiles.save(directory(), text).getFileName());
        } catch (IOException | IllegalArgumentException ex) { failure(ex); }
    }
    private void preview(GradientRecipe.Recipe recipe) {
        int missing = recipe.missing(HueBlocksRuntime.candidates()).size();
        boolean missingGroup = recipe.palette().equals("group") && session.wheelState.resolve(recipe.group()) == null;
        var data = HueBlocksRuntime.repository().data();
        boolean missingPalette = !recipe.palette().equals("all") && !recipe.palette().equals("group")
                && (data == null || data.palettes().stream().noneMatch(p -> p.name().equals(recipe.palette())));
        // Never silently turn a missing source group/preset into an unrestricted candidate pool.
        if (missingGroup || missingPalette) { status = HueGradientScreen.text("plan_source_missing"); return; }
        confirm(HueGradientScreen.text(recipe.paletteOnly() ? "import_palette_confirm" : "import_plan_confirm", missing),
                HueGradientScreen.text("import_accept"), () -> {
                    recipe.apply(parent.model, HueBlocksRuntime.candidates()); parent.parametersImported();
                    minecraft.setScreenAndShow(parent);
                });
    }
    private void failure(Exception ex) { status = HueGradientScreen.text("plan_error", ex.getMessage()); }
    @Override protected void renderWorkbench(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        boundedText(graphics, HueGradientScreen.text("plans_hint"), left(), 90, panel(), 0xFFBBBBBB, mx, my);
        boundedText(graphics, Component.literal(GradientRecipeFiles.directory(directory()).toString()), left(), 106, panel(), 0xFF999999, mx, my);
        boundedText(graphics, status, left(), height - 40, panel(), 0xFFFFC14D, mx, my);
    }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
}
