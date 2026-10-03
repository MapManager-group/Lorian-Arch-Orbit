package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.davidblackcn.lorianarchorbit.palette.TemporaryPaletteSession;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksData;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Independent gradient workbench with draft, temporary wheel and reversible inventory targets. */
final class HueGradientScreen extends Screen {
    private static final List<String> FACES = List.of("all", "sides", "top", "bottom", "north", "south", "east", "west");
    private final PaletteEditorScreen parent;
    private final PaletteGroup group;
    private final List<Node> nodes = new ArrayList<>();
    private int selectedNode;
    private boolean oklab = true;
    private String face = "all";
    private int palette = -1;
    private List<HueGradient.Candidate> results = List.of();
    private List<HueGradient.Candidate> previewResults = List.of();
    private boolean hideConsecutive = true;
    private int page;
    private boolean dirty = true;
    private Component message = Component.empty();
    private HueBlocksData widgetData;
    private CycleButton<Integer> paletteButton;
    private Button generateButton;
    private ApplyTarget applyTarget = ApplyTarget.TEMPORARY;
    private Button applyButton;
    private Button restoreButton;
    private Button pickButton;
    private Button heldButton;
    private Button refreshButton;
    private Button previousPage;
    private Button nextPage;

    HueGradientScreen(PaletteEditorScreen parent, PaletteGroup group) {
        super(text("title"));
        this.parent = parent;
        this.group = group;
        nodes.add(new Node("E0C9A2", "8"));
        nodes.add(new Node("4B5E73", "8"));
        HueBlocksRuntime.initialize();
    }

    static Component text(String key, Object... args) {
        return Component.translatable("hueblocks.lorian_arch_orbit." + key, args);
    }

    @Override
    protected void init() {
        HueGradientLayout layout = layout();
        int left = layout.left();
        int panel = layout.width();
        restoreButton = button(left + panel - 112, 22, 64, text("restore_inventory"), b -> {
            if (CreativeInventoryHelper.restore(minecraft)) message = text("inventory_restored");
            updateAvailability();
        });
        restoreButton.setTooltip(Tooltip.create(text("restore_inventory_hint")));
        refreshButton = button(left + panel - 44, 22, 44, text("refresh"), b -> HueBlocksRuntime.repository().refresh());
        button(left, 42, 24, Component.literal("<"), b -> selectNode(-1))
                .setTooltip(Tooltip.create(text("previous_node")));
        button(left + 84, 42, 24, Component.literal(">"), b -> selectNode(1))
                .setTooltip(Tooltip.create(text("next_node")));
        button(left + 112, 42, 48, text("add_node"), b -> {
            if (nodes.size() >= HueGradient.MAX_STOPS) return;
            nodes.add(selectedNode + 1, new Node(nodes.get(selectedNode).hex, nodes.get(selectedNode).steps));
            selectedNode++;
            invalidate();
            rebuildWidgets();
        }).active = nodes.size() < HueGradient.MAX_STOPS;
        button(left + 164, 42, 48, text("remove_node"), b -> {
            if (nodes.size() <= 2) return;
            nodes.remove(selectedNode);
            selectedNode = Math.min(selectedNode, nodes.size() - 1);
            invalidate();
            rebuildWidgets();
        }).active = nodes.size() > 2;
        button(left + 216, 42, Math.min(80, panel - 216), text("reverse"), b -> reverse());

        Node node = nodes.get(selectedNode);
        EditBox color = new EditBox(font, left + 28, 66, 80, 20, text("color"));
        color.setMaxLength(7);
        color.setHint(Component.literal("#RRGGBB"));
        color.setTooltip(Tooltip.create(text("color_hint")));
        color.setValue(node.hex);
        color.setResponder(value -> { node.hex = value; node.pinned = null; invalidate(); });
        addRenderableWidget(color);
        EditBox steps = new EditBox(font, left + 132, 66, 40, 20, text("steps"));
        steps.setMaxLength(3);
        steps.setValue(selectedNode == nodes.size() - 1 ? "—" : node.steps);
        steps.setTooltip(Tooltip.create(text("steps_hint")));
        steps.setResponder(value -> { node.steps = value; invalidate(); });
        steps.active = selectedNode < nodes.size() - 1;
        addRenderableWidget(steps);
        int pickWidth = (panel - 180 - 4) / 2;
        heldButton = button(left + 176, 66, pickWidth, text("held_block"), b -> useHeld());
        pickButton = button(left + 180 + pickWidth, 66, panel - 180 - pickWidth, text("pick_block"), b -> {
            List<HueGradient.Candidate> available = filteredCandidates();
            minecraft.setScreenAndShow(new HueBlockPickerScreen(this, available, candidate -> setBlock(candidate)));
        });

        int colorWidth = 74;
        int faceWidth = 76;
        addRenderableWidget(CycleButton.booleanBuilder(Component.literal("OkLAB"), Component.literal("RGB"), oklab)
                .displayOnlyValue().withTooltip(v -> Tooltip.create(text("color_space_hint")))
                .create(left, 90, colorWidth, 20, text("color_space"), (b, value) -> { oklab = value; invalidate(); }));
        addRenderableWidget(CycleButton.builder(value -> text("face." + value), face).withValues(FACES)
                .displayOnlyValue().withTooltip(v -> Tooltip.create(text("face_hint")))
                .create(left + colorWidth + 4, 90, faceWidth, 20, text("facing"), (b, value) -> { face = value; invalidate(); }));
        widgetData = HueBlocksRuntime.repository().data();
        rebuildPaletteButton();

        addRenderableWidget(CycleButton.booleanBuilder(text("hide_repeats_on"), text("hide_repeats_off"), hideConsecutive)
                .displayOnlyValue().withTooltip(value -> Tooltip.create(text("hide_repeats_hint")))
                .create(left + panel - 156, 114, 100, 16, text("hide_repeats"), (b, value) -> {
                    hideConsecutive = value;
                    updatePreview();
                    updateAvailability();
                }));
        previousPage = button(left + panel - 52, 114, 24, Component.literal("<"), b -> changePage(-1));
        previousPage.setHeight(16);
        previousPage.setTooltip(Tooltip.create(text("previous_page")));
        nextPage = button(left + panel - 24, 114, 24, Component.literal(">"), b -> changePage(1));
        nextPage.setHeight(16);
        nextPage.setTooltip(Tooltip.create(text("next_page")));
        int footerWidth = (panel - 12) / 4;
        generateButton = button(left, layout.footerTop(), footerWidth, text("generate"), b -> generate());
        addRenderableWidget(CycleButton.builder(target -> text(target.key), applyTarget).withValues(ApplyTarget.values())
                .displayOnlyValue().withTooltip(target -> Tooltip.create(targetHint(target)))
                .create(left + footerWidth + 4, layout.footerTop(), footerWidth, 20, text("apply_target"), (b, value) -> {
                    applyTarget = value;
                    updateAvailability();
                }));
        applyButton = button(left + 2 * (footerWidth + 4), layout.footerTop(), footerWidth, text("apply"), b -> apply());
        button(left + 3 * (footerWidth + 4), layout.footerTop(), panel - 3 * (footerWidth + 4), text("back"), b -> onClose());
        setInitialFocus(color);
        updateAvailability();
    }

    private void rebuildPaletteButton() {
        if (paletteButton != null) removeWidget(paletteButton);
        List<Integer> values = new ArrayList<>(List.of(-1));
        if (group != null) values.add(-2);
        HueBlocksData data = HueBlocksRuntime.repository().data();
        if (data != null) for (int i = 0; i < data.palettes().size(); i++) values.add(i);
        if (!values.contains(palette)) palette = -1;
        int x = layout().left() + 158;
        paletteButton = addRenderableWidget(CycleButton.<Integer>builder(this::paletteLabel, (java.util.function.Supplier<Integer>) () -> palette).withValues(values)
                .displayOnlyValue().withTooltip(value -> Tooltip.create(paletteLabel(value)))
                .create(x, 90, layout().width() - 158, 20, text("palette"), (button, value) -> {
                    palette = value;
                    invalidate();
                }));
    }

    private Component paletteLabel(int index) {
        if (index == -1) return text("palette.all");
        if (index == -2) return text("palette.group");
        HueBlocksData data = HueBlocksRuntime.repository().data();
        if (data == null || index >= data.palettes().size()) return text("palette.all");
        return switch (data.palettes().get(index).name()) {
            case "Default (opaque only)" -> text("palette.opaque");
            case "Build-friendly blocks" -> text("palette.build");
            case "Grayscale blocks" -> text("palette.gray");
            case "Overworld natural blocks" -> text("palette.overworld");
            case "Nether + End blocks" -> text("palette.dimensions");
            default -> Component.literal(data.palettes().get(index).name());
        };
    }

    private Button button(int x, int y, int width, Component label, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(x, y, width, 20).build());
    }

    @Override
    public void tick() {
        if (widgetData != HueBlocksRuntime.repository().data()) {
            widgetData = HueBlocksRuntime.repository().data();
            // Invalidate results, but preserve every node input and text-field focus while refreshing choices.
            invalidate();
            rebuildPaletteButton();
        }
        updateAvailability();
    }

    private void updateAvailability() {
        List<HueGradient.Candidate> candidates = filteredCandidates();
        generateButton.active = validNodes() && !candidates.isEmpty();
        boolean distinct = results.stream().map(HueGradient.Candidate::itemId).distinct().limit(2).count() >= 2;
        applyButton.active = !dirty && !results.isEmpty() && switch (applyTarget) {
            case TEMPORARY -> true;
            case INVENTORY -> CreativeInventoryHelper.canApply(minecraft) && results.size() <= TemporaryPaletteSession.INVENTORY_SIZE;
            case CREATE -> distinct;
            case REPLACE -> distinct && group != null;
        };
        applyButton.setTooltip(Tooltip.create(dirty ? text("preview_stale") : targetHint(applyTarget)));
        restoreButton.active = CreativeInventoryHelper.canRestore(minecraft);
        pickButton.active = !candidates.isEmpty();
        heldButton.active = minecraft.player != null && !candidates.isEmpty();
        refreshButton.active = !HueBlocksRuntime.repository().checking();
        previousPage.active = page > 0;
        nextPage.active = page < maxPage();
    }

    private Component targetHint(ApplyTarget target) {
        return switch (target) {
            case TEMPORARY -> text("temporary_hint");
            case INVENTORY -> !CreativeInventoryHelper.canApply(minecraft) ? text("creative_required")
                    : results.size() > TemporaryPaletteSession.INVENTORY_SIZE ? text("inventory_too_many") : text("inventory_hint");
            case CREATE -> !results.isEmpty() && results.stream().map(HueGradient.Candidate::itemId).distinct().limit(2).count() < 2
                    ? text("too_few") : text("create_group");
            case REPLACE -> group == null ? text("select_group") : text("replace_hint", group.displayName());
        };
    }

    private List<HueGradient.Candidate> filteredCandidates() {
        HueBlocksData data = HueBlocksRuntime.repository().data();
        Set<String> textures = data != null && palette >= 0 && palette < data.palettes().size()
                ? data.palettes().get(palette).textures() : null;
        Set<String> groupItems = palette == -2 && group != null
                ? group.members().stream().map(PaletteMember::itemId).collect(Collectors.toSet()) : null;
        return HueBlocksRuntime.candidates().stream().filter(c -> c.block().faces(face)
                && (textures == null || textures.contains(c.block().texture()))
                && (groupItems == null || groupItems.contains(c.itemId()))).toList();
    }

    private void selectNode(int delta) {
        selectedNode = Math.floorMod(selectedNode + delta, nodes.size());
        rebuildWidgets();
    }

    private void reverse() {
        List<String> lengths = nodes.subList(0, nodes.size() - 1).stream().map(n -> n.steps).toList();
        Collections.reverse(nodes);
        for (int i = 0; i < nodes.size() - 1; i++) nodes.get(i).steps = lengths.get(lengths.size() - 1 - i);
        selectedNode = nodes.size() - 1 - selectedNode;
        invalidate();
        rebuildWidgets();
    }

    private void setBlock(HueGradient.Candidate candidate) {
        Node node = nodes.get(selectedNode);
        node.hex = String.format(Locale.ROOT, "%06X", candidate.block().rgb());
        node.pinned = candidate;
        invalidate();
    }

    private void useHeld() {
        if (minecraft.player == null) return;
        String id = ClientPaletteItemCodec.itemId(minecraft.player.getInventory().getSelectedItem());
        var candidate = filteredCandidates().stream().filter(c -> c.itemId().equals(id)).findFirst();
        if (candidate.isEmpty()) {
            message = text("held_missing");
        } else {
            setBlock(candidate.get());
            rebuildWidgets();
        }
    }

    private boolean validNodes() {
        try { stops(); return true; } catch (IllegalArgumentException exception) { return false; }
    }

    private List<HueGradient.Stop> stops() {
        List<HueGradient.Stop> stops = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String hex = node.hex.startsWith("#") ? node.hex.substring(1) : node.hex;
            if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use #RRGGBB");
            int steps = i == nodes.size() - 1 ? 2 : Integer.parseInt(node.steps);
            stops.add(new HueGradient.Stop(Integer.parseInt(hex, 16), steps, node.pinned));
        }
        return List.copyOf(stops);
    }

    private void invalidate() {
        dirty = true;
        message = Component.empty();
    }

    private void generate() {
        try {
            results = HueGradient.generate(stops(), filteredCandidates(), oklab);
            updatePreview();
            dirty = false;
            message = results.isEmpty() ? text("no_candidates")
                    : text("generated", results.size());
            updateAvailability();
        } catch (IllegalArgumentException exception) {
            message = text("invalid_inputs");
        }
    }

    private void updatePreview() {
        previewResults = HueGradient.preview(results, hideConsecutive);
        page = 0;
    }

    private void apply() {
        if (dirty || results.isEmpty()) return;
        if (applyTarget == ApplyTarget.TEMPORARY) {
            CreativeInventoryHelper.setTemporaryPalette(results.stream().map(HueGradient.Candidate::itemId).toList());
            message = text("temporary_applied", results.size());
            updateAvailability();
            return;
        }
        if (applyTarget == ApplyTarget.INVENTORY) {
            if (CreativeInventoryHelper.applyGradient(minecraft, results.stream().map(HueBlocksRuntime::stack).toList())) {
                message = text("inventory_applied", results.size());
            } else message = targetHint(applyTarget);
            updateAvailability();
            return;
        }
        boolean create = applyTarget == ApplyTarget.CREATE;
        if (!create && group == null) return;
        List<String> items = results.stream().map(HueGradient.Candidate::itemId).distinct().toList();
        if (items.size() < 2) {
            message = text("too_few");
            return;
        }
        PaletteEditorScreen editor = parent == null ? new PaletteEditorScreen(null) : parent;
        editor.applyGradient(items, create, results.size() - items.size());
        minecraft.setScreenAndShow(editor);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        HueGradientLayout layout = layout();
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        Component status = !message.getString().isEmpty() ? message
                : HueBlocksRuntime.repository().data() != null && filteredCandidates().isEmpty() ? text("no_candidates") : dataStatus();
        if (!validNodes()) status = text("invalid_inputs");
        graphics.text(font, font.plainSubstrByWidth(status.getString(), layout.width() - 116), layout.left(), 27, 0xFFFFC14D);
        if (mouseY >= 22 && mouseY < 42 && mouseX >= layout.left() && mouseX < layout.left() + layout.width() - 116) {
            graphics.setTooltipForNextFrame(font, status, mouseX, mouseY);
        }
        graphics.centeredText(font, (selectedNode + 1) + " / " + nodes.size(), layout.left() + 56, 48, 0xFFFFFFFF);
        graphics.text(font, text("color_label"), layout.left(), 72, 0xFFBBBBBB);
        graphics.text(font, text("steps_label"), layout.left() + 112, 72, 0xFFBBBBBB);
        int rgb = parsePreviewRgb(nodes.get(selectedNode).hex);
        graphics.fill(layout.left() + 30, 84, layout.left() + 106, 86, 0xFF000000 | rgb);
        Component label = text(results.isEmpty() ? "preview" : dirty ? "preview_stale" : "preview_count",
                previewResults.size(), results.size(), page + 1, maxPage() + 1);
        graphics.text(font, font.plainSubstrByWidth(label.getString(), layout.width() - 160), layout.left(), 117, 0xFFBBBBBB);
        if (mouseY >= 114 && mouseY < 130 && mouseX >= layout.left() && mouseX < layout.left() + layout.width() - 160) {
            graphics.setTooltipForNextFrame(font, label, mouseX, mouseY);
        }
        page = Math.min(page, maxPage());
        for (int slot = 0; slot < layout.pageSize() && page * layout.pageSize() + slot < previewResults.size(); slot++) {
            var candidate = previewResults.get(page * layout.pageSize() + slot);
            int x = layout.left() + (slot % layout.columns()) * HueGradientLayout.CELL;
            int y = layout.previewTop() + (slot / layout.columns()) * HueGradientLayout.CELL;
            graphics.fill(x, y, x + 22, y + 22, 0x88202020);
            graphics.item(HueBlocksRuntime.stack(candidate), x + 3, y + 2);
            graphics.fill(x + 2, y + 20, x + 20, y + 22, 0xFF000000 | candidate.block().rgb());
            if (mouseX >= x && mouseX < x + 22 && mouseY >= y && mouseY < y + 22) {
                graphics.setTooltipForNextFrame(font, text("block_hint", HueBlocksRuntime.stack(candidate).getHoverName(),
                        candidate.itemId(), candidate.block().texture()), mouseX, mouseY);
            }
        }
        if (results.isEmpty()) graphics.text(font, text("preview_empty"), layout.left() + 4, layout.previewTop() + 6, 0xFFBBBBBB);
    }

    private Component dataStatus() {
        var repository = HueBlocksRuntime.repository();
        if (repository.checking()) return text(repository.data() == null ? "loading" : "checking_cached");
        return switch (repository.state()) {
            case READY -> text("ready", repository.data().blocks().size());
            case CACHED -> text("cached");
            case UNSUPPORTED -> text("unsupported");
            case FAILED, EMPTY -> text("download_failed");
        };
    }

    private static int parsePreviewRgb(String hex) {
        try { return Integer.parseInt(hex.replace("#", ""), 16); } catch (NumberFormatException ignored) { return 0; }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        HueGradientLayout layout = layout();
        int x = (int) event.x() - layout.left();
        int y = (int) event.y() - layout.previewTop();
        if (event.button() == 0 && x >= 0 && x < layout.columns() * HueGradientLayout.CELL
                && y >= 0 && y < layout.rows() * HueGradientLayout.CELL) {
            int index = page * layout.pageSize() + (y / HueGradientLayout.CELL) * layout.columns() + x / HueGradientLayout.CELL;
            if (index < previewResults.size()) { setBlock(previewResults.get(index)); rebuildWidgets(); return true; }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double ax, double ay) {
        if (y >= layout().previewTop() && y < layout().footerTop()) {
            changePage((ay != 0 ? ay : ax) < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(x, y, ax, ay);
    }

    private void changePage(int delta) { page = Math.max(0, Math.min(maxPage(), page + delta)); updateAvailability(); }
    private int maxPage() { return Math.max(0, (previewResults.size() - 1) / layout().pageSize()); }
    private HueGradientLayout layout() { return HueGradientLayout.calculate(width, height); }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }

    private enum ApplyTarget {
        TEMPORARY("temporary_wheel"), INVENTORY("inventory"), CREATE("create_group"), REPLACE("replace_group");
        private final String key;
        ApplyTarget(String key) { this.key = key; }
    }

    private static final class Node {
        String hex;
        String steps;
        HueGradient.Candidate pinned;
        Node(String hex, String steps) { this.hex = hex; this.steps = steps; }
    }
}
