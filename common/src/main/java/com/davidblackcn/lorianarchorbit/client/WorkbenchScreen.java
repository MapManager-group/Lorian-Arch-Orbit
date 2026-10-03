package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;

/** Shared navigation and modal menus. All input is already in canvas coordinates. */
abstract class WorkbenchScreen extends AdaptivePaletteScreen {
    protected final EditorSession session;
    private final String pageId;
    private List<MenuEntry> menu = List.of();
    private WorkbenchMenuLayout menuLayout;
    private int menuStart;
    private final WorkbenchMenuNavigation menuNavigation = new WorkbenchMenuNavigation();
    private int lastPointerX = Integer.MIN_VALUE, lastPointerY = Integer.MIN_VALUE;
    private final List<Button> menuButtons = new ArrayList<>();
    private boolean consumeRelease;
    private Button pendingAnchor, menuAnchor;
    private boolean scrollbarDragging;
    private int scrollbarGrab;
    record MenuEntry(Component label, Runnable action, boolean enabled) {
        MenuEntry(Component label, Runnable action) { this(label, action, true); }
    }
    WorkbenchScreen(EditorSession session, String pageId, Component title) {
        super(title); this.session = session; this.pageId = pageId;
    }
    @Override protected final void initContent() {
        closeMenu(); initWorkbench();
        if (pageId != null) dropdown(width - 148, 4, 136,
                EditorSession.PAGES.stream().filter(page -> page.id().equals(pageId)).findFirst()
                        .map(page -> EditorSession.text(page.name())).orElse(title), () -> {
            List<MenuEntry> entries = new ArrayList<>();
            for (var page : EditorSession.PAGES) entries.add(new MenuEntry(
                    EditorSession.text(page.name()),
                    () -> session.show(page.id())));
            menu(width - 200, 26, entries);
        });
    }
    protected abstract void initWorkbench();
    protected Button action(int x, int y, int w, Component label, Runnable callback) {
        Button button = Button.builder(label, b -> callback.run()).bounds(x, y, w, 20).build();
        button.setTooltip(Tooltip.create(label));
        return addRenderableWidget(button);
    }
    protected Button dropdown(int x, int y, int w, Component label, Runnable callback) {
        Button button = new WorkbenchFlatButton(x, y, w, label, b -> {
            pendingAnchor = b;
            try { callback.run(); } finally { pendingAnchor = null; }
        }, -1, () -> menuOpen() && menuAnchor != null && menuAnchor.getX() == x && menuAnchor.getY() == y);
        button.setTooltip(Tooltip.create(label));
        return addRenderableWidget(button);
    }
    protected final void menu(int x, int y, List<MenuEntry> entries) {
        if (entries.isEmpty()) return;
        menu = List.copyOf(entries); menuStart = 0; menuNavigation.reset();
        menuAnchor = pendingAnchor;
        int longest = entries.stream().mapToInt(e -> font.width(e.label())).max().orElse(0);
        int preferred = WorkbenchMenuLayout.preferredWidth(longest, false);
        menuLayout = popupLayout(x, y, preferred, entries.size());
        if (menuAnchor == null && menuLayout.rows() < entries.size()) menuLayout = popupLayout(x, y,
                WorkbenchMenuLayout.preferredWidth(longest, true), entries.size());
        scrollbarDragging = false;
        buildMenu();
    }
    private WorkbenchMenuLayout popupLayout(int x, int y, int preferred, int count) {
        return menuAnchor == null ? WorkbenchMenuLayout.calculate(width, height, x, y, preferred, count)
                : WorkbenchMenuLayout.anchored(width, height, menuAnchor.getX(), menuAnchor.getY(),
                        menuAnchor.getWidth(), menuAnchor.getHeight(), count);
    }
    final void confirm(Component explanation, Component accept, Runnable action) {
        pendingAnchor = null;
        int preferred = WorkbenchMenuLayout.preferredWidth(Math.max(font.width(explanation),
                Math.max(font.width(accept), font.width(EditorSession.text("continue_editing")))), false);
        menu((width - preferred) / 2, (height - 60) / 2, List.of(
                new MenuEntry(explanation, () -> {}, false),
                new MenuEntry(EditorSession.text("continue_editing"), () -> {}),
                new MenuEntry(accept, action)));
        menuNavigation.focus(1); buildMenu();
    }
    private void buildMenu() {
        menuButtons.clear();
        for (int row = 0; row < menuLayout.rows(); row++) {
            int index = menuStart + row;
            MenuEntry entry = menu.get(index);
            Button button = new WorkbenchFlatButton(menuLayout.x() + 1, menuLayout.y() + 1 + row * WorkbenchMenuLayout.ROW_HEIGHT,
                    menuLayout.width() - 2 - (menuLayout.rows() < menu.size() ? 8 : 0), entry.label(), b -> {
                activateMenuEntry(entry);
            }, index, null);
            button.active = entry.enabled(); button.setFocused(menuNavigation.keyboard() && index == menuNavigation.index());
            button.setTooltip(Tooltip.create(entry.label()));
            menuButtons.add(button);
        }
    }
    private void activateMenuEntry(MenuEntry entry) {
        Button anchor = menuAnchor;
        closeMenu();
        pendingAnchor = anchor;
        try { entry.action().run(); } finally { pendingAnchor = null; }
    }
    private void closeMenu() { menu = List.of(); menuButtons.clear(); menuAnchor = null; scrollbarDragging = false; menuNavigation.reset(); }
    protected final boolean menuOpen() { return !menu.isEmpty(); }
    @Override protected final void renderContent(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        boolean pointerMoved = lastPointerX != Integer.MIN_VALUE && (x != lastPointerX || y != lastPointerY);
        lastPointerX = x; lastPointerY = y;
        super.renderContent(graphics, menuOpen() ? -1000 : x, menuOpen() ? -1000 : y, tick);
        renderWorkbench(graphics, menuOpen() ? -1000 : x, menuOpen() ? -1000 : y, tick);
        if (menuOpen()) {
            if (scrollbarDragging) scrollMenu(menuLayout.scrollAt(y, scrollbarGrab, menu.size()));
            int hovered = -1;
            for (int i = 0; i < menuButtons.size(); i++) {
                var button = menuButtons.get(i);
                if (button.active && button.isMouseOver(x, y)) hovered = menuStart + i;
            }
            menuNavigation.pointer(hovered, pointerMoved);
            graphics.nextStratum();
            graphics.fill(menuLayout.x(), menuLayout.y(), menuLayout.x() + menuLayout.width(),
                    menuLayout.y() + menuLayout.height(), 0xFFBBBBBB);
            for (int i = 0; i < menuButtons.size(); i++) {
                var button = menuButtons.get(i);
                button.setFocused(menuNavigation.keyboard() && menuNavigation.index() == menuStart + i);
                button.extractRenderState(graphics, menuNavigation.keyboard() ? -1000 : x, menuNavigation.keyboard() ? -1000 : y, tick);
            }
            if (menuLayout.rows() < menu.size()) {
                int right = menuLayout.x() + menuLayout.width() - 1, top = menuLayout.thumbY(menuStart, menu.size());
                graphics.fill(right - 8, menuLayout.y() + 1, right, menuLayout.y() + menuLayout.height() - 1, 0xFF191919);
                graphics.fill(right - 6, top, right - 1, top + menuLayout.thumbHeight(menu.size()), 0xFFCCCCCC);
            }
        }
    }
    protected abstract void renderWorkbench(GuiGraphicsExtractor graphics, int x, int y, float tick);
    @Override protected final boolean mouseClickedContent(MouseButtonEvent event, boolean twice) {
        if (menuOpen()) {
            consumeRelease = true;
            if (event.button() == 0 && menuLayout.rows() < menu.size()
                    && event.x() >= menuLayout.x() + menuLayout.width() - 9 && event.x() < menuLayout.x() + menuLayout.width() - 1
                    && event.y() > menuLayout.y() && event.y() < menuLayout.y() + menuLayout.height() - 1) {
                int thumbY = menuLayout.thumbY(menuStart, menu.size()), thumbHeight = menuLayout.thumbHeight(menu.size());
                scrollbarGrab = event.y() >= thumbY && event.y() < thumbY + thumbHeight ? (int) event.y() - thumbY : thumbHeight / 2;
                scrollbarDragging = true; menuNavigation.reset(); scrollMenu(menuLayout.scrollAt(event.y(), scrollbarGrab, menu.size())); return true;
            }
            for (var button : List.copyOf(menuButtons)) if (button.mouseClicked(event, twice)) return true;
            if (event.x() >= menuLayout.x() && event.x() < menuLayout.x() + menuLayout.width()
                    && event.y() >= menuLayout.y() && event.y() < menuLayout.y() + menuLayout.height()) return true;
            closeMenu(); return true;
        }
        return clickWorkbench(event, twice);
    }
    protected boolean clickWorkbench(MouseButtonEvent event, boolean twice) { return super.mouseClickedContent(event, twice); }
    @Override protected boolean mouseClickedOutsideCanvas() {
        if (!menuOpen()) return false;
        closeMenu(); consumeRelease = true; return true;
    }
    @Override protected boolean mouseReleasedContent(MouseButtonEvent event) {
        scrollbarDragging = false;
        if (consumeRelease) { consumeRelease = false; return true; }
        return super.mouseReleasedContent(event);
    }
    @Override protected final boolean mouseScrolledContent(double x, double y, double ax, double ay) {
        if (menuOpen()) {
            if (ay != 0 || ax != 0) {
                menuNavigation.reset(); scrollMenu(menuStart + ((ay != 0 ? ay : ax) < 0 ? 1 : -1));
            }
            return true;
        }
        return scrollWorkbench(x, y, ax, ay);
    }
    protected boolean scrollWorkbench(double x, double y, double ax, double ay) { return super.mouseScrolledContent(x, y, ax, ay); }
    private void scrollMenu(int start) {
        menuStart = Math.clamp(start, 0, menu.size() - menuLayout.rows());
        buildMenu();
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (!menuOpen()) return super.keyPressed(event);
        if (event.key() == 256) closeMenu();
        else if (event.key() == 257 || event.key() == 335 || event.key() == 32) {
            int selected = menuNavigation.index();
            if (selected >= 0) {
                var entry = menu.get(selected); if (entry.enabled()) activateMenuEntry(entry);
            } else moveMenuFocus(1);
        } else if (event.key() == 258 || event.key() == 264 || event.key() == 265) {
            int direction = event.key() == 265 || (event.modifiers() & 1) != 0 ? -1 : 1;
            moveMenuFocus(direction);
        }
        return true;
    }
    private void moveMenuFocus(int direction) {
        menuNavigation.move(direction, menu.size(), index -> menu.get(index).enabled());
        int focused = menuNavigation.index();
        if (focused >= 0) menuStart = Math.clamp(menuStart, Math.max(0, focused - menuLayout.rows() + 1),
                Math.min(focused, menu.size() - menuLayout.rows()));
        buildMenu();
    }
    @Override public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        return menuOpen() || super.charTyped(event);
    }
    @Override public void onClose() { if (menuOpen()) closeMenu(); else session.exit(this); }
}
