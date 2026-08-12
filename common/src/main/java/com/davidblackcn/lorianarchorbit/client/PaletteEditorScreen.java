package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.config.ConfigLoadResult;
import com.davidblackcn.lorianarchorbit.config.WheelConfigCodec;
import com.davidblackcn.lorianarchorbit.interaction.HudPoint;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationMode;
import com.davidblackcn.lorianarchorbit.interaction.RadialAnimationState;
import com.davidblackcn.lorianarchorbit.interaction.RadialGeometry;
import com.davidblackcn.lorianarchorbit.interaction.RadialMenuSnapshot;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class PaletteEditorScreen extends Screen {
    private static final int GROUP_LEFT = 20;
    private static final int GROUP_WIDTH = 125;
    private static final int COLUMN_GAP = 14;
    private static final int GRID_CELL = 20;
    private static final int GRID_TOP = 78;
    private static final int TAB_SIZE = 20;
    private static final int PREVIEW_ROTATION_MILLIS = 140;
    private static final long CLICK_FEEDBACK_MILLIS = 180;
    private static final long DRAG_TRANSITION_MILLIS = 120;
    private final Screen parent;
    private final PaletteWheelDraft primary;
    private final PaletteWheelDraft secondary;
    private final com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot primaryBase;
    private final com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot secondaryBase;
    private final ScrollAccumulator previewScroll = new ScrollAccumulator();
    private final Deque<EditorSnapshot> undo = new ArrayDeque<>();
    private List<CreativeModeTab> creativeTabs = List.of();
    private CreativeModeTab selectedCreativeTab;
    private Layer layer = Layer.PRIMARY;
    private int selectedGroup = -1;
    private int groupScroll;
    private int tabStart;
    private int itemScrollRow;
    private int memberPage;
    private boolean browserScrollbarDragging;
    private boolean groupScrollbarDragging;
    private int draggedMember = -1;
    private int dragTarget = -1;
    private int previousDragTarget = -1;
    private int dragMouseY;
    private double draggedVisualY = Double.NaN;
    private long dragTransitionStartedAt;
    private int pressedTabArrow;
    private long tabArrowPressedAt = -1L;
    private int previewSelection;
    private RadialRotationState previewRotation;
    private EditBox search;
    private EditBox groupName;
    private boolean syncingName;
    private Component status = Component.empty();

    public PaletteEditorScreen(Screen parent) {
        super(Component.translatable("palette_editor.lorian_arch_orbit.title"));
        this.parent = parent;
        this.primaryBase = ClientConfigRuntime.configManager().primaryWheel();
        this.secondaryBase = ClientConfigRuntime.configManager().secondaryWheel();
        this.primary = new PaletteWheelDraft(primaryBase);
        this.secondary = new PaletteWheelDraft(secondaryBase);
    }

    @Override
    protected void init() {
        refreshCreativeTabs();
        EditorLayout layout = layout();
        int bottom = height - 26;
        search = new EditBox(font, layout.browserLeft(), 28, layout.browserWidth(), 20,
                Component.translatable("palette_editor.lorian_arch_orbit.search"));
        search.setHint(Component.translatable("palette_editor.lorian_arch_orbit.search"));
        search.setResponder(value -> itemScrollRow = 0);
        addRenderableWidget(search);

        groupName = new EditBox(font, layout.memberLeft(), 28, layout.memberWidth(), 20,
                Component.translatable("palette_editor.lorian_arch_orbit.group_name"));
        groupName.setMaxLength(80);
        groupName.setResponder(this::renameSelectedGroup);
        addRenderableWidget(groupName);

        addButton(20, bottom, 74, text("layer"), button -> switchLayer());
        addButton(98, bottom, 54, text("new"), button -> createGroup());
        addButton(156, bottom, 54, text("copy"), button -> copyGroup());
        addButton(214, bottom, 54, text("delete"), button -> deleteGroup());
        addButton(272, bottom, 64, text("defaults"), button -> restoreDefaults());
        addButton(340, bottom, 54, text("undo"), button -> undo());
        addButton(398, bottom, 54, text("share"), button -> openShareScreen());
        addButton(456, bottom, 54, text("import"), button -> openImportScreen());
        addButton(width - 176, bottom, 74, text("save"), button -> save());
        addButton(width - 98, bottom, 78, text("cancel"), button -> onClose());
        addButton(layout.memberLeft(), 52, layout.memberWidth(), text("held_exact"), button -> addHeldExact());
        resetPreviewAnimation();
        syncSelection();
    }

    private void addButton(int x, int y, int width, Component label, Button.OnPress press) {
        addRenderableWidget(Button.builder(label, press).bounds(x, y, width, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF);
        graphics.text(font, Component.literal(layer == Layer.PRIMARY ? "Primary" : "Secondary"),
                GROUP_LEFT, 10, 0xFF7FD4FF);
        EditorLayout layout = layout();
        updateScrollbarDragging(layout, mouseY);
        drawGroups(graphics, mouseX, mouseY);
        drawCreativeBrowser(graphics, layout, mouseX, mouseY);
        drawPreview(graphics, layout);
        updateDragFeedback(layout, mouseX, mouseY);
        drawMembers(graphics, layout, mouseX, mouseY);
        graphics.text(font, status, 20, height - 40, 0xFFFFC14D);
    }

    private void drawGroups(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, text("groups"), GROUP_LEFT, 32, 0xFFFFFFFF);
        List<PaletteGroup> groups = draft().groups();
        int rows = groupRows();
        groupScroll = Math.max(0, Math.min(groupScroll, maxGroupScroll()));
        int start = groupScroll;
        for (int row = 0; row < rows && start + row < groups.size(); row++) {
            int index = start + row;
            int y = 50 + row * 18;
            int color = index == selectedGroup ? 0xAA3275A8 : 0x88202020;
            graphics.fill(GROUP_LEFT, y, GROUP_LEFT + GROUP_WIDTH, y + 16, color);
            boolean builtin = builtinGroup(groups.get(index).id()) != null;
            int nameLeft = builtin ? GROUP_LEFT + 12 : GROUP_LEFT + 4;
            if (builtin) {
                graphics.text(font, "◆", GROUP_LEFT + 3, y + 4, 0xFFFFC14D);
            }
            String name = elideMiddle(groups.get(index).displayName(), GROUP_LEFT + GROUP_WIDTH - nameLeft - 4);
            graphics.text(font, name, nameLeft, y + 4, 0xFFFFFFFF);
            if (inside(mouseX, mouseY, GROUP_LEFT, y, GROUP_WIDTH, 16)
                    && !name.equals(groups.get(index).displayName())) {
                graphics.setTooltipForNextFrame(font, Component.literal(groups.get(index).displayName()), mouseX, mouseY);
            }
        }
        drawGroupScrollbar(graphics, groups.size(), rows);
    }

    private void drawGroupScrollbar(GuiGraphicsExtractor graphics, int groupCount, int rows) {
        int trackLeft = GROUP_LEFT + GROUP_WIDTH + 2;
        int trackHeight = rows * 18;
        graphics.fill(trackLeft, 50, trackLeft + 6, 50 + trackHeight, 0x88303030);
        if (groupCount <= rows) {
            graphics.fill(trackLeft, 50, trackLeft + 6, 50 + trackHeight, 0xFF777777);
            return;
        }
        int thumbHeight = Math.max(12, trackHeight * rows / groupCount);
        int thumbY = 50 + (trackHeight - thumbHeight) * groupScroll / (groupCount - rows);
        graphics.fill(trackLeft, thumbY, trackLeft + 6, thumbY + thumbHeight,
                groupScrollbarDragging ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    private void drawCreativeBrowser(
            GuiGraphicsExtractor graphics,
            EditorLayout layout,
            int mouseX,
            int mouseY
    ) {
        drawCreativeTabs(graphics, layout, mouseX, mouseY);
        List<ItemStack> items = filteredCreativeItems();
        int columns = layout.gridColumns();
        int rows = layout.gridRows(height);
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
            EditorLayout layout,
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
        int firstX = layout.browserLeft() + (creativeTabs.size() > visible ? 20 : 0);
        int count = Math.min(visible, creativeTabs.size() - Math.min(tabStart, creativeTabs.size()));
        for (int offset = 0; offset < count; offset++) {
            CreativeModeTab tab = creativeTabs.get(tabStart + offset);
            int x = firstX + offset * (TAB_SIZE + 2);
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
            EditorLayout layout,
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

    private void drawPreview(GuiGraphicsExtractor graphics, EditorLayout layout) {
        int left = layout.previewLeft();
        int top = 28;
        int previewWidth = layout.previewWidth();
        int previewHeight = height - 76;
        if (previewWidth < 60 || previewHeight < 80) {
            return;
        }
        graphics.fill(left, top, left + previewWidth, top + previewHeight, 0x33202020);
        graphics.outline(left, top, previewWidth, previewHeight, 0x66777777);
        graphics.centeredText(font, text("preview"), left + previewWidth / 2, top + 6, 0xFFBBBBBB);
        PaletteGroup group = selected();
        if (group == null || group.members().isEmpty()) {
            graphics.centeredText(font, text("preview_empty"), left + previewWidth / 2,
                    top + previewHeight / 2, 0xFF999999);
            return;
        }
        List<ItemStack> stacks = group.members().stream()
                .map(member -> ClientPaletteItemCodec.resolve(minecraft, member).orElse(ItemStack.EMPTY))
                .toList();
        previewSelection = Math.floorMod(previewSelection, stacks.size());
        RadialMenuSnapshot<ItemStack> snapshot = new RadialMenuSnapshot<>(stacks, previewSelection);
        int centerX = left + previewWidth / 2;
        int centerY = top + previewHeight / 2;
        int maximumRadius = Math.max(0, Math.min(previewWidth / 2 - RadialWheelVisuals.ITEM_HALF_SIZE - 6,
                previewHeight / 2 - RadialWheelVisuals.ITEM_HALF_SIZE - 18));
        int preferredRadius = RadialWheelVisuals.MINIMUM_RADIUS
                + (int) Math.round(Math.max(0, stacks.size() - 1) * 1.8);
        int radius = Math.min(maximumRadius, preferredRadius);
        long now = System.currentTimeMillis();
        if (previewRotation == null) {
            previewRotation = RadialRotationState.idle(now, PREVIEW_ROTATION_MILLIS);
        }
        var slots = RadialGeometry.slots(snapshot, new HudPoint(centerX, centerY), radius,
                new RadialAnimationState(RadialAnimationMode.OFF, now, 1), now,
                previewRotation.offsetRadians(now));
        for (var slot : slots) {
            RadialWheelVisuals.renderItem(graphics, slot.value(), slot.x(), slot.y());
        }
        ItemStack selectedStack = stacks.get(previewSelection);
        Component label = selectedStack.isEmpty()
                ? Component.literal(group.members().get(previewSelection).itemId())
                : selectedStack.getHoverName();
        int labelWidth = Math.min(font.width(label), Math.max(0, previewWidth - 12));
        String labelText = elideMiddle(label.getString(), labelWidth);
        int textWidth = font.width(labelText);
        int labelY = centerY + 12;
        graphics.fill(centerX - textWidth / 2 - 3, labelY - 2,
                centerX + (textWidth + 1) / 2 + 3, labelY + font.lineHeight + 2, 0x90000000);
        graphics.centeredText(font, labelText, centerX, labelY, 0xFFFFFFFF);
    }

    private void drawMembers(
            GuiGraphicsExtractor graphics,
            EditorLayout layout,
            int mouseX,
            int mouseY
    ) {
        int left = layout.memberLeft();
        PaletteGroup group = selected();
        int count = group == null ? 0 : group.members().size();
        graphics.text(font, text("members_count", count), left, 78, 0xFFFFFFFF);
        if (group == null) {
            return;
        }
        int top = 94;
        int rows = memberRows();
        int start = memberPage * rows;
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
            graphics.fill(left + 2, floatingY + 3, layout.memberRight() + 2, floatingY + 19, 0x66000000);
            drawMemberRow(
                    graphics, layout, group.members().get(draggedMember), floatingY, mouseX, mouseY, true
            );
        }
        if (transition >= 1.0) {
            previousDragTarget = dragTarget;
        }
    }

    private void drawMemberRow(
            GuiGraphicsExtractor graphics,
            EditorLayout layout,
            PaletteMember member,
            int y,
            int mouseX,
            int mouseY,
            boolean floating
    ) {
        int left = layout.memberLeft();
        graphics.fill(left, y, layout.memberRight(), y + 16, floating ? 0xDD5A4628 : 0x88202020);
        if (floating) {
            graphics.outline(left, y, layout.memberWidth(), 16, 0xFFFFC14D);
        }
        ItemStack stack = ClientPaletteItemCodec.resolve(minecraft, member).orElse(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            graphics.item(stack, left, y);
        }
        String suffix = member.matchMode() == PaletteMatchMode.EXACT_COMPONENTS ? " *" : "";
        String fullId = member.itemId() + suffix;
        int textLeft = left + 20;
        int textWidth = Math.max(0, layout.memberRight() - textLeft - 4);
        String shownId = elideMiddle(fullId, textWidth);
        graphics.enableScissor(textLeft, y, layout.memberRight() - 3, y + 16);
        graphics.text(font, shownId, textLeft, y + 4, 0xFFFFFFFF);
        graphics.disableScissor();
        if (!floating && inside(mouseX, mouseY, left, y, layout.memberWidth(), 16)) {
            List<Component> tooltip = new ArrayList<>();
            if (!stack.isEmpty()) {
                tooltip.add(stack.getHoverName());
            }
            tooltip.add(Component.literal(member.itemId()).withStyle(ChatFormatting.GRAY));
            if (member.matchMode() == PaletteMatchMode.EXACT_COMPONENTS) {
                tooltip.add(text("exact_components").copy().withStyle(ChatFormatting.GOLD));
            }
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        EditorLayout layout = layout();
        if (event.button() == 0 && startGroupScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        if (event.button() == 0 && startBrowserScrollbarDrag(layout, mouseX, mouseY)) {
            return true;
        }
        if (event.button() == 0 && inside(mouseX, mouseY, GROUP_LEFT, 50, GROUP_WIDTH, groupRows() * 18)) {
            int index = groupScroll + (mouseY - 50) / 18;
            if (index < draft().groups().size()) {
                selectedGroup = index;
                memberPage = 0;
                previewSelection = 0;
                resetPreviewAnimation();
                syncSelection();
                return true;
            }
        }
        if (event.button() == 0 && clickCreativeTab(layout, mouseX, mouseY)) {
            return true;
        }
        int gridIndex = gridIndex(layout, mouseX, mouseY);
        if (event.button() == 0 && gridIndex >= 0) {
            List<ItemStack> items = filteredCreativeItems();
            int itemIndex = itemScrollRow * layout.gridColumns() + gridIndex;
            if (itemIndex < items.size()) {
                addItem(items.get(itemIndex), event.hasShiftDown());
                return true;
            }
        }
        int member = memberIndex(layout, mouseX, mouseY);
        if (member >= 0) {
            if (event.button() == 1) {
                removeMember(member);
            } else if (event.button() == 0) {
                draggedMember = member;
                dragTarget = member;
                previousDragTarget = member;
                dragMouseY = mouseY;
                draggedVisualY = 94 + (member - memberPage * memberRows()) * 18;
                dragTransitionStartedAt = System.currentTimeMillis();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (browserScrollbarDragging || groupScrollbarDragging) {
            browserScrollbarDragging = false;
            groupScrollbarDragging = false;
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
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        EditorLayout layout = layout();
        double amount = amountY != 0.0 ? amountY : amountX;
        if (mouseX < GROUP_LEFT + GROUP_WIDTH) {
            groupScroll = Math.max(0, Math.min(maxGroupScroll(), groupScroll + (amount < 0 ? 1 : -1)));
            return true;
        }
        if (inside((int) mouseX, (int) mouseY, layout.browserLeft(), 52,
                layout.browserWidth(), height - 100)) {
            List<ItemStack> items = filteredCreativeItems();
            int max = maxItemScroll(items.size(), layout.gridColumns(), layout.gridRows(height));
            itemScrollRow = Math.max(0, Math.min(max, itemScrollRow + (amount < 0 ? 1 : -1)));
            return true;
        }
        if (inside((int) mouseX, (int) mouseY, layout.previewLeft(), 28,
                layout.previewWidth(), height - 76)) {
            PaletteGroup group = selected();
            if (group != null && !group.members().isEmpty()) {
                int steps = previewScroll.add(amount);
                if (steps != 0) {
                    int selectionSteps = -steps;
                    long now = System.currentTimeMillis();
                    previewSelection = Math.floorMod(previewSelection + selectionSteps, group.members().size());
                    if (previewRotation == null) {
                        previewRotation = RadialRotationState.idle(now, PREVIEW_ROTATION_MILLIS);
                    }
                    previewRotation = previewRotation.retarget(
                            selectionSteps, group.members().size(), now, PREVIEW_ROTATION_MILLIS
                    );
                }
            }
            return true;
        }
        if (mouseX >= layout.memberLeft()) {
            PaletteGroup selected = selected();
            if (selected != null) {
                int max = Math.max(0, (selected.members().size() - 1) / memberRows());
                memberPage = Math.max(0, Math.min(max, memberPage + (amount < 0 ? 1 : -1)));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amountX, amountY);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
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

    private boolean clickCreativeTab(EditorLayout layout, int mouseX, int mouseY) {
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
        int firstX = layout.browserLeft() + (creativeTabs.size() > visible ? 20 : 0);
        int offset = (mouseX - firstX) / (TAB_SIZE + 2);
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
        memberPage = 0;
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
        memberPage = 0;
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
        memberPage = 0;
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
        memberPage = 0;
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
        memberPage = 0;
        previewSelection = 0;
        clearDragFeedback();
        resetPreviewAnimation();
        syncSelection();
        status = text("defaults_pending");
    }

    private void undo() {
        if (!undo.isEmpty()) {
            EditorSnapshot snapshot = undo.pop();
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
        int maxPage = Math.max(0, (members.size() - 1) / memberRows());
        memberPage = Math.min(memberPage, maxPage);
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
        undo.push(new EditorSnapshot(primary.groups(), secondary.groups()));
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
            memberPage = 0;
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

    private int gridIndex(EditorLayout layout, int mouseX, int mouseY) {
        int rows = layout.gridRows(height);
        int width = layout.gridColumns() * GRID_CELL;
        if (!inside(mouseX, mouseY, layout.browserLeft(), GRID_TOP, width, rows * GRID_CELL)) return -1;
        return (mouseY - GRID_TOP) / GRID_CELL * layout.gridColumns()
                + (mouseX - layout.browserLeft()) / GRID_CELL;
    }

    private int memberIndex(EditorLayout layout, int mouseX, int mouseY) {
        int top = 94;
        if (!inside(mouseX, mouseY, layout.memberLeft(), top, layout.memberWidth(), memberRows() * 18)) return -1;
        int index = memberPage * memberRows() + (mouseY - top) / 18;
        PaletteGroup group = selected();
        return group != null && index < group.members().size() ? index : -1;
    }

    private void updateDragFeedback(EditorLayout layout, int mouseX, int mouseY) {
        if (draggedMember < 0) {
            return;
        }
        dragMouseY = mouseY;
        PaletteGroup group = selected();
        if (group == null || mouseX < layout.memberLeft() || mouseX >= layout.memberRight()) {
            return;
        }
        int rows = memberRows();
        int start = memberPage * rows;
        int visibleCount = Math.min(rows, group.members().size() - start);
        if (visibleCount <= 0) {
            return;
        }
        int row = Math.max(0, Math.min(visibleCount - 1, (mouseY - 94) / 18));
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
    }

    private boolean startBrowserScrollbarDrag(EditorLayout layout, int mouseX, int mouseY) {
        int rows = layout.gridRows(height);
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

    private boolean startGroupScrollbarDrag(int mouseX, int mouseY) {
        int rows = groupRows();
        if (!inside(mouseX, mouseY, GROUP_LEFT + GROUP_WIDTH + 2, 50, 6, rows * 18)) {
            return false;
        }
        if (maxGroupScroll() <= 0) {
            return true;
        }
        groupScrollbarDragging = true;
        browserScrollbarDragging = false;
        updateGroupScrollbar(mouseY);
        return true;
    }

    private void updateScrollbarDragging(EditorLayout layout, int mouseY) {
        if (browserScrollbarDragging) {
            updateBrowserScrollbar(layout, mouseY);
        } else if (groupScrollbarDragging) {
            updateGroupScrollbar(mouseY);
        }
    }

    private void updateBrowserScrollbar(EditorLayout layout, int mouseY) {
        int rows = layout.gridRows(height);
        int itemCount = filteredCreativeItems().size();
        int totalRows = Math.max(1, (itemCount + layout.gridColumns() - 1) / layout.gridColumns());
        int maxScroll = Math.max(0, totalRows - rows);
        int trackHeight = rows * GRID_CELL;
        int thumbHeight = Math.max(12, trackHeight * rows / totalRows);
        itemScrollRow = scrollbarValue(mouseY, GRID_TOP, trackHeight, thumbHeight, maxScroll);
    }

    private void updateGroupScrollbar(int mouseY) {
        int rows = groupRows();
        int count = Math.max(1, draft().groups().size());
        int trackHeight = rows * 18;
        int thumbHeight = Math.max(12, trackHeight * rows / count);
        groupScroll = scrollbarValue(mouseY, 50, trackHeight, thumbHeight, maxGroupScroll());
    }

    private static int scrollbarValue(int mouseY, int top, int trackHeight, int thumbHeight, int maximum) {
        if (maximum <= 0 || trackHeight <= thumbHeight) {
            return 0;
        }
        double position = (double) (mouseY - top - thumbHeight / 2) / (trackHeight - thumbHeight);
        return Math.max(0, Math.min(maximum, (int) Math.round(position * maximum)));
    }

    private void revealSelectedGroup() {
        if (selectedGroup < 0) {
            groupScroll = 0;
            return;
        }
        int rows = groupRows();
        if (selectedGroup < groupScroll) {
            groupScroll = selectedGroup;
        } else if (selectedGroup >= groupScroll + rows) {
            groupScroll = selectedGroup - rows + 1;
        }
        groupScroll = Math.max(0, Math.min(groupScroll, maxGroupScroll()));
    }

    private int maxGroupScroll() {
        return Math.max(0, draft().groups().size() - groupRows());
    }

    private int groupRows() {
        return Math.max(1, (height - 98) / 18);
    }

    private void clampItemScroll(int itemCount, int columns, int rows) {
        itemScrollRow = Math.max(0, Math.min(itemScrollRow, maxItemScroll(itemCount, columns, rows)));
    }

    private static int maxItemScroll(int itemCount, int columns, int rows) {
        int totalRows = (itemCount + columns - 1) / columns;
        return Math.max(0, totalRows - rows);
    }

    private int memberRows() {
        return Math.max(1, (height - 140) / 18);
    }

    private EditorLayout layout() {
        int browserLeft = GROUP_LEFT + GROUP_WIDTH + COLUMN_GAP;
        int memberWidth = Math.max(180, Math.min(280, width / 4));
        int memberLeft = width - 20 - memberWidth;
        int available = Math.max(180, memberLeft - browserLeft - COLUMN_GAP);
        int browserWidth = Math.max(140, Math.min(204, available * 45 / 100));
        int gridColumns = Math.max(6, Math.min(9, (browserWidth - 8) / GRID_CELL));
        browserWidth = Math.max(browserWidth, gridColumns * GRID_CELL + 8);
        int previewLeft = browserLeft + browserWidth + COLUMN_GAP;
        int previewWidth = Math.max(0, memberLeft - COLUMN_GAP - previewLeft);
        return new EditorLayout(browserLeft, browserWidth, previewLeft, previewWidth, memberLeft, memberWidth,
                gridColumns);
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

    private record EditorLayout(
            int browserLeft,
            int browserWidth,
            int previewLeft,
            int previewWidth,
            int memberLeft,
            int memberWidth,
            int gridColumns
    ) {
        int browserRight() {
            return browserLeft + browserWidth;
        }

        int memberRight() {
            return memberLeft + memberWidth;
        }

        int visibleTabs() {
            return Math.max(1, (browserWidth - 40) / (TAB_SIZE + 2));
        }

        int gridRows(int screenHeight) {
            return Math.max(1, (screenHeight - GRID_TOP - 48) / GRID_CELL);
        }
    }

    private record EditorSnapshot(List<PaletteGroup> primary, List<PaletteGroup> secondary) {
        private EditorSnapshot {
            primary = List.copyOf(primary);
            secondary = List.copyOf(secondary);
        }
    }
}
