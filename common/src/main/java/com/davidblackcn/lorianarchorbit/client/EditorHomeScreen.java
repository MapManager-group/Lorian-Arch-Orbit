package com.davidblackcn.lorianarchorbit.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

final class EditorHomeScreen extends WorkbenchScreen {
    EditorHomeScreen(EditorSession session) { super(session, null, EditorSession.text("title")); }
    @Override protected void initWorkbench() {
        var layout = EditorHomeLayout.calculate(width, height, EditorSession.PAGES.size());
        int index = 0;
        for (var page : EditorSession.PAGES) {
            addRenderableWidget(new EditorPageCard(session, page, layout.x(index), layout.y(index), layout.cardWidth(), layout.cardHeight()));
            index++;
        }
    }
    @Override protected void renderWorkbench(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
    }
    @Override public void onClose() { if (menuOpen()) super.onClose(); else session.exit(); }
}
