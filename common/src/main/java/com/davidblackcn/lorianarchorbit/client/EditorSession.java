package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** One in-memory editing visit. Pages are retained, including their focus-independent view state. */
final class EditorSession {
    record Page(String id, String name, String description, EditorPageIllustration illustration,
                Function<EditorSession, WorkbenchScreen> factory) { }
    static final List<Page> PAGES = List.of(
            new Page("wheel", "wheel", "wheel_hint", EditorPageIllustration.WHEEL, PaletteEditorScreen::new),
            new Page("gradient", "gradient", "gradient_hint", EditorPageIllustration.GRADIENT, HueGradientScreen::new));
    private final Screen parent;
    private final Map<String, WorkbenchScreen> pages = new LinkedHashMap<>();
    private final EditorHomeScreen home;
    final com.davidblackcn.lorianarchorbit.palette.WheelEditorState wheelState;
    final com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench gradientState =
            new com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench();

    private EditorSession(Screen parent) {
        this.parent = parent;
        wheelState = new com.davidblackcn.lorianarchorbit.palette.WheelEditorState(
                ClientConfigRuntime.configManager().primaryWheel(), ClientConfigRuntime.configManager().secondaryWheel());
        home = new EditorHomeScreen(this);
    }
    static Screen open(Screen parent) { return new EditorSession(parent).home; }
    static Component text(String key, Object... args) {
        return Component.translatable("workbench.lorian_arch_orbit." + key, args);
    }
    WorkbenchScreen page(String id) {
        WorkbenchScreen existing = pages.get(id);
        if (existing != null) return existing;
        WorkbenchScreen created = PAGES.stream().filter(p -> p.id().equals(id))
                .findFirst().orElseThrow().factory().apply(this);
        pages.put(id, created);
        return created;
    }
    PaletteEditorScreen wheel() { return (PaletteEditorScreen) page("wheel"); }
    void show(String id) { Minecraft.getInstance().setScreenAndShow(page(id)); }
    void home() { Minecraft.getInstance().setScreenAndShow(home); }
    void exit() {
        exit(home);
    }
    void exit(WorkbenchScreen current) {
        boolean wheelDirty = pages.get("wheel") instanceof PaletteEditorScreen wheel && wheel.hasUnsavedChanges();
        boolean gradientDirty = pages.get("gradient") instanceof HueGradientScreen gradient && gradient.hasUnappliedChanges();
        if (wheelDirty || gradientDirty) {
            String key = wheelDirty && gradientDirty ? "unsaved_both" : wheelDirty ? "unsaved_wheel" : "unsaved_gradient";
            current.confirm(text(key), text("discard_exit"), () -> Minecraft.getInstance().setScreenAndShow(parent));
        } else Minecraft.getInstance().setScreenAndShow(parent);
    }
}
