package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.GradientWorkbench;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksData;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Settings, positional editing and texture inspection share one memory-only model. */
final class HueGradientScreen extends WorkbenchScreen {
    private static final List<String> FACES = List.of("all", "sides", "top", "bottom", "north", "south", "east", "west");
    final GradientWorkbench model;
    private boolean resultTab, textures, vertical, editing;
    private boolean hideConsecutive = true;
    private int cell = 24, textureOffset, crossOffset, settingsScroll, nodeStart, page;
    private Component message = Component.empty();
    private HueBlocksData widgetData;
    private List<HueGradient.Candidate> previousCandidates = List.of();
    private ApplyTarget target = ApplyTarget.TEMPORARY;
    private Button applyButton, generateButton, restoreButton, refreshButton;
    private final List<Button> nodeButtons = new ArrayList<>();
    private int nodeButtonStart;

    HueGradientScreen(EditorSession session) {
        super(session, "gradient", text("title")); model = session.gradientState;
        model.sourceGroup = session.wheel().selectedRef(); model.targetGroup = model.sourceGroup;
        HueBlocksRuntime.initialize();
    }
    static Component text(String key, Object... args) { return Component.translatable("hueblocks.lorian_arch_orbit." + key, args); }
    static Component work(String key, Object... args) { return EditorSession.text(key, args); }
    boolean hasUnappliedChanges() { return model.unapplied(); }
    private HueGradientLayout layout() { return HueGradientLayout.calculate(width, height); }
    private boolean settingsVisible() { return !layout().compact() || !resultTab; }
    private boolean resultsVisible() { return !layout().compact() || resultTab; }
    @Override protected void onViewportChanged(PaletteViewport previous, PaletteViewport next) {
        int anchor = page * HueGradientLayout.calculate(previous.width(), previous.height()).pageSize();
        page = anchor / HueGradientLayout.calculate(next.width(), next.height()).pageSize();
    }
    @Override protected void initWorkbench() {
        var l = layout(); nodeButtons.clear(); generateButton = null;
        settingsScroll = Math.clamp(settingsScroll, 0, l.settingsScrollMax()); page = Math.min(page, maxPage());
        if (l.compact()) {
            action(l.left(), 28, 84, work("settings"), () -> { resultTab = false; rebuildWidgets(); }).active = resultTab;
            action(l.left() + 88, 28, 84, work("results"), () -> { resultTab = true; rebuildWidgets(); }).active = !resultTab;
        }
        refreshButton = action(l.left() + l.width() - 66, l.compact() ? 50 : 28, 66, text("refresh"),
                () -> HueBlocksRuntime.repository().refresh());
        if (settingsVisible()) initSettings();
        if (resultsVisible()) initResults();
        action(l.backLeft(), l.footerTop(), 64, text("back"), session::home);
        restoreButton = action(l.left(), l.footerTop(), 64, text("restore_inventory"), () -> {
            if (CreativeInventoryHelper.restore(minecraft)) message = text("inventory_restored");
        });
        restoreButton.setTooltip(Tooltip.create(text("restore_inventory_hint")));
        dropdown(l.targetLeft(), l.footerTop(), l.targetWidth(), text(target.key), () -> {
            List<MenuEntry> choices = new ArrayList<>();
            for (ApplyTarget option : ApplyTarget.values()) choices.add(new MenuEntry(text(option.key), () -> {
                target = option; rebuildWidgets(); if (option == ApplyTarget.REPLACE) chooseGroup(false);
            }));
            menu(l.left() + l.width() - 224, l.footerTop() - 90, choices);
        });
        applyButton = action(l.applyLeft(), l.footerTop(), 64, text("apply"), this::apply);
        widgetData = HueBlocksRuntime.repository().data(); updateAvailability();
    }
    private Button settingAction(int x, int offset, int w, Component label, Runnable callback) {
        var l = layout(); int y = l.bodyTop() + offset - settingsScroll;
        return y < l.bodyTop() || y + 20 > l.bodyBottom() ? null : action(x, y, w, label, callback);
    }
    private Button settingDropdown(int x, int offset, int w, Component label, Runnable callback) {
        var l = layout(); int y = l.bodyTop() + offset - settingsScroll;
        return y < l.bodyTop() || y + 20 > l.bodyBottom() ? null : dropdown(x, y, w, label, callback);
    }
    private void initSettings() {
        var l = layout(); int x = l.left(), w = l.settingsWidth() - 16;
        boolean overflow = HueGradientLayout.nodeOverflow(w, model.nodes.size());
        int visibleNodes = HueGradientLayout.visibleNodes(w, model.nodes.size());
        nodeStart = Math.clamp(nodeStart, 0, Math.max(0, model.nodes.size() - visibleNodes)); nodeButtonStart = nodeStart;
        if (overflow) {
            Button previous = settingAction(x, 16, 16, Component.literal("<"), () -> { nodeStart--; rebuildWidgets(); });
            if (previous != null) previous.active = nodeStart > 0;
        }
        for (int i = 0; i < visibleNodes && nodeStart + i < model.nodes.size(); i++) {
            int index = nodeStart + i;
            Button b = settingAction(x + (overflow ? 20 : 0) + i * 28, 16, 26, Component.literal(Integer.toString(index + 1)), () -> {
                model.selectedNode = index; rebuildWidgets();
            });
            if (b != null) { b.active = index != model.selectedNode; nodeButtons.add(b); }
        }
        if (overflow) {
            Button next = settingAction(x + w - 16, 16, 16, Component.literal(">"), () -> { nodeStart++; rebuildWidgets(); });
            if (next != null) next.active = nodeStart + visibleNodes < model.nodes.size();
        }
        int third = (w - 8) / 3;
        settingAction(x, 42, third, text("add_node"), () -> {
            if (model.nodes.size() == HueGradient.MAX_STOPS) return;
            structural(() -> {
                var n = model.nodes.get(model.selectedNode);
                model.structural(() -> model.nodes.add(++model.selectedNode, new GradientWorkbench.Node(n.hex, n.steps)));
                nodeStart = Math.max(0, model.selectedNode - visibleNodes + 1);
            });
        });
        settingAction(x + third + 4, 42, third, text("remove_node"), () -> {
            if (model.nodes.size() <= 2) return;
            structural(() -> model.structural(() -> {
                model.nodes.remove(model.selectedNode); model.selectedNode = Math.min(model.selectedNode, model.nodes.size() - 1);
            }));
        });
        settingAction(x + 2 * (third + 4), 42, third, text("reverse"), () -> structural(model::reverse));
        settingAction(x, 66, (w - 4) / 2, work("move_before"), () -> moveNode(-1));
        settingAction(x + (w - 4) / 2 + 4, 66, (w - 4) / 2, work("move_after"), () -> moveNode(1));
        var node = model.nodes.get(model.selectedNode);
        int colorY = l.bodyTop() + 108 - settingsScroll;
        if (colorY >= l.bodyTop() && colorY + 20 <= l.bodyBottom()) {
            EditBox color = new EditBox(font, x, colorY, w, 20, text("color"));
            color.setMaxLength(7); color.setValue(node.hex); color.setHint(Component.literal("#RRGGBB"));
            color.setResponder(model::setColor); color.setTooltip(Tooltip.create(text("color_hint"))); addRenderableWidget(color);
        }
        settingAction(x, 132, (w - 4) / 2, text("held_block"), this::useHeld);
        settingAction(x + (w - 4) / 2 + 4, 132, (w - 4) / 2, text("pick_block"), () ->
                minecraft.setScreenAndShow(new HueBlockPickerScreen(this, candidates(), model::setBlock)));
        if (model.selectedNode < model.nodes.size() - 1) {
            int y = l.bodyTop() + 160 - settingsScroll;
            if (y >= l.bodyTop() && y + 20 <= l.bodyBottom()) {
                EditBox count = new EditBox(font, x + 38, y, w - 38, 20, text("steps"));
                count.setMaxLength(3); count.setValue(node.editedSteps); count.setResponder(value -> editCount(node, count, value));
                count.setTooltip(Tooltip.create(text("steps_hint"))); addRenderableWidget(count);
            }
        }
        settingAction(x, 202, 72, Component.literal(model.oklab ? "OkLAB" : "RGB"), () -> {
            model.oklab = !model.oklab; model.invalidate(); rebuildWidgets();
        });
        settingDropdown(x + 76, 202, w - 76, text("face." + model.face), () ->
                menu(x, l.bodyTop() + 202 - settingsScroll, FACES.stream().map(face -> new MenuEntry(text("face." + face), () -> {
                    model.face = face; model.invalidate(); rebuildWidgets();
                })).toList()));
        settingDropdown(x, 226, w, paletteLabel(), this::choosePalette);
        settingDropdown(x, 250, w, groupLabel(model.sourceGroup), () -> chooseGroup(true));
        generateButton = settingAction(x, 278, w, text("generate"), this::generate);
        if (l.settingsScrollMax() > 0) {
            action(x + l.settingsWidth() - 12, l.bodyTop(), 12, Component.literal("↑"), () -> scrollSettings(-24));
            action(x + l.settingsWidth() - 12, l.bodyBottom() - 20, 12, Component.literal("↓"), () -> scrollSettings(24));
        }
    }
    private void editCount(GradientWorkbench.Node node, EditBox input, String value) {
        var result = model.editSteps(node, value, false);
        if (result == GradientWorkbench.CountEdit.CONFIRM_LOCK_LOSS) {
            // Restore the last committed count before opening the existing lock-loss confirmation.
            input.setValue(node.steps);
            confirm(work("clear_locks_hint"), work("clear_continue"), () -> {
                model.editSteps(node, value, true);
                page = textureOffset = crossOffset = 0; editing = false;
                rebuildWidgets();
            });
        } else if (result == GradientWorkbench.CountEdit.CHANGED) {
            page = textureOffset = crossOffset = 0; editing = false;
        }
    }
    private void structural(Runnable change) {
        Runnable apply = () -> {
            change.run(); page = textureOffset = crossOffset = 0; editing = false;
            nodeStart = Math.max(0, model.selectedNode - 2); rebuildWidgets();
        };
        if (model.hasLocks()) confirm(work("clear_locks_hint"), work("clear_continue"), apply); else apply.run();
    }
    private void moveNode(int delta) {
        int next = model.selectedNode + delta;
        if (next >= 0 && next < model.nodes.size()) structural(() -> model.structural(() -> {
            Collections.swap(model.nodes, model.selectedNode, next); model.selectedNode = next;
        }));
    }
    private void initResults() {
        var l = layout();
        dropdown(l.resultLeft(), l.bodyTop(), 84, work(textures ? "tile_view" : "sequence_view"), () -> menu(l.resultLeft(), l.bodyTop() + 22, List.of(
                new MenuEntry(work("sequence_view"), () -> { textures = false; rebuildWidgets(); }),
                new MenuEntry(work("tile_view"), () -> { textures = true; rebuildWidgets(); }))));
        dropdown(l.resultLeft() + 88, l.bodyTop(), 80, work("view_options"), () -> {
            List<MenuEntry> options = new ArrayList<>();
            if (textures) {
                options.add(new MenuEntry(work(vertical ? "vertical" : "horizontal"), () -> { vertical = !vertical; textureOffset = crossOffset = 0; }));
                for (int size : new int[]{16, 24, 32}) options.add(new MenuEntry(work("tile_size", size), () -> { cell = size; textureOffset = crossOffset = 0; }));
                options.add(new MenuEntry(work("cross_previous"), () -> crossOffset = Math.max(0, crossOffset - cell)));
                options.add(new MenuEntry(work("cross_next"), () -> crossOffset += cell));
            } else {
                options.add(new MenuEntry(text(hideConsecutive ? "hide_repeats_on" : "hide_repeats_off"), () -> {
                    hideConsecutive = !hideConsecutive; editing = false; page = 0;
                }));
                options.add(new MenuEntry(work(editing ? "stop_editing" : "edit_results"), () -> {
                    editing = !editing;
                    if (editing && !model.samples().isEmpty()) minecraft.setScreenAndShow(new GradientInspectorScreen(this,
                            Math.min(model.samples().size() - 1, page * layout().pageSize())));
                }));
            }
            options.add(new MenuEntry(work("unlock_all"), model::unlockAll, model.hasLocks()));
            menu(l.resultLeft() + 88, l.bodyTop() + 22, options);
        });
        if (!textures) {
            action(l.resultLeft() + l.resultWidth() - 52, l.bodyTop(), 24, Component.literal("<"), () -> changePage(-1));
            action(l.resultLeft() + l.resultWidth() - 24, l.bodyTop(), 24, Component.literal(">"), () -> changePage(1));
        }
    }
    private Component paletteLabel() {
        if (model.palette.equals("all")) return text("palette.all");
        if (model.palette.equals("group")) return text("palette.group");
        return paletteName(model.palette);
    }
    private Component paletteName(String name) {
        return switch (name) {
            case "Default (opaque only)" -> text("palette.opaque");
            case "Build-friendly blocks" -> text("palette.build");
            case "Grayscale blocks" -> text("palette.gray");
            case "Overworld natural blocks" -> text("palette.overworld");
            case "Nether + End blocks" -> text("palette.dimensions");
            default -> Component.literal(name);
        };
    }
    private void choosePalette() {
        List<MenuEntry> entries = new ArrayList<>();
        entries.add(new MenuEntry(text("palette.all"), () -> setPalette("all")));
        entries.add(new MenuEntry(text("palette.group"), () -> { setPalette("group"); chooseGroup(true); }));
        var data = HueBlocksRuntime.repository().data();
        if (data != null) for (var palette : data.palettes()) entries.add(new MenuEntry(paletteName(palette.name()), () -> setPalette(palette.name())));
        menu(layout().left(), layout().bodyTop(), entries);
    }
    private void setPalette(String name) { model.palette = name; model.invalidate(); rebuildWidgets(); }
    private Component groupLabel(GroupRef ref) {
        var group = session.wheelState.resolve(ref);
        return group == null ? work("choose_group") : Component.literal((ref.primary() ? "I · " : "II · ") + group.displayName());
    }
    private void chooseGroup(boolean source) {
        List<MenuEntry> entries = new ArrayList<>();
        for (boolean primary : new boolean[]{true, false}) for (var group : (primary ? session.wheelState.primary : session.wheelState.secondary).groups()) {
            GroupRef ref = new GroupRef(primary, group.id());
            entries.add(new MenuEntry(groupLabel(ref), () -> {
                if (source) { model.sourceGroup = ref; model.palette = "group"; model.invalidate(); } else model.targetGroup = ref;
                rebuildWidgets();
            }));
        }
        if (entries.isEmpty()) message = text("select_group"); else menu(width / 2 - 112, layout().bodyTop(), entries);
    }
    List<HueGradient.Candidate> candidates() {
        var data = HueBlocksRuntime.repository().data(); if (data == null) return List.of();
        Set<String> textures = null, ids = null;
        if (model.palette.equals("group")) {
            var group = session.wheelState.resolve(model.sourceGroup); if (group == null) return List.of();
            ids = group.members().stream().map(PaletteMember::itemId).collect(Collectors.toSet());
        } else if (!model.palette.equals("all")) {
            var palette = data.palettes().stream().filter(p -> p.name().equals(model.palette)).findFirst();
            if (palette.isEmpty()) return List.of(); textures = palette.get().textures();
        }
        final Set<String> allowedTextures = textures, allowedIds = ids;
        return HueBlocksRuntime.candidates().stream().filter(c -> c.block().faces(model.face)
                && (allowedTextures == null || allowedTextures.contains(c.block().texture()))
                && (allowedIds == null || allowedIds.contains(c.itemId()))).toList();
    }
    private void useHeld() {
        if (minecraft.player == null) { message = text("held_missing"); return; }
        String id = ClientPaletteItemCodec.itemId(minecraft.player.getInventory().getSelectedItem());
        var candidate = candidates().stream().filter(c -> c.itemId().equals(id)).findFirst();
        if (candidate.isPresent()) { model.setBlock(candidate.get()); rebuildWidgets(); } else message = text("held_missing");
    }
    private void generate() {
        if (!model.valid() || model.pendingSteps()) { message = work("pending_count"); return; }
        var available = candidates();
        if (model.generate(available)) {
            previousCandidates = available;
            message = text("generated", model.samples().size()); page = 0; resultTab = true; rebuildWidgets();
        } else message = work("lock_conflicts");
    }
    @Override public void tick() {
        var current = HueBlocksRuntime.repository().data(); var available = candidates();
        if (current != widgetData || !previousCandidates.equals(available)) {
            if (!model.samples().isEmpty() && !model.stale()) model.invalidate(); widgetData = current;
        }
        previousCandidates = available; updateAvailability();
    }
    private void updateAvailability() {
        var available = candidates();
        boolean ready = model.valid() && !model.pendingSteps() && !available.isEmpty() && model.conflicts(available).isEmpty();
        if (generateButton != null) generateButton.active = ready;
        boolean distinct = model.results().stream().map(HueGradient.Candidate::itemId).distinct().limit(2).count() >= 2;
        applyButton.active = ready && !model.stale() && !model.samples().isEmpty() && switch (target) {
            case TEMPORARY -> true;
            case INVENTORY -> CreativeInventoryHelper.canApply(minecraft) && model.samples().size() <= 36;
            case CREATE -> distinct;
            case REPLACE -> distinct && session.wheelState.resolve(model.targetGroup) != null;
        };
        applyButton.setTooltip(Tooltip.create(targetHint())); restoreButton.active = CreativeInventoryHelper.canRestore(minecraft);
        refreshButton.active = !HueBlocksRuntime.repository().checking();
    }
    private Component targetHint() {
        if (model.pendingSteps()) return work("pending_count");
        if (!model.conflicts(candidates()).isEmpty()) return work("lock_conflicts");
        if (model.stale()) return text("preview_stale");
        return switch (target) {
            case TEMPORARY -> text("temporary_hint");
            case INVENTORY -> !CreativeInventoryHelper.canApply(minecraft) ? text("creative_required")
                    : model.samples().size() > 36 ? text("inventory_too_many") : text("inventory_hint");
            case CREATE -> text("create_group");
            case REPLACE -> session.wheelState.resolve(model.targetGroup) == null ? text("select_group")
                    : text("replace_hint", session.wheelState.resolve(model.targetGroup).displayName());
        };
    }
    private void apply() {
        updateAvailability(); if (!applyButton.active) return;
        var items = model.results().stream().map(HueGradient.Candidate::itemId).toList();
        if (target == ApplyTarget.TEMPORARY) {
            CreativeInventoryHelper.setTemporaryPalette(items); model.applied(); message = text("temporary_applied", items.size());
        } else if (target == ApplyTarget.INVENTORY) {
            if (CreativeInventoryHelper.applyGradient(minecraft, model.results().stream().map(HueBlocksRuntime::stack).toList())) {
                model.applied(); message = text("inventory_applied", items.size());
            }
        } else {
            var editor = session.wheel(); if (target == ApplyTarget.REPLACE) editor.selectRef(model.targetGroup);
            var distinct = items.stream().distinct().toList(); editor.applyGradient(distinct, target == ApplyTarget.CREATE, items.size() - distinct.size());
            model.applied(); session.show("wheel");
        }
    }
    @Override protected void renderWorkbench(GuiGraphicsExtractor graphics, int mx, int my, float tick) {
        var l = layout(); graphics.text(font, title, l.left(), 10, 0xFFFFFFFF);
        Component status = !model.conflicts(candidates()).isEmpty() ? work("lock_conflicts")
                : model.pendingSteps() ? work("pending_count") : !model.valid() ? text("invalid_inputs") : dataStatus();
        if (minecraft.level == null) status = Component.translatable("palette_editor.lorian_arch_orbit.world_required");
        boundedText(graphics, status, l.left(), l.compact() ? 56 : 34,
                l.compact() ? l.width() - 72 : l.settingsWidth(), 0xFFFFC14D, mx, my);
        if (!l.compact()) boundedText(graphics, model.samples().isEmpty() ? text("preview")
                : work("result_count", model.samples().size(), model.results().stream().map(HueGradient.Candidate::itemId).distinct().count())
                .copy().append(model.stale() ? " *" : ""), l.resultLeft(), 34, l.resultWidth() - 72, 0xFFBBBBBB, mx, my);
        if (settingsVisible()) renderSettings(graphics, mx, my);
        if (resultsVisible()) {
            graphics.enableScissor(l.resultLeft(), l.previewTop(), l.resultLeft() + l.resultWidth(), l.bodyBottom());
            try {
                if (model.samples().isEmpty()) renderEmpty(graphics);
                else if (textures) renderTiles(graphics, mx, my); else renderSequence(graphics, mx, my);
            } finally { graphics.disableScissor(); }
        }
        Component feedback = message;
        if (model.stale() && !model.samples().isEmpty()) feedback = text("preview_stale");
        else if (feedback.getString().isEmpty() && !model.samples().isEmpty()) feedback = work("result_count", model.samples().size(),
                model.results().stream().map(HueGradient.Candidate::itemId).distinct().count());
        if (editing) feedback = work("editing_all").copy().append(" · ").append(feedback);
        if (target == ApplyTarget.REPLACE) feedback = groupLabel(model.targetGroup).copy().append(" · ").append(feedback);
        boundedText(graphics, feedback, l.left(), l.footerTop() - 14, l.width(), 0xFFBBBBBB, mx, my);
    }
    private void renderSettings(GuiGraphicsExtractor graphics, int mx, int my) {
        var l = layout(); int x = l.left(), w = l.settingsWidth() - 16;
        graphics.enableScissor(x, l.bodyTop(), x + w, l.bodyBottom());
        try {
            int y = l.bodyTop() - settingsScroll;
            graphics.text(font, work("nodes", model.selectedNode + 1, model.nodes.size()), x, y + 2, 0xFFBBBBBB);
            for (int i = 0; i < nodeButtons.size(); i++) {
                var n = model.nodes.get(nodeButtonStart + i); var b = nodeButtons.get(i);
                graphics.fill(b.getX() + 2, b.getY() + 17, b.getX() + b.getWidth() - 2, b.getY() + 19, 0xFF000000 | parseRgb(n.hex));
                if (n.pinned != null) {
                    graphics.item(HueBlocksRuntime.stack(n.pinned), b.getX() + 8, b.getY() + 1);
                    graphics.text(font, Integer.toString(nodeButtonStart + i + 1), b.getX() + 2, b.getY() + 2, 0xFFFFFFFF);
                }
            }
            graphics.text(font, text("color"), x, y + 96, 0xFFBBBBBB);
            graphics.text(font, model.selectedNode < model.nodes.size() - 1 ? text("steps_label") : work("last_node"), x, y + 166, 0xFFBBBBBB);
            boundedText(graphics, model.valid() ? work("expected", model.expectedCount()) : text("invalid_inputs"), x, y + 186, w, 0xFFBBBBBB, mx, my);
        } finally { graphics.disableScissor(); }
    }
    private void renderEmpty(GuiGraphicsExtractor graphics) {
        var l = layout(); int y = l.previewTop(), w = l.resultWidth();
        var targets = model.valid() ? HueGradient.targets(model.stops()) : List.<HueGradient.Target>of();
        for (int i = 0; i < w; i++) {
            int color = targets.isEmpty() ? 0x555555 : targets.get(Math.min(targets.size() - 1, i * targets.size() / w)).displayRgb(model.oklab);
            graphics.fill(l.resultLeft() + i, y, l.resultLeft() + i + 1, y + 12, 0xFF000000 | color);
        }
        graphics.text(font, work("empty_guidance"), l.resultLeft() + 4, y + 18, 0xFFBBBBBB);
    }
    private List<GradientWorkbench.Sample> visibleSamples() { return model.preview(hideConsecutive && !editing); }
    private void renderSequence(GuiGraphicsExtractor graphics, int mx, int my) {
        var l = layout(); var samples = visibleSamples(); page = Math.min(page, maxPage());
        for (int slot = 0; slot < l.pageSize() && page * l.pageSize() + slot < samples.size(); slot++) {
            var sample = samples.get(page * l.pageSize() + slot);
            int x = l.resultLeft() + slot % l.columns() * 24, y = l.previewTop() + slot / l.columns() * 24;
            graphics.fill(x, y, x + 22, y + 22, sample.locked() ? 0xAA365E43 : 0x88202020);
            graphics.item(HueBlocksRuntime.stack(sample.candidate()), x + 3, y + 2);
            graphics.fill(x + 2, y + 20, x + 20, y + 22, 0xFF000000 | sample.candidate().block().rgb());
            if (sample.locked()) graphics.text(font, "*", x + 15, y, 0xFFFFC14D);
            if (inside(mx, my, x, y, 22, 22)) sampleTooltip(graphics, sample, mx, my);
        }
    }
    private TextureTilingLayout tiling() {
        var l = layout(); int h = l.bodyBottom() - l.previewTop();
        var tile = TextureTilingLayout.calculate(model.samples().size(), cell, vertical ? h : l.resultWidth(),
                vertical ? l.resultWidth() : h, textureOffset, crossOffset);
        textureOffset = tile.offset(); crossOffset = tile.crossOffset(); return tile;
    }
    private void renderTiles(GuiGraphicsExtractor graphics, int mx, int my) {
        var l = layout(); var tile = tiling();
        for (int i = tile.first(); i < tile.last(); i++) {
            var sample = model.samples().get(i); String texture = sample.candidate().block().texture();
            var sprite = graphics.getSprite(new SpriteId(TextureAtlas.LOCATION_BLOCKS,
                    Identifier.withDefaultNamespace("block/" + texture.substring(0, texture.length() - 4))));
            for (int cross = tile.firstCross(); cross < tile.lastCross(); cross++) {
                int along = i * cell - tile.offset(), across = cross * cell - tile.crossOffset();
                int x = l.resultLeft() + (vertical ? across : along), y = l.previewTop() + (vertical ? along : across);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, cell, cell);
                if (inside(mx, my, x, y, cell, cell)) {
                    if (sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))
                        graphics.setTooltipForNextFrame(font, work("missing_texture"), mx, my);
                    else sampleTooltip(graphics, sample, mx, my);
                }
            }
        }
    }
    private void sampleTooltip(GuiGraphicsExtractor graphics, GradientWorkbench.Sample sample, int x, int y) {
        Component tooltip = text("block_hint", HueBlocksRuntime.stack(sample.candidate()).getHoverName(), sample.candidate().itemId(), sample.candidate().block().texture())
                .copy().append("\n").append(work("sample_position", sample.index() + 1));
        if (textures) tooltip = tooltip.copy().append("\n").append(work("texture_hint"));
        graphics.setTooltipForNextFrame(font, font.split(tooltip, Math.min(320, width - 24)), x, y);
    }
    @Override protected boolean clickWorkbench(MouseButtonEvent event, boolean twice) {
        if (super.clickWorkbench(event, twice)) return true;
        var l = layout(); int x = (int)event.x(), y = (int)event.y();
        if (event.button() != 0 || !resultsVisible() || !inside(x, y, l.resultLeft(), l.previewTop(), l.resultWidth(), l.bodyBottom() - l.previewTop())) return false;
        int index;
        if (textures) {
            tiling(); int cross = (vertical ? x - l.resultLeft() : y - l.previewTop()) + crossOffset;
            if (cross >= 4 * cell) return true;
            index = ((vertical ? y - l.previewTop() : x - l.resultLeft()) + textureOffset) / cell;
        } else {
            int col = (x - l.resultLeft()) / 24, row = (y - l.previewTop()) / 24;
            if (col >= l.columns() || row >= l.rows()) return true;
            int offset = page * l.pageSize() + row * l.columns() + col;
            var samples = visibleSamples(); if (offset >= samples.size()) return true; index = samples.get(offset).index();
        }
        if (index >= 0 && index < model.samples().size()) {
            editing = true; page = index / l.pageSize(); minecraft.setScreenAndShow(new GradientInspectorScreen(this, index));
        }
        return true;
    }
    @Override protected boolean scrollWorkbench(double x, double y, double ax, double ay) {
        var l = layout();
        if (settingsVisible() && inside((int)x, (int)y, l.left(), l.bodyTop(), l.settingsWidth(), l.bodyBottom() - l.bodyTop())) {
            if (y < l.bodyTop() + 38 - settingsScroll) { nodeStart += ay < 0 ? 1 : -1; rebuildWidgets(); }
            else scrollSettings(ay < 0 ? 24 : -24); return true;
        }
        if (resultsVisible() && y >= l.previewTop() && y < l.bodyBottom()) {
            if (textures) { textureOffset = Math.max(0, textureOffset + (ay < 0 ? cell : -cell)); crossOffset = Math.max(0, crossOffset + (int)(-ax * cell)); }
            else changePage(ay < 0 ? 1 : -1); return true;
        }
        return super.scrollWorkbench(x, y, ax, ay);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (menuOpen()) return super.keyPressed(event);
        if (settingsVisible() && (event.key() == 266 || event.key() == 267)) { scrollSettings(event.key() == 266 ? -48 : 48); return true; }
        if (textures && resultsVisible() && !(getFocused() instanceof EditBox) && event.key() >= 262 && event.key() <= 265) {
            int dx = event.key() == 262 ? cell : event.key() == 263 ? -cell : 0, dy = event.key() == 264 ? cell : event.key() == 265 ? -cell : 0;
            textureOffset = Math.max(0, textureOffset + (vertical ? dy : dx)); crossOffset = Math.max(0, crossOffset + (vertical ? dx : dy)); return true;
        }
        return super.keyPressed(event);
    }
    private void scrollSettings(int amount) { settingsScroll = Math.clamp(settingsScroll + amount, 0, layout().settingsScrollMax()); rebuildWidgets(); }
    private void changePage(int delta) { page = Math.clamp(page + delta, 0, maxPage()); }
    private int maxPage() { return Math.max(0, (visibleSamples().size() - 1) / layout().pageSize()); }
    private static boolean inside(int x, int y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private static int parseRgb(String value) { try { return Integer.parseInt(value.replace("#", ""), 16); } catch (NumberFormatException ex) { return 0; } }
    private Component dataStatus() {
        var repository = HueBlocksRuntime.repository();
        if (repository.checking()) return text(repository.data() == null ? "loading" : "checking_cached");
        return switch (repository.state()) {
            case READY -> candidates().isEmpty() ? text("no_candidates") : text("ready", repository.data().blocks().size());
            case CACHED -> text("cached"); case UNSUPPORTED -> text("unsupported"); default -> text("download_failed");
        };
    }
    private enum ApplyTarget {
        TEMPORARY("temporary_wheel"), INVENTORY("inventory"), CREATE("create_group"), REPLACE("replace_group");
        final String key; ApplyTarget(String key) { this.key = key; }
    }
}
