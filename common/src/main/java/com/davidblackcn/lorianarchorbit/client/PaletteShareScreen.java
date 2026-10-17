package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareBundle;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareCodec;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareEntry;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareException;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareFiles;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareLayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PaletteShareScreen extends AdaptivePaletteScreen {
    private static final int ROW_HEIGHT = 20;
    private final PaletteEditorScreen parent;
    private final List<PaletteShareEntry> entries;
    private final Set<Integer> selected = new HashSet<>();
    private final PaletteShareCodec codec = new PaletteShareCodec();
    private final PaletteShareEntry initial;
    private EditBox shareName;
    private int scroll;
    private Component status = Component.empty();

    PaletteShareScreen(
            PaletteEditorScreen parent,
            List<PaletteShareEntry> entries,
            PaletteShareEntry initial
    ) {
        super(Component.translatable("palette_share.lorian_arch_orbit.title"));
        this.parent = parent;
        this.entries = List.copyOf(entries);
        this.initial = initial;
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).equals(initial)) {
                selected.add(index);
                break;
            }
        }
    }

    @Override
    protected void initContent() {
        PaletteDialogLayout layout = layout();
        int panelWidth = layout.width();
        int left = layout.left();
        String previousName = shareName == null ? null : shareName.getValue();
        shareName = new EditBox(font, left, 32, panelWidth, 20,
                Component.translatable("palette_share.lorian_arch_orbit.name"));
        shareName.setMaxLength(80);
        shareName.setHint(Component.translatable("palette_share.lorian_arch_orbit.name"));
        String defaultName = selected.size() == 1 && initial != null
                ? initial.group().displayName()
                : Component.translatable("palette_share.lorian_arch_orbit.default_name").getString();
        shareName.setValue(previousName == null ? defaultName : previousName);
        addRenderableWidget(shareName);

        addAction(0, text("all"), button -> selectAll());
        addAction(1, text("none"), button -> selected.clear());
        addAction(2, text("copy_code"), button -> copyCode());
        addAction(3, text("export_file"), button -> exportFile());
        addAction(4, text("back"), button -> onClose());
        if (entries.isEmpty()) {
            status = text("empty");
        }
    }

    private void addAction(int index, Component label, Button.OnPress press) {
        var action = layout().actions().get(index);
        Button button = Button.builder(label, press).bounds(action.x(), action.y(), action.width(), 20).build();
        button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(label));
        addRenderableWidget(button);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.renderContent(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        boundedText(graphics, text("instructions"), left, 58, panelWidth, 0xFFBBBBBB, mouseX, mouseY);
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        for (int row = 0; row < rows && scroll + row < entries.size(); row++) {
            int index = scroll + row;
            PaletteShareEntry entry = entries.get(index);
            int y = 78 + row * ROW_HEIGHT;
            boolean checked = selected.contains(index);
            boolean hovered = inside(mouseX, mouseY, left, y, panelWidth, 18);
            graphics.fill(left, y, left + panelWidth, y + 18,
                    checked ? 0xAA3275A8 : hovered ? 0xAA4D6A7D : 0x88202020);
            graphics.outline(left + 3, y + 3, 12, 12, checked ? 0xFFFFFFFF : 0xFF888888);
            if (checked) {
                graphics.centeredText(font, "✓", left + 9, y + 4, 0xFFFFFFFF);
            }
            String layer = entry.layer() == PaletteShareLayer.PRIMARY ? "P" : "S";
            boundedText(graphics, Component.literal("[" + layer + "] " + entry.group().displayName()),
                    left + 21, y + 5, panelWidth - 55, 0xFFFFFFFF, mouseX, mouseY);
            graphics.text(font, Component.literal(Integer.toString(entry.group().members().size())),
                    left + panelWidth - 28, y + 5, 0xFFBBBBBB);
        }
        graphics.text(font, Component.translatable("palette_share.lorian_arch_orbit.selected", selected.size()),
                left, layout().footerTop() - 26, 0xFFFFC14D);
        boundedText(graphics, status, left, layout().footerTop() - 14, panelWidth, 0xFFFFC14D, mouseX, mouseY);
    }

    @Override
    protected boolean mouseClickedContent(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClickedContent(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        if (inside((int) event.x(), (int) event.y(), left, 78, panelWidth, visibleRows() * ROW_HEIGHT)) {
            int index = scroll + ((int) event.y() - 78) / ROW_HEIGHT;
            if (index < entries.size()) {
                if (!selected.add(index)) {
                    selected.remove(index);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean mouseScrolledContent(double mouseX, double mouseY, double amountX, double amountY) {
        double amount = amountY != 0.0 ? amountY : amountX;
        scroll = Math.max(0, Math.min(maxScroll(), scroll + (amount < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    private void selectAll() {
        selected.clear();
        for (int index = 0; index < entries.size(); index++) {
            selected.add(index);
        }
    }

    private void copyCode() {
        try {
            PaletteShareBundle bundle = selectedBundle();
            minecraft.keyboardHandler.setClipboard(codec.encodeCode(bundle));
            status = text("copied");
        } catch (PaletteShareException exception) {
            status = Component.literal(exception.getMessage());
        }
    }

    private void exportFile() {
        try {
            Path file = PaletteShareFiles.export(
                    ClientConfigRuntime.configManager().directory(), selectedBundle(), codec
            );
            minecraft.keyboardHandler.setClipboard(file.toString());
            status = Component.translatable("palette_share.lorian_arch_orbit.exported", file.getFileName().toString());
        } catch (IOException | PaletteShareException exception) {
            status = Component.translatable("palette_share.lorian_arch_orbit.failed", exception.getMessage());
        }
    }

    private PaletteShareBundle selectedBundle() throws PaletteShareException {
        String name = shareName.getValue().strip();
        if (name.isBlank()) {
            throw new PaletteShareException("share name is empty");
        }
        List<PaletteShareEntry> chosen = new ArrayList<>();
        selected.stream().sorted().map(entries::get).forEach(chosen::add);
        if (chosen.isEmpty()) {
            throw new PaletteShareException("no groups are selected");
        }
        return new PaletteShareBundle(name, chosen);
    }

    private int visibleRows() {
        return layout().rows(78, ROW_HEIGHT);
    }

    private PaletteDialogLayout layout() {
        return PaletteDialogLayout.calculate(width, height, 520, 70, 70, 104, 104, 80);
    }

    private int maxScroll() {
        return Math.max(0, entries.size() - visibleRows());
    }

    private Component text(String suffix) {
        return Component.translatable("palette_share.lorian_arch_orbit." + suffix);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
