package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.ConfigLoadResult;
import com.davidblackcn.lorianarchorbit.config.WheelConfigCodec;
import com.davidblackcn.lorianarchorbit.interaction.HudPoint;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationState;
import com.davidblackcn.lorianarchorbit.interaction.RadialGeometry;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuWindow;
import com.davidblackcn.lorianarchorbit.interaction.PaletteRadialLayout;
import com.davidblackcn.lorianarchorbit.interaction.RadialRotationState;
import com.davidblackcn.lorianarchorbit.interaction.ScrollAccumulator;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.BuiltinPalettePresets;
import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.davidblackcn.lorianarchorbit.palette.PaletteWheelDraft;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteImportConflictPolicy;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteImportResult;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareBundle;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareEntry;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareImporter;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class PaletteEditorScreen extends WorkbenchScreen {
    private static final int GRID_CELL = PaletteEditorLayout.GRID_CELL;
    private static final int GRID_TOP = PaletteEditorLayout.GRID_TOP;
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 0;
    private static final int TAB_ARROW_GAP = 6;
    private static final int MEMBER_SCROLLBAR_WIDTH = 6;
    private static final long MEMBER_AUTO_SCROLL_MILLIS = 90;
    private static final int PREVIEW_ROTATION_MILLIS = 140;
    private static final long CLICK_FEEDBACK_MILLIS = 180;
    private static final long DRAG_TRANSITION_MILLIS = 120;
    private final PaletteWheelDraft primary;
    private final PaletteWheelDraft secondary;
    private final com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot primaryBase;
    private final com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot secondaryBase;
    private final ScrollAccumulator previewScroll = new ScrollAccumulator();
    private final Deque<com.davidblackcn.lorianarchorbit.palette.WheelEditorState.Snapshot> undo;
    private List<CreativeModeTab> creativeTabs = List.of();
    private CreativeModeTab selectedCreativeTab;
    private Layer layer = Layer.PRIMARY;
    private int selectedGroup = -1;
    private int groupScroll;
    private boolean revealOnInit;
    private int tabStart;
    private int itemScrollRow;
    private int memberScrollRow;
    private boolean browserScrollbarDragging;
    private boolean groupScrollbarDragging;
    private boolean memberScrollbarDragging;
    private int draggedMember = -1;
    private int dragTarget = -1;
    private int previousDragTarget = -1;
    private int dragMouseY;
    private double draggedVisualY = Double.NaN;
    private long dragTransitionStartedAt;
    private long memberAutoScrollAt;
    private int pressedTabArrow;
    private long tabArrowPressedAt = -1L;
    private int previewSelection;
    private RadialRotationState previewRotation;
    private EditBox search;
    private EditBox groupName;
    private boolean syncingName;
    private Component status = Component.empty();
    private Pane pane = Pane.GROUPS;
    private int previousGridColumns;
    private int previewVisibleCount;
    private String nameGroupId;
    private Layer nameLayer;

    private enum Pane { GROUPS, ITEMS, MEMBERS, PREVIEW }

    private boolean shows(Pane target) { return !layout().compact() || pane == target; }

    PaletteEditorScreen(EditorSession session) {
        super(session, "wheel", Component.translatable("palette_editor.lorian_arch_orbit.title"));
        this.primaryBase = session.wheelState.primaryBase;
        this.secondaryBase = session.wheelState.secondaryBase;
        this.primary = session.wheelState.primary;
        this.secondary = session.wheelState.secondary;
        this.undo = session.wheelState.undo;
    }

    boolean hasUnsavedChanges() { return session.wheelState.dirty(); }
    com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef selectedRef() {
        return selected() == null ? null : new com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef(
                layer == Layer.PRIMARY, selected().id());
    }
    void selectRef(com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef ref) {
        if (session.wheelState.resolve(ref) == null) return;
        layer = ref.primary() ? Layer.PRIMARY : Layer.SECONDARY;
        for (int i = 0; i < draft().groups().size(); i++) if (draft().groups().get(i).id().equals(ref.id())) selectedGroup = i;
    }

    @Override
    protected void initWorkbench() {
        clearDragFeedback();
        browserScrollbarDragging = groupScrollbarDragging = memberScrollbarDragging = false;
        refreshCreativeTabs();
        PaletteEditorLayout layout = layout();
        if (previousGridColumns > 0) itemScrollRow = itemScrollRow * previousGridColumns / layout.gridColumns();
        previousGridColumns = layout.gridColumns();
        if (layout.compact()) {
            int tabWidth = (width - 24 - 12) / 4;
            String[] labels = {"groups", "items", "members", "preview_tab"};
            for (Pane target : Pane.values()) {
                Button tab = Button.builder(text(labels[target.ordinal()]), button -> {
                    pane = target;
                    rebuildWidgets();
                }).bounds(12 + target.ordinal() * (tabWidth + 4), 28, tabWidth, 20).build();
                tab.active = pane != target;
                addRenderableWidget(tab);
            }
        }
        String previousSearch = search == null ? "" : search.getValue();
        String previousName = groupName != null && nameLayer == layer && selected() != null
                && selected().id().equals(nameGroupId) ? groupName.getValue() : null;
        int searchWidth = layout.compact() ? (layout.browserWidth() - 4) / 2 : layout.browserWidth();
        search = new EditBox(font, layout.browserLeft(), layout.compact() ? 52 : 28, searchWidth, 20,
                Component.translatable("palette_editor.lorian_arch_orbit.search"));
        search.setHint(Component.translatable("palette_editor.lorian_arch_orbit.search"));
        search.setValue(previousSearch);
        search.setResponder(value -> itemScrollRow = 0);
        if (shows(Pane.ITEMS)) addRenderableWidget(search);
        if (layout.compact() && shows(Pane.ITEMS)) {
            Button category = Button.builder(selectedCreativeTab == null ? text("items") : selectedCreativeTab.getDisplayName(), button -> {
                if (!creativeTabs.isEmpty()) {
                    selectedCreativeTab = creativeTabs.get(Math.floorMod(creativeTabs.indexOf(selectedCreativeTab) + 1, creativeTabs.size()));
                    itemScrollRow = 0;
                    rebuildWidgets();
                }
            }).bounds(layout.browserLeft() + searchWidth + 4, 52, layout.browserWidth() - searchWidth - 4, 20).build();
            category.setTooltip(net.minecraft.client.gui.components.Tooltip.create(category.getMessage()));
            addRenderableWidget(category);
        }

        int nameWidth = layout.compact() ? layout.memberWidth() - 112 : layout.memberWidth();
        groupName = new EditBox(font, layout.memberLeft(), layout.compact() ? 52 : 28, nameWidth, 20,
                Component.translatable("palette_editor.lorian_arch_orbit.group_name"));
        groupName.setMaxLength(80);
        groupName.setResponder(this::renameSelectedGroup);
        if (shows(Pane.MEMBERS)) addRenderableWidget(groupName);

        addFooterButtons(layout);
        dropdown(12, 4, 102, text(layer == Layer.PRIMARY ? "layer.primary" : "layer.secondary"), () -> menu(12, 26, List.of(
                new MenuEntry(text("layer.primary"), () -> { if (layer != Layer.PRIMARY) switchLayer(); rebuildWidgets(); }),
                new MenuEntry(text("layer.secondary"), () -> { if (layer != Layer.SECONDARY) switchLayer(); rebuildWidgets(); }))));
        if (shows(Pane.GROUPS)) {
            int top = layout.compact() ? 52 : 28;
            int right = layout.groupLeft() + layout.groupWidth();
            action(right - 88, top, 36, text("new"), this::createGroup);
            dropdown(right - 48, top, 48, EditorSession.text("group_menu"), () -> menu(right - 180, top + 22, List.of(
                    new MenuEntry(text("copy"), this::copyGroup, selected() != null),
                    new MenuEntry(EditorSession.text("delete_named", selected() == null ? "" : selected().displayName()),
                            this::deleteGroup, selected() != null))));
        }
        if (shows(Pane.MEMBERS)) addButton(layout.memberLeft() + (layout.compact() ? nameWidth + 4 : 0), 52,
                layout.compact() ? 108 : layout.memberWidth(), text("held_exact"), button -> addHeldExact());
        syncSelection();
        if (revealOnInit) {
            revealOnInit = false;
            revealSelectedGroup();
        }
        if (previousName != null) {
            syncingName = true;
            try { groupName.setValue(previousName); }
            finally { syncingName = false; }
        }
    }

    private void addButton(int x, int y, int width, Component label, Button.OnPress press) {
        addRenderableWidget(Button.builder(label, press).bounds(x, y, width, 20).build());
    }

    private void addFooterButtons(PaletteEditorLayout layout) {
        int y = layout.footerTop();
        dropdown(12, y, 60, EditorSession.text("file_menu"), () -> menu(12, y - 68, List.of(
                new MenuEntry(text("share"), this::openShareScreen),
                new MenuEntry(text("import"), this::openImportScreen),
                new MenuEntry(EditorSession.text("restore_all"), () -> confirm(EditorSession.text("restore_all_hint"),
                        text("defaults"), this::restoreDefaults)))));
        action(76, y, 52, text("undo"), this::undo);
        action(width - 144, y, 64, EditorSession.text("back"), session::home);
        action(width - 76, y, 64, text("save"), this::save);
    }

    @Override
    protected void renderWorkbench(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (width >= 600) graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        PaletteEditorLayout layout = layout();
        updateScrollbarDragging(layout, mouseY);
        if (shows(Pane.GROUPS)) drawGroups(graphics, layout, mouseX, mouseY);
        if (shows(Pane.ITEMS)) drawCreativeBrowser(graphics, layout, mouseX, mouseY);
        if (shows(Pane.PREVIEW)) drawPreview(graphics, layout, mouseX, mouseY);
        if (shows(Pane.MEMBERS)) {
            updateDragFeedback(layout, mouseX, mouseY);
            drawMembers(graphics, layout, mouseX, mouseY);
        }
        boundedText(graphics, minecraft.level == null ? text("world_required") : hasUnsavedChanges()
                ? EditorSession.text("unsaved_status").copy().append(" · ").append(status) : status,
                layout.groupLeft(), layout.footerTop() - 14, width - 24, 0xFFFFC14D, mouseX, mouseY);
    }

    private void drawGroups(GuiGraphicsExtractor graphics, PaletteEditorLayout layout, int mouseX, int mouseY) {
        graphics.text(font, text("groups"), layout.groupLeft(), layout.compact() ? 58 : 34, 0xFFFFFFFF);
        List<PaletteGroup> groups = draft().groups();
        int rows = groupRows(layout);
        groupScroll = Math.max(0, Math.min(groupScroll, maxGroupScroll(layout)));
        int start = groupScroll;
        for (int row = 0; row < rows && start + row < groups.size(); row++) {
            int index = start + row;
            int y = layout.groupTop() + row * 18;
            int color = index == selectedGroup ? 0xAA3275A8 : 0x88202020;
            graphics.fill(layout.groupLeft(), y, layout.groupLeft() + layout.groupWidth(), y + 16, color);
            boolean builtin = builtinGroup(groups.get(index).id()) != null;
            int nameLeft = builtin ? layout.groupLeft() + 12 : layout.groupLeft() + 4;
            if (builtin) {
                graphics.text(font, "◆", layout.groupLeft() + 3, y + 4, 0xFFFFC14D);
            }
            String name = elideMiddle(groups.get(index).displayName(),
                    layout.groupLeft() + layout.groupWidth() - nameLeft - 4);
            graphics.text(font, name, nameLeft, y + 4, 0xFFFFFFFF);
            if (inside(mouseX, mouseY, layout.groupLeft(), y, layout.groupWidth(), 16)
                    && !name.equals(groups.get(index).displayName())) {
                graphics.setTooltipForNextFrame(font, font.split(Component.literal(groups.get(index).displayName()), width - 24),
                        mouseX, mouseY);
            }
        }
        drawGroupScrollbar(graphics, layout, groups.size(), rows);
    }

    private void drawGroupScrollbar(GuiGraphicsExtractor graphics, PaletteEditorLayout layout, int groupCount, int rows) {
        int trackLeft = layout.groupLeft() + layout.groupWidth() + 2;
        int trackHeight = rows * 18;
        graphics.fill(trackLeft, layout.groupTop(), trackLeft + 6, layout.groupTop() + trackHeight, 0x88303030);
        if (groupCount <= rows) {
            graphics.fill(trackLeft, layout.groupTop(), trackLeft + 6, layout.groupTop() + trackHeight, 0xFF777777);
            return;
        }
        int thumbHeight = Math.max(12, trackHeight * rows / groupCount);
        int thumbY = layout.groupTop() + (trackHeight - thumbHeight) * groupScroll / (groupCount - rows);
        graphics.fill(trackLeft, thumbY, trackLeft + 6, thumbY + thumbHeight,
                groupScrollbarDragging ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void drawCreativeBrowser(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            int mouseX,
            int mouseY
    ) {
        if (!layout.compact()) drawCreativeTabs(graphics, layout, mouseX, mouseY);
        List<ItemStack> items = filteredCreativeItems();
        int columns = layout.gridColumns();
        int rows = layout.gridRows();
        clampItemScroll(items.size(), columns, rows);
        int start = itemScrollRow * columns;
        int gridWidth = columns * GRID_CELL;
        graphics.fill(layout.browserLeft(), GRID_TOP, layout.browserLeft() + gridWidth,
                GRID_TOP + rows * GRID_CELL, 0x55202020);
        for (int index = 0; index < rows * columns && start + index < items.size(); index++) {
            int x = layout.browserLeft() + index % columns * GRID_CELL;
            int y = GRID_TOP + index / columns * GRID_CELL;
            ItemStack stack = items.get(start + index);
            boolean hovered = inside(mouseX, mouseY, x, y, GRID_CELL, GRID_CELL);
            if (hovered) {
                graphics.fill(x, y, x + GRID_CELL, y + GRID_CELL, 0xAA4D6A7D);
            }
            graphics.item(stack, x + 2, y + 2);
            if (hovered) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
        drawBrowserScrollbar(graphics, layout, items.size(), columns, rows);
    }

    private void drawCreativeTabs(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            int mouseX,
            int mouseY
    ) {
        int y = 54;
        int visible = layout.visibleTabs();
        if (creativeTabs.size() > visible) {
            drawTabArrow(graphics, layout.browserLeft(), y, -1, tabStart > 0, mouseX, mouseY);
            drawTabArrow(graphics, layout.browserRight() - 18, y, 1,
                    tabStart + visible < creativeTabs.size(), mouseX, mouseY);
        }
        int firstX = layout.browserLeft() + (creativeTabs.size() > visible ? 18 + TAB_ARROW_GAP : 0);
        int count = Math.min(visible, creativeTabs.size() - Math.min(tabStart, creativeTabs.size()));
        for (int offset = 0; offset < count; offset++) {
            CreativeModeTab tab = creativeTabs.get(tabStart + offset);
            int x = firstX + offset * (TAB_SIZE + TAB_GAP);
            boolean selected = tab == selectedCreativeTab;
            boolean hovered = inside(mouseX, mouseY, x, y, TAB_SIZE, TAB_SIZE);
            graphics.fill(x, y, x + TAB_SIZE, y + TAB_SIZE,
                    selected ? 0xCC527C94 : hovered ? 0xAA4D6A7D : 0x88303030);
            graphics.outline(x, y, TAB_SIZE, TAB_SIZE, selected ? 0xFFFFFFFF : 0xFF777777);
            graphics.item(tab.getIconItem(), x + 2, y + 2);
            if (hovered) {
                graphics.setTooltipForNextFrame(font, tab.getDisplayName(), mouseX, mouseY);
            }
        }
    }

    private void drawTabArrow(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int direction,
            boolean enabled,
            int mouseX,
            int mouseY
    ) {
        long elapsed = System.currentTimeMillis() - tabArrowPressedAt;
        boolean pressed = pressedTabArrow == direction && elapsed >= 0 && elapsed < CLICK_FEEDBACK_MILLIS;
        boolean hovered = enabled && inside(mouseX, mouseY, x, y, 18, TAB_SIZE);
        int offsetY = pressed ? 1 : 0;
        int color = pressed ? 0xEE4A91BD : !enabled ? 0x44202020 : hovered ? 0xBB426D86 : 0x88303030;
        graphics.fill(x, y, x + 18, y + TAB_SIZE, color);
        graphics.outline(x, y, 18, TAB_SIZE, pressed ? 0xFFFFD36A : hovered ? 0xFFB9E6FF : 0xFF777777);
        graphics.centeredText(font, direction > 0 ? ">" : "<", x + 9, y + 6 + offsetY,
                pressed || enabled ? 0xFFFFFFFF : 0xFF777777);
    }

    private void drawBrowserScrollbar(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            int itemCount,
            int columns,
            int rows
    ) {
        int totalRows = Math.max(1, (itemCount + columns - 1) / columns);
        int trackLeft = layout.browserLeft() + columns * GRID_CELL + 2;
        int trackHeight = rows * GRID_CELL;
        graphics.fill(trackLeft, GRID_TOP, trackLeft + 6, GRID_TOP + trackHeight, 0x88303030);
        if (totalRows <= rows) {
            graphics.fill(trackLeft, GRID_TOP, trackLeft + 6, GRID_TOP + trackHeight, 0xFF777777);
            return;
        }
        int thumbHeight = Math.max(12, trackHeight * rows / totalRows);
        int maxScroll = totalRows - rows;
        int thumbY = GRID_TOP + (trackHeight - thumbHeight) * itemScrollRow / maxScroll;
        graphics.fill(trackLeft, thumbY, trackLeft + 6, thumbY + thumbHeight,
                browserScrollbarDragging ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void drawPreview(GuiGraphicsExtractor graphics, PaletteEditorLayout layout, int mouseX, int mouseY) {
        int left = layout.previewLeft();
        int top = layout.previewTop();
        int previewWidth = layout.previewWidth();
        int previewHeight = layout.contentBottom() - top;
        if (previewWidth < 60 || previewHeight < 48) {
            return;
        }
        graphics.fill(left, top, left + previewWidth, top + previewHeight, 0x33202020);
        graphics.outline(left, top, previewWidth, previewHeight, 0x66777777);
        previewText(graphics, text("preview"), left, previewWidth, top + 6, 0xFFBBBBBB, mouseX, mouseY);
        PaletteGroup group = selected();
        if (group == null || group.members().isEmpty()) {
            previewText(graphics, text("preview_empty"), left, previewWidth,
                    top + previewHeight / 2, 0xFF999999, mouseX, mouseY);
            return;
        }
        List<ItemStack> stacks = group.members().stream()
                .map(member -> ClientPaletteItemCodec.resolve(minecraft, member).orElse(ItemStack.EMPTY))
                .toList();
        previewSelection = Math.floorMod(previewSelection, stacks.size());
        boolean singlePreview = previewHeight < 120;
        PaletteRadialLayout ring = singlePreview ? PaletteRadialLayout.calculate(stacks.size(), 0)
                : PaletteRadialLayout.preview(stacks.size(), previewWidth, previewHeight);
        RadialMenuSnapshot<ItemStack> snapshot = RadialMenuWindow.from(stacks, previewSelection, Math.max(1, ring.visibleCount()));
        int centerX = left + previewWidth / 2;
        int centerY = singlePreview ? top + 28 : top + previewHeight / 2;
        int radius = ring.radius();
        long now = System.currentTimeMillis();
        if (previewRotation == null || previewVisibleCount != ring.visibleCount()) {
            previewRotation = RadialRotationState.idle(now, PREVIEW_ROTATION_MILLIS);
        }
        previewVisibleCount = ring.visibleCount();
        var slots = RadialGeometry.slots(snapshot, new HudPoint(centerX, centerY), radius,
                new RadialAnimationState(RadialAnimationMode.OFF, now, 1), now,
                previewRotation.offsetRadians(now)
                        + ClientConfigRuntime.configManager().client().paletteTargetPosition().angleOffset());
        for (var slot : slots) {
            RadialWheelVisuals.renderItem(graphics, slot.value(), slot.x(), slot.y(),
                    PaletteRadialLayout.selectionScale(slot.sourceIndex(), snapshot.selectedIndex(),
                            snapshot.entries().size(), previewRotation.offsetRadians(now)));
        }
        ItemStack selectedStack = stacks.get(previewSelection);
        if (!selectedStack.isEmpty()) {
            RadialWheelVisuals.renderTargetArrow(graphics, centerX, centerY, radius,
                    ClientConfigRuntime.configManager().client().paletteTargetPosition(),
                    ClientConfigRuntime.configManager().client().paletteArrowStyle(), now);
        }
        Component label = selectedStack.isEmpty()
                ? Component.literal(group.members().get(previewSelection).itemId())
                : selectedStack.getHoverName();
        int labelWidth = Math.min(font.width(label), Math.max(0, previewWidth - 12));
        String labelText = elideMiddle(label.getString(), labelWidth);
        int textWidth = font.width(labelText);
        int labelY = centerY + (singlePreview ? 16 : 12);
        graphics.fill(centerX - textWidth / 2 - 3, labelY - 2,
                centerX + (textWidth + 1) / 2 + 3, labelY + font.lineHeight + 2, 0x90000000);
        graphics.centeredText(font, labelText, centerX, labelY, 0xFFFFFFFF);
        if (ring.visibleCount() < stacks.size()) {
            Component overflow = text("wheel_overflow", previewSelection + 1, stacks.size());
            previewText(graphics, overflow, left, previewWidth, top + previewHeight - 10,
                    0xFFBBBBBB, mouseX, mouseY);
        }
    }

    private void previewText(GuiGraphicsExtractor graphics, Component text, int left, int previewWidth,
                             int y, int color, int mouseX, int mouseY) {
        int textWidth = Math.min(font.width(text), previewWidth - 12);
        boundedText(graphics, text, left + (previewWidth - textWidth) / 2, y, textWidth, color, mouseX, mouseY);
    }

    private void drawMembers(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            int mouseX,
            int mouseY
    ) {
        int left = layout.memberLeft();
        PaletteGroup group = selected();
        int count = group == null ? 0 : group.members().size();
        if (!layout.compact()) boundedText(graphics, text("members_count", count), left, 78,
                layout.memberWidth(), 0xFFFFFFFF, mouseX, mouseY);
        if (group == null) {
            return;
        }
        int top = layout.memberTop();
        int rows = memberRows(layout);
        memberScrollRow = Math.max(0, Math.min(memberScrollRow, maxMemberScroll(group.members().size(), rows)));
        int start = memberScrollRow;
        long now = System.currentTimeMillis();
        double transition = Math.min(1.0, Math.max(0.0,
                (double) (now - dragTransitionStartedAt) / DRAG_TRANSITION_MILLIS
        ));
        transition = 1.0 - Math.pow(1.0 - transition, 3.0);
        for (int row = 0; row < rows && start + row < group.members().size(); row++) {
            int index = start + row;
            if (index == draggedMember) {
                continue;
            }
            int previousShift = memberShift(index, previousDragTarget);
            int targetShift = memberShift(index, dragTarget);
            int y = top + row * 18 + (int) Math.round(previousShift + (targetShift - previousShift) * transition);
            drawMemberRow(graphics, layout, group.members().get(index), y, mouseX, mouseY, false);
        }
        if (draggedMember >= start && draggedMember < Math.min(group.members().size(), start + rows)) {
            int desiredY = Math.max(top, Math.min(top + (rows - 1) * 18, dragMouseY - 8));
            if (!Double.isFinite(draggedVisualY)) {
                draggedVisualY = top + (draggedMember - start) * 18;
            }
            draggedVisualY += (desiredY - draggedVisualY) * 0.45;
            int floatingY = (int) Math.round(draggedVisualY);
            graphics.fill(left + 2, floatingY + 3, layout.memberRight() - MEMBER_SCROLLBAR_WIDTH,
                    floatingY + 19, 0x66000000);
            drawMemberRow(
                    graphics, layout, group.members().get(draggedMember), floatingY, mouseX, mouseY, true
            );
        }
        if (transition >= 1.0) {
            previousDragTarget = dragTarget;
        }
        drawMemberScrollbar(graphics, layout, group.members().size(), rows);
    }

    private void drawMemberScrollbar(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            int memberCount,
            int rows
    ) {
        int top = layout.memberTop();
        int trackLeft = layout.memberRight() - MEMBER_SCROLLBAR_WIDTH;
        int trackHeight = rows * 18;
        graphics.fill(trackLeft, top, layout.memberRight(), top + trackHeight, 0x88303030);
        if (memberCount <= rows) {
            graphics.fill(trackLeft, top, layout.memberRight(), top + trackHeight, 0xFF777777);
            return;
        }
        int thumbHeight = Math.max(12, trackHeight * rows / memberCount);
        int maxScroll = maxMemberScroll(memberCount, rows);
        int thumbY = top + (trackHeight - thumbHeight) * memberScrollRow / maxScroll;
        graphics.fill(trackLeft, thumbY, layout.memberRight(), thumbY + thumbHeight,
                memberScrollbarDragging ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void drawMemberRow(
            GuiGraphicsExtractor graphics,
            PaletteEditorLayout layout,
            PaletteMember member,
            int y,
            int mouseX,
            int mouseY,
            boolean floating
    ) {
        int left = layout.memberLeft();
        int contentRight = layout.memberRight() - MEMBER_SCROLLBAR_WIDTH - 2;
        graphics.fill(left, y, contentRight, y + 16, floating ? 0xDD5A4628 : 0x88202020);
        if (floating) {
            graphics.outline(left, y, contentRight - left, 16, 0xFFFFC14D);
        }
        ItemStack stack = ClientPaletteItemCodec.resolve(minecraft, member).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            graphics.item(stack, left, y);
        }
        String suffix = member.matchMode() == PaletteMatchMode.EXACT_COMPONENTS ? " *" : "";
        String fullId = member.itemId() + suffix;
        int textLeft = left + 20;
        int textWidth = Math.max(0, contentRight - textLeft - 4);
        String shownId = elideMiddle(fullId, textWidth);
        graphics.enableScissor(textLeft, y, contentRight - 3, y + 16);
        graphics.text(font, shownId, textLeft, y + 4, 0xFFFFFFFF);
        graphics.disableScissor();
        if (!floating && inside(mouseX, mouseY, left, y, contentRight - left, 16)) {
            List<Component> tooltip = new ArrayList<>();
            if (!stack.isEmpty()) {
                tooltip.add(stack.getHoverName());
            }
            tooltip.add(Component.literal(member.itemId()).withStyle(ChatFormatting.GRAY));
            if (member.matchMode() == PaletteMatchMode.EXACT_COMPONENTS) {
                tooltip.add(text("exact_components").copy().withStyle(ChatFormatting.GOLD));
            }
            graphics.setTooltipForNextFrame(font, tooltip.stream()
                    .flatMap(line -> font.split(line, width - 24).stream()).toList(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean clickWorkbench(MouseButtonEvent event, boolean doubleClick) {
        if (super.clickWorkbench(event, doubleClick)) {
            return true;
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        PaletteEditorLayout layout = layout();
        if (shows(Pane.GROUPS) && event.button() == 0 && startGroupScrollbarDrag(layout, mouseX, mouseY)) {
            return true;
        }
        if (shows(Pane.ITEMS) && event.button() == 0 && startBrowserScrollbarDrag(layout, mouseX, mouseY)) {
            return true;
        }
        if (shows(Pane.MEMBERS) && event.button() == 0 && startMemberScrollbarDrag(layout, mouseX, mouseY)) {
            return true;
        }
        if (shows(Pane.GROUPS) && event.button() == 0 && inside(mouseX, mouseY, layout.groupLeft(), layout.groupTop(),
                layout.groupWidth(), groupRows(layout) * 18)) {
            int index = groupScroll + (mouseY - layout.groupTop()) / 18;
            if (index < draft().groups().size()) {
                selectedGroup = index;
                memberScrollRow = 0;
                previewSelection = 0;
                resetPreviewAnimation();
                syncSelection();
                return true;
            }
        }
        if (shows(Pane.ITEMS) && !layout.compact() && event.button() == 0 && clickCreativeTab(layout, mouseX, mouseY)) {
            return true;
        }
        int gridIndex = shows(Pane.ITEMS) ? gridIndex(layout, mouseX, mouseY) : -1;
        if (event.button() == 0 && gridIndex >= 0) {
            List<ItemStack> items = filteredCreativeItems();
            int itemIndex = itemScrollRow * layout.gridColumns() + gridIndex;
            if (itemIndex < items.size()) {
                addItem(items.get(itemIndex), event.hasShiftDown());
                return true;
            }
        }
        int member = shows(Pane.MEMBERS) ? memberIndex(layout, mouseX, mouseY) : -1;
        if (member >= 0) {
            if (event.button() == 1) {
                removeMember(member);
            } else if (event.button() == 0) {
                draggedMember = member;
                dragTarget = member;
                previousDragTarget = member;
                dragMouseY = mouseY;
                draggedVisualY = layout.memberTop() + (member - memberScrollRow) * 18;
                dragTransitionStartedAt = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseReleasedContent(MouseButtonEvent event) {
        if (browserScrollbarDragging || groupScrollbarDragging || memberScrollbarDragging) {
            browserScrollbarDragging = false;
            groupScrollbarDragging = false;
            memberScrollbarDragging = false;
            return true;
        }
        if (draggedMember >= 0) {
            int target = dragTarget;
            if (target >= 0 && target != draggedMember) {
                moveMember(draggedMember, target);
            }
            clearDragFeedback();
            return true;
        }
        return super.mouseReleasedContent(event);
    }

    @Override
    protected boolean scrollWorkbench(double mouseX, double mouseY, double amountX, double amountY) {
        PaletteEditorLayout layout = layout();
        double amount = amountY != 0.0 ? amountY : amountX;
        if (shows(Pane.GROUPS) && inside((int) mouseX, (int) mouseY, layout.groupLeft(), layout.groupTop(),
                layout.groupWidth() + 8, groupRows(layout) * 18)) {
            groupScroll = Math.max(0, Math.min(maxGroupScroll(layout), groupScroll + (amount < 0 ? 1 : -1)));
            return true;
        }
        if (shows(Pane.ITEMS) && inside((int) mouseX, (int) mouseY, layout.browserLeft(), 52,
                layout.browserWidth(), layout.contentBottom() - 52)) {
            List<ItemStack> items = filteredCreativeItems();
            int max = maxItemScroll(items.size(), layout.gridColumns(), layout.gridRows());
            itemScrollRow = Math.max(0, Math.min(max, itemScrollRow + (amount < 0 ? 1 : -1)));
            return true;
        }
        if (shows(Pane.PREVIEW) && inside((int) mouseX, (int) mouseY, layout.previewLeft(), layout.previewTop(),
                layout.previewWidth(), layout.contentBottom() - layout.previewTop())) {
            PaletteGroup group = selected();
            if (group != null && !group.members().isEmpty()) {
                int steps = previewScroll.add(amount);
                if (steps != 0) {
                    int selectionSteps = ClientConfigRuntime.configManager().client().paletteRotationDirection().selectionSteps(steps);
                    long now = System.currentTimeMillis();
                    previewSelection = Math.floorMod(previewSelection + selectionSteps, group.members().size());
                    if (previewRotation == null) {
                        previewRotation = RadialRotationState.idle(now, PREVIEW_ROTATION_MILLIS);
                    }
                    previewRotation = previewRotation.retarget(
                            selectionSteps, Math.max(1, previewVisibleCount), now, PREVIEW_ROTATION_MILLIS
                    );
                }
            }
            return true;
        }
        if (shows(Pane.MEMBERS) && inside((int) mouseX, (int) mouseY, layout.memberLeft(), layout.memberTop(),
                layout.memberWidth(), memberRows(layout) * 18)) {
            PaletteGroup selected = selected();
            if (selected != null) {
                int max = maxMemberScroll(selected.members().size(), memberRows(layout));
                memberScrollRow = Math.max(0, Math.min(max, memberScrollRow + (amount < 0 ? 1 : -1)));
                return true;
            }
        }
        return super.scrollWorkbench(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public void onClose() {
        super.onClose();
    }

    private void refreshCreativeTabs() {
        if (minecraft.level != null && minecraft.player != null) {
            boolean showOperatorItems = minecraft.options.operatorItemsTab().get()
                    && minecraft.player.canUseGameMasterBlocks();
            CreativeModeTabs.tryRebuildTabContents(
                    minecraft.level.enabledFeatures(), showOperatorItems, minecraft.level.registryAccess()
            );
        }
        creativeTabs = CreativeModeTabs.allTabs().stream()
                .filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY)
                .filter(CreativeModeTab::shouldDisplay)
                .toList();
        if (selectedCreativeTab == null || !creativeTabs.contains(selectedCreativeTab)) {
            CreativeModeTab defaultTab = CreativeModeTabs.getDefaultTab();
            selectedCreativeTab = creativeTabs.contains(defaultTab)
                    ? defaultTab
                    : creativeTabs.stream().findFirst().orElse(null);
        }
    }

    private boolean clickCreativeTab(PaletteEditorLayout layout, int mouseX, int mouseY) {
        if (!inside(mouseX, mouseY, layout.browserLeft(), 54, layout.browserWidth(), TAB_SIZE)) {
            return false;
        }
        int visible = layout.visibleTabs();
        if (creativeTabs.size() > visible) {
            if (inside(mouseX, mouseY, layout.browserLeft(), 54, 18, TAB_SIZE)) {
                if (tabStart > 0) {
                    pressedTabArrow = -1;
                    tabArrowPressedAt = System.currentTimeMillis();
                    playArrowClick();
                    tabStart = Math.max(0, tabStart - visible);
                }
                return true;
            }
            if (inside(mouseX, mouseY, layout.browserRight() - 18, 54, 18, TAB_SIZE)) {
                if (tabStart + visible < creativeTabs.size()) {
                    pressedTabArrow = 1;
                    tabArrowPressedAt = System.currentTimeMillis();
                    playArrowClick();
                    tabStart = Math.min(Math.max(0, creativeTabs.size() - visible), tabStart + visible);
                }
                return true;
            }
        }
        int firstX = layout.browserLeft() + (creativeTabs.size() > visible ? 18 + TAB_ARROW_GAP : 0);
        int offset = (mouseX - firstX) / (TAB_SIZE + TAB_GAP);
        int index = tabStart + offset;
        if (offset >= 0 && offset < visible && index < creativeTabs.size()) {
            selectedCreativeTab = creativeTabs.get(index);
            itemScrollRow = 0;
            return true;
        }
        return false;
    }

    private void playArrowClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private List<ItemStack> filteredCreativeItems() {
        // Since 26.2, item components are bound by the world's registry lifecycle.
        if (minecraft.level == null) return List.of();
        String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
        List<ItemStack> source;
        if (!query.isBlank()) {
            source = CreativeModeTabs.searchTab().getDisplayItems().stream().toList();
            if (source.isEmpty()) {
                source = creativeTabs.stream().flatMap(tab -> tab.getSearchTabDisplayItems().stream()).toList();
            }
        } else if (selectedCreativeTab != null) {
            source = selectedCreativeTab.getDisplayItems().stream().toList();
        } else {
            source = BuiltInRegistries.ITEM.stream()
                    .map(item -> item.getDefaultInstance())
                    .filter(stack -> !stack.isEmpty())
                    .toList();
        }
        if (query.isBlank()) {
            return source;
        }
        return source.stream().filter(stack -> {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
            return id.contains(query) || name.contains(query);
        }).toList();
    }

    private void switchLayer() {
        layer = layer == Layer.PRIMARY ? Layer.SECONDARY : Layer.PRIMARY;
        selectedGroup = draft().groups().isEmpty() ? -1 : 0;
        groupScroll = 0;
        memberScrollRow = 0;
        previewSelection = 0;
        resetPreviewAnimation();
        syncSelection();
    }

    private void createGroup() {
        String idBase = "group";
        int suffix = 1;
        Set<String> ids = new TreeSet<>();
        draft().groups().forEach(group -> ids.add(group.id()));
        while (ids.contains(idBase + suffix)) suffix++;
        rememberEditor();
        draft().addGroup(new PaletteGroup(idBase + suffix, "Group " + suffix, "minecraft:stone", List.of()));
        selectedGroup = draft().groups().size() - 1;
        revealSelectedGroup();
        memberScrollRow = 0;
        previewSelection = 0;
        syncSelection();
    }

    private void copyGroup() {
        PaletteGroup selected = selected();
        if (selected == null) return;
        String base = selected.id() + "_copy";
        String id = base;
        int suffix = 2;
        Set<String> ids = new TreeSet<>();
        draft().groups().forEach(group -> ids.add(group.id()));
        while (ids.contains(id)) id = base + suffix++;
        rememberEditor();
        draft().addGroup(new PaletteGroup(id, selected.displayName() + " Copy", selected.iconItemId(), selected.members()));
        selectedGroup = draft().groups().size() - 1;
        revealSelectedGroup();
        memberScrollRow = 0;
        previewSelection = 0;
        syncSelection();
    }

    private void deleteGroup() {
        if (selectedGroup < 0) return;
        rememberEditor();
        PaletteGroup selected = selected();
        PaletteGroup builtin = selected == null ? null : builtinGroup(selected.id());
        if (builtin != null) {
            draft().replaceGroup(selectedGroup, builtin);
            status = text("builtin_restored");
        } else {
            draft().removeGroup(selectedGroup);
        }
        selectedGroup = Math.min(selectedGroup, draft().groups().size() - 1);
        revealSelectedGroup();
        memberScrollRow = 0;
        previewSelection = 0;
        syncSelection();
    }

    private void restoreDefaults() {
        rememberEditor();
        var clientConfig = ClientConfigRuntime.configManager().client();
        primary.replace(BuiltinPalettePresets.groups(clientConfig.primaryPalettePreset()));
        secondary.replace(BuiltinPalettePresets.groups(clientConfig.secondaryPalettePreset()));
        selectedGroup = draft().groups().isEmpty() ? -1 : 0;
        groupScroll = 0;
        memberScrollRow = 0;
        previewSelection = 0;
        clearDragFeedback();
        resetPreviewAnimation();
        syncSelection();
        status = text("defaults_pending");
    }

    private void undo() {
        if (!undo.isEmpty()) {
            var snapshot = undo.pop();
            primary.restoreWithoutUndo(snapshot.primary());
            secondary.restoreWithoutUndo(snapshot.secondary());
            selectedGroup = Math.min(selectedGroup, draft().groups().size() - 1);
            revealSelectedGroup();
            previewSelection = 0;
            syncSelection();
        }
    }

    private void save() {
        var manager = ClientConfigRuntime.configManager();
        var clientConfig = manager.client();
        WheelConfigCodec primaryCodec = new WheelConfigCodec(
                () -> BuiltinPalettePresets.groups(clientConfig.primaryPalettePreset())
        );
        WheelConfigCodec secondaryCodec = new WheelConfigCodec(
                () -> BuiltinPalettePresets.groups(clientConfig.secondaryPalettePreset())
        );
        ConfigLoadResult primaryResult = ClientConfigRuntime.configManager()
                .savePrimaryWheel(primaryCodec.fromGroups(primaryBase, primary.groups()));
        ConfigLoadResult secondaryResult = ClientConfigRuntime.configManager()
                .saveSecondaryWheel(secondaryCodec.fromGroups(secondaryBase, secondary.groups()));
        session.wheelState.saved(primaryResult.successful(), secondaryResult.successful());
        status = primaryResult.successful() && secondaryResult.successful() ? text("saved") : text("save_failed");
    }

    private void addItem(ItemStack stack, boolean setIcon) {
        PaletteGroup group = selected();
        if (group == null || stack.isEmpty()) return;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (setIcon) {
            replaceSelected(new PaletteGroup(group.id(), group.displayName(), id, group.members()));
            status = text("icon_set");
            return;
        }
        PaletteMember member = new PaletteMember(id);
        if (group.members().contains(member)) {
            status = text("duplicate");
            return;
        }
        List<PaletteMember> members = new ArrayList<>(group.members());
        members.add(member);
        replaceSelected(new PaletteGroup(group.id(), group.displayName(), group.iconItemId(), members));
        previewSelection = members.size() - 1;
    }

    private void addHeldExact() {
        PaletteGroup group = selected();
        if (group == null || minecraft.player == null) return;
        ItemStack held = minecraft.player.getInventory().getSelectedItem();
        if (held.isEmpty()) return;
        ClientPaletteItemCodec.encodePatch(minecraft, held).ifPresentOrElse(components -> {
            PaletteMember member = new PaletteMember(
                    ClientPaletteItemCodec.itemId(held), PaletteMatchMode.EXACT_COMPONENTS, components
            );
            if (group.members().contains(member)) {
                status = text("duplicate");
                return;
            }
            List<PaletteMember> members = new ArrayList<>(group.members());
            members.add(member);
            replaceSelected(new PaletteGroup(group.id(), group.displayName(), group.iconItemId(), members));
            previewSelection = members.size() - 1;
        }, () -> status = text("component_unavailable"));
    }

    private void removeMember(int memberIndex) {
        PaletteGroup group = selected();
        if (group == null || memberIndex >= group.members().size()) return;
        List<PaletteMember> members = new ArrayList<>(group.members());
        members.remove(memberIndex);
        replaceSelected(new PaletteGroup(group.id(), group.displayName(), group.iconItemId(), members));
        previewSelection = members.isEmpty() ? 0 : Math.min(previewSelection, members.size() - 1);
        int maxScroll = maxMemberScroll(members.size(), memberRows(layout()));
        memberScrollRow = Math.min(memberScrollRow, maxScroll);
    }

    private void moveMember(int from, int to) {
        PaletteGroup group = selected();
        if (group == null || from >= group.members().size() || to >= group.members().size()) return;
        List<PaletteMember> members = new ArrayList<>(group.members());
        PaletteMember moved = members.remove(from);
        members.add(to, moved);
        replaceSelected(new PaletteGroup(group.id(), group.displayName(), group.iconItemId(), members));
        previewSelection = to;
    }

    private void renameSelectedGroup(String name) {
        if (syncingName || name.isBlank()) return;
        PaletteGroup group = selected();
        if (group != null && !group.displayName().equals(name)) {
            replaceSelected(new PaletteGroup(group.id(), name, group.iconItemId(), group.members()));
        }
    }

    private void replaceSelected(PaletteGroup replacement) {
        if (selectedGroup >= 0) {
            rememberEditor();
            draft().replaceGroup(selectedGroup, replacement);
        }
    }

    private void rememberEditor() {
        undo.push(new com.davidblackcn.lorianarchorbit.palette.WheelEditorState.Snapshot(primary.groups(), secondary.groups()));
    }

    void applyGradient(List<String> items, boolean create, int duplicatesRemoved) {
        if (items.size() < 2) return;
        if (create || selected() == null) {
            createGroup();
            PaletteGroup target = selected();
            // createGroup already captured the pre-apply snapshot; the whole application is one undo.
            draft().replaceGroup(selectedGroup, new PaletteGroup(target.id(),
                    HueGradientScreen.text("group_name").getString(), items.getFirst(),
                    items.stream().map(PaletteMember::new).toList()));
        } else {
            PaletteGroup target = selected();
            replaceSelected(new PaletteGroup(target.id(), target.displayName(), target.iconItemId(),
                    items.stream().map(PaletteMember::new).toList()));
        }
        memberScrollRow = 0;
        previewSelection = 0;
        resetPreviewAnimation();
        syncSelection();
        status = HueGradientScreen.text("applied", items.size(), duplicatesRemoved);
    }

    private void openShareScreen() {
        minecraft.setScreenAndShow(new PaletteShareScreen(this, shareableEntries(), selectedShareEntry()));
    }

    private void openImportScreen() {
        minecraft.setScreenAndShow(new PaletteImportScreen(this, ClientConfigRuntime.configManager().directory()));
    }

    List<PaletteShareEntry> shareableEntries() {
        List<PaletteShareEntry> entries = new ArrayList<>();
        addShareable(entries, PaletteShareLayer.PRIMARY, primary.groups());
        addShareable(entries, PaletteShareLayer.SECONDARY, secondary.groups());
        return List.copyOf(entries);
    }

    private void addShareable(
            List<PaletteShareEntry> entries,
            PaletteShareLayer shareLayer,
            List<PaletteGroup> groups
    ) {
        var config = ClientConfigRuntime.configManager().client();
        var preset = shareLayer == PaletteShareLayer.PRIMARY
                ? config.primaryPalettePreset()
                : config.secondaryPalettePreset();
        java.util.Map<String, PaletteGroup> builtins = new java.util.HashMap<>();
        BuiltinPalettePresets.groups(preset).forEach(group -> builtins.put(group.id(), group));
        groups.stream().filter(group -> !group.equals(builtins.get(group.id())))
                .map(group -> new PaletteShareEntry(shareLayer, group)).forEach(entries::add);
    }

    private PaletteShareEntry selectedShareEntry() {
        PaletteGroup selected = selected();
        if (selected == null) {
            return null;
        }
        PaletteShareLayer shareLayer = layer == Layer.PRIMARY
                ? PaletteShareLayer.PRIMARY
                : PaletteShareLayer.SECONDARY;
        return new PaletteShareEntry(shareLayer, selected);
    }

    void applyImport(PaletteShareBundle bundle, PaletteImportConflictPolicy policy) {
        PaletteImportResult result = PaletteShareImporter.merge(primary.groups(), secondary.groups(), bundle, policy);
        if (result.imported() > 0) {
            rememberEditor();
            primary.restoreWithoutUndo(result.primary());
            secondary.restoreWithoutUndo(result.secondary());
            selectedGroup = Math.min(selectedGroup, draft().groups().size() - 1);
            revealSelectedGroup();
            memberScrollRow = 0;
            previewSelection = 0;
            resetPreviewAnimation();
            syncSelection();
        }
        status = Component.translatable(
                "palette_editor.lorian_arch_orbit.import_result",
                result.imported(), result.renamed(), result.replaced(), result.skipped()
        );
    }

    private void syncSelection() {
        syncingName = true;
        if (groupName != null) {
            PaletteGroup selected = selected();
            nameGroupId = selected == null ? null : selected.id();
            nameLayer = layer;
            groupName.setValue(selected == null ? "" : selected.displayName());
            groupName.setEditable(selected != null);
        }
        syncingName = false;
    }

    private void resetPreviewAnimation() {
        previewScroll.reset();
        previewRotation = RadialRotationState.idle(System.currentTimeMillis(), PREVIEW_ROTATION_MILLIS);
    }

    private PaletteWheelDraft draft() {
        return layer == Layer.PRIMARY ? primary : secondary;
    }

    private PaletteGroup selected() {
        List<PaletteGroup> groups = draft().groups();
        return selectedGroup >= 0 && selectedGroup < groups.size() ? groups.get(selectedGroup) : null;
    }

    private PaletteGroup builtinGroup(String id) {
        var config = ClientConfigRuntime.configManager().client();
        var preset = layer == Layer.PRIMARY
                ? config.primaryPalettePreset()
                : config.secondaryPalettePreset();
        return BuiltinPalettePresets.groups(preset).stream()
                .filter(group -> group.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    private int gridIndex(PaletteEditorLayout layout, int mouseX, int mouseY) {
        int rows = layout.gridRows();
        int width = layout.gridColumns() * GRID_CELL;
        if (!inside(mouseX, mouseY, layout.browserLeft(), GRID_TOP, width, rows * GRID_CELL)) return -1;
        return (mouseY - GRID_TOP) / GRID_CELL * layout.gridColumns()
                + (mouseX - layout.browserLeft()) / GRID_CELL;
    }

    private int memberIndex(PaletteEditorLayout layout, int mouseX, int mouseY) {
        int top = layout.memberTop();
        if (!inside(mouseX, mouseY, layout.memberLeft(), top, layout.memberWidth(), memberRows(layout) * 18)) return -1;
        int index = memberScrollRow + (mouseY - top) / 18;
        PaletteGroup group = selected();
        return group != null && index < group.members().size() ? index : -1;
    }

    private void updateDragFeedback(PaletteEditorLayout layout, int mouseX, int mouseY) {
        if (draggedMember < 0) {
            return;
        }
        dragMouseY = mouseY;
        PaletteGroup group = selected();
        if (group == null || mouseX < layout.memberLeft() || mouseX >= layout.memberRight()) {
            return;
        }
        int rows = memberRows(layout);
        int top = layout.memberTop();
        int bottom = top + rows * 18;
        int scrollDirection = mouseY < top + 9 ? -1 : mouseY >= bottom - 9 ? 1 : 0;
        long now = System.currentTimeMillis();
        if (scrollDirection != 0 && now - memberAutoScrollAt >= MEMBER_AUTO_SCROLL_MILLIS) {
            int maxScroll = maxMemberScroll(group.members().size(), rows);
            int next = Math.max(0, Math.min(maxScroll, memberScrollRow + scrollDirection));
            if (next != memberScrollRow) {
                memberScrollRow = next;
            }
            memberAutoScrollAt = now;
        }
        int start = memberScrollRow;
        int visibleCount = Math.min(rows, group.members().size() - start);
        if (visibleCount <= 0) {
            return;
        }
        int row = Math.max(0, Math.min(visibleCount - 1, (mouseY - top) / 18));
        int candidate = start + row;
        if (candidate != dragTarget) {
            previousDragTarget = dragTarget;
            dragTarget = candidate;
            dragTransitionStartedAt = System.currentTimeMillis();
        }
    }

    private int memberShift(int member, int target) {
        if (draggedMember < 0 || target < 0) {
            return 0;
        }
        if (draggedMember < target && member > draggedMember && member <= target) {
            return -18;
        }
        if (draggedMember > target && member >= target && member < draggedMember) {
            return 18;
        }
        return 0;
    }

    private void clearDragFeedback() {
        draggedMember = -1;
        dragTarget = -1;
        previousDragTarget = -1;
        draggedVisualY = Double.NaN;
        memberAutoScrollAt = 0L;
    }

    private boolean startBrowserScrollbarDrag(PaletteEditorLayout layout, int mouseX, int mouseY) {
        int rows = layout.gridRows();
        int trackLeft = layout.browserLeft() + layout.gridColumns() * GRID_CELL + 2;
        if (!inside(mouseX, mouseY, trackLeft, GRID_TOP, 6, rows * GRID_CELL)) {
            return false;
        }
        int maxScroll = maxItemScroll(filteredCreativeItems().size(), layout.gridColumns(), rows);
        if (maxScroll <= 0) {
            return true;
        }
        browserScrollbarDragging = true;
        groupScrollbarDragging = false;
        updateBrowserScrollbar(layout, mouseY);
        return true;
    }

    private boolean startGroupScrollbarDrag(PaletteEditorLayout layout, int mouseX, int mouseY) {
        int rows = groupRows(layout);
        if (!inside(mouseX, mouseY, layout.groupLeft() + layout.groupWidth() + 2, layout.groupTop(), 6, rows * 18)) {
            return false;
        }
        if (maxGroupScroll(layout) <= 0) {
            return true;
        }
        groupScrollbarDragging = true;
        browserScrollbarDragging = false;
        updateGroupScrollbar(mouseY);
        return true;
    }

    private boolean startMemberScrollbarDrag(PaletteEditorLayout layout, int mouseX, int mouseY) {
        int rows = memberRows(layout);
        int trackLeft = layout.memberRight() - MEMBER_SCROLLBAR_WIDTH;
        if (!inside(mouseX, mouseY, trackLeft, layout.memberTop(), MEMBER_SCROLLBAR_WIDTH, rows * 18)) {
            return false;
        }
        PaletteGroup group = selected();
        if (group == null || maxMemberScroll(group.members().size(), rows) <= 0) {
            return true;
        }
        memberScrollbarDragging = true;
        browserScrollbarDragging = false;
        groupScrollbarDragging = false;
        updateMemberScrollbar(layout, mouseY);
        return true;
    }

    private void updateScrollbarDragging(PaletteEditorLayout layout, int mouseY) {
        if (browserScrollbarDragging) {
            updateBrowserScrollbar(layout, mouseY);
        } else if (groupScrollbarDragging) {
            updateGroupScrollbar(mouseY);
        } else if (memberScrollbarDragging) {
            updateMemberScrollbar(layout, mouseY);
        }
    }

    private void updateBrowserScrollbar(PaletteEditorLayout layout, int mouseY) {
        int rows = layout.gridRows();
        int itemCount = filteredCreativeItems().size();
        int totalRows = Math.max(1, (itemCount + layout.gridColumns() - 1) / layout.gridColumns());
        int maxScroll = Math.max(0, totalRows - rows);
        int trackHeight = rows * GRID_CELL;
        int thumbHeight = Math.max(12, trackHeight * rows / totalRows);
        itemScrollRow = scrollbarValue(mouseY, GRID_TOP, trackHeight, thumbHeight, maxScroll);
    }

    private void updateGroupScrollbar(int mouseY) {
        PaletteEditorLayout layout = layout();
        int rows = groupRows(layout);
        int count = Math.max(1, draft().groups().size());
        int trackHeight = rows * 18;
        int thumbHeight = Math.max(12, trackHeight * rows / count);
        groupScroll = scrollbarValue(mouseY, layout.groupTop(), trackHeight, thumbHeight, maxGroupScroll(layout));
    }

    private void updateMemberScrollbar(PaletteEditorLayout layout, int mouseY) {
        PaletteGroup group = selected();
        if (group == null) {
            memberScrollRow = 0;
            return;
        }
        int rows = memberRows(layout);
        int count = Math.max(1, group.members().size());
        int trackHeight = rows * 18;
        int thumbHeight = Math.max(12, trackHeight * rows / count);
        memberScrollRow = scrollbarValue(mouseY, layout.memberTop(), trackHeight, thumbHeight, maxMemberScroll(count, rows));
    }

    private static int scrollbarValue(int mouseY, int top, int trackHeight, int thumbHeight, int maximum) {
        if (maximum <= 0 || trackHeight <= thumbHeight) {
            return 0;
        }
        double position = (double) (mouseY - top - thumbHeight / 2) / (trackHeight - thumbHeight);
        return Math.max(0, Math.min(maximum, (int) Math.round(position * maximum)));
    }

    private void revealSelectedGroup() {
        // A gradient can create a group before this cached page has ever been opened.
        if (width < 320 || height < 180) {
            revealOnInit = true;
            return;
        }
        if (selectedGroup < 0) {
            groupScroll = 0;
            return;
        }
        int rows = groupRows(layout());
        if (selectedGroup < groupScroll) {
            groupScroll = selectedGroup;
        } else if (selectedGroup >= groupScroll + rows) {
            groupScroll = selectedGroup - rows + 1;
        }
        groupScroll = Math.max(0, Math.min(groupScroll, maxGroupScroll(layout())));
    }

    private int maxGroupScroll(PaletteEditorLayout layout) {
        return Math.max(0, draft().groups().size() - groupRows(layout));
    }

    private int groupRows(PaletteEditorLayout layout) {
        return layout.groupRows();
    }

    private void clampItemScroll(int itemCount, int columns, int rows) {
        itemScrollRow = Math.max(0, Math.min(itemScrollRow, maxItemScroll(itemCount, columns, rows)));
    }

    private static int maxItemScroll(int itemCount, int columns, int rows) {
        int totalRows = (itemCount + columns - 1) / columns;
        return Math.max(0, totalRows - rows);
    }

    private static int maxMemberScroll(int memberCount, int rows) {
        return Math.max(0, memberCount - rows);
    }

    private int memberRows(PaletteEditorLayout layout) {
        return layout.memberRows();
    }

    private PaletteEditorLayout layout() {
        return PaletteEditorLayout.calculate(width, height, pane == Pane.PREVIEW);
    }

    private String elideMiddle(String value, int maximumWidth) {
        if (maximumWidth <= 0) return "";
        if (font.width(value) <= maximumWidth) return value;
        String ellipsis = "…";
        int remaining = maximumWidth - font.width(ellipsis);
        if (remaining <= 0) return font.plainSubstrByWidth(ellipsis, maximumWidth);
        String left = font.plainSubstrByWidth(value, remaining / 2);
        String right = font.plainSubstrByWidth(value, remaining - font.width(left), true);
        return left + ellipsis + right;
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return width > 0 && height > 0 && x >= left && x < left + width && y >= top && y < top + height;
    }

    private static Component text(String suffix, Object... args) {
        return Component.translatable("palette_editor.lorian_arch_orbit." + suffix, args);
    }

    private enum Layer { PRIMARY, SECONDARY }

}
