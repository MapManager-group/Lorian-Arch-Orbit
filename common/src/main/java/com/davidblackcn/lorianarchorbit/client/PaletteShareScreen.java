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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PaletteShareScreen extends Screen {
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
    protected void init() {
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        shareName = new EditBox(font, left, 32, panelWidth, 20,
                Component.translatable("palette_share.lorian_arch_orbit.name"));
        shareName.setMaxLength(80);
        shareName.setHint(Component.translatable("palette_share.lorian_arch_orbit.name"));
        String defaultName = selected.size() == 1 && initial != null
                ? initial.group().displayName()
                : Component.translatable("palette_share.lorian_arch_orbit.default_name").getString();
        shareName.setValue(defaultName);
        addRenderableWidget(shareName);

        int bottom = height - 28;
        addButton(left, bottom, 70, text("all"), button -> selectAll());
        addButton(left + 74, bottom, 70, text("none"), button -> selected.clear());
        addButton(left + 152, bottom, 104, text("copy_code"), button -> copyCode());
        addButton(left + 260, bottom, 104, text("export_file"), button -> exportFile());
        addButton(left + panelWidth - 80, bottom, 80, text("back"), button -> onClose());
        if (entries.isEmpty()) {
            status = text("empty");
        }
    }

    private void addButton(int x, int y, int width, Component label, Button.OnPress press) {
        addRenderableWidget(Button.builder(label, press).bounds(x, y, width, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int panelWidth = Math.min(520, width - 40);
        int left = (width - panelWidth) / 2;
        graphics.text(font, text("instructions"), left, 58, 0xFFBBBBBB);
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
            graphics.text(font, "[" + layer + "] " + entry.group().displayName(), left + 21, y + 5, 0xFFFFFFFF);
            graphics.text(font, Component.literal(Integer.toString(entry.group().members().size())),
                    left + panelWidth - 28, y + 5, 0xFFBBBBBB);
        }
        graphics.text(font, Component.translatable("palette_share.lorian_arch_orbit.selected", selected.size()),
                left, height - 48, 0xFFFFC14D);
        graphics.text(font, status, left + 150, height - 48, 0xFFFFC14D);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
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
        return Math.max(1, (height - 154) / ROW_HEIGHT);
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
