package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteImportConflictPolicy;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareBundle;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareCodec;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareException;
import com.davidblackcn.lorianarchorbit.palette.share.PaletteShareFiles;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class PaletteImportScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private final PaletteEditorScreen parent;
    private final Path configDirectory;
    private final PaletteShareCodec codec = new PaletteShareCodec();
    private final List<ImportSource> sources = new ArrayList<>();
    private PaletteImportConflictPolicy policy = PaletteImportConflictPolicy.KEEP_BOTH;
    private Button policyButton;
    private int selected = -1;
    private int scroll;
    private Component status = Component.empty();

    PaletteImportScreen(PaletteEditorScreen parent, Path configDirectory) {
        super(Component.translatable("palette_import.lorian_arch_orbit.title"));
        this.parent = parent;
        this.configDirectory = configDirectory;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, width - 40);
        int left = (width - panelWidth) / 2;
        int bottom = height - 28;
        addButton(left, bottom, 82, text("refresh"), button -> refreshSources());
        policyButton = addButton(left + 86, bottom, 150, policyLabel(), button -> cyclePolicy());
        addButton(left + panelWidth - 170, bottom, 82, text("confirm"), button -> confirmImport());
        addButton(left + panelWidth - 84, bottom, 84, text("cancel"), button -> onClose());
        refreshSources();
    }

    private Button addButton(int x, int y, int width, Component label, Button.OnPress press) {
        Button button = Button.builder(label, press).bounds(x, y, width, 20).build();
        addRenderableWidget(button);
        return button;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        int panelWidth = Math.min(560, width - 40);
        int left = (width - panelWidth) / 2;
        graphics.text(font, text("instructions"), left, 32, 0xFFBBBBBB);
        graphics.text(font, Component.literal(PaletteShareFiles.shareDirectory(configDirectory).toString()),
                left, 48, 0xFF888888);
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        for (int row = 0; row < rows && scroll + row < sources.size(); row++) {
            int index = scroll + row;
            ImportSource source = sources.get(index);
            int y = 70 + row * ROW_HEIGHT;
            boolean active = selected == index;
            boolean hovered = inside(mouseX, mouseY, left, y, panelWidth, 21);
            graphics.fill(left, y, left + panelWidth, y + 21,
                    active ? 0xAA3275A8 : hovered ? 0xAA4D6A7D : 0x88202020);
            graphics.text(font, source.label(), left + 5, y + 4, 0xFFFFFFFF);
            graphics.text(font, Component.translatable(
                    "palette_import.lorian_arch_orbit.summary",
                    source.bundle().entries().size(), source.members(), source.missing()
            ), left + 190, y + 4, source.missing() > 0 ? 0xFFFFC14D : 0xFFBBBBBB);
        }
        if (selected >= 0 && selected < sources.size()) {
            ImportSource source = sources.get(selected);
            graphics.text(font, Component.translatable(
                    "palette_import.lorian_arch_orbit.preview", source.bundle().name()
            ), left, height - 50, 0xFFFFFFFF);
        }
        graphics.text(font, status, left + 260, height - 50, 0xFFFFC14D);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }
        int panelWidth = Math.min(560, width - 40);
        int left = (width - panelWidth) / 2;
        if (inside((int) event.x(), (int) event.y(), left, 70, panelWidth, visibleRows() * ROW_HEIGHT)) {
            int index = scroll + ((int) event.y() - 70) / ROW_HEIGHT;
            if (index < sources.size()) {
                selected = index;
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

    private void refreshSources() {
        sources.clear();
        int invalidFiles = 0;
        String clipboard = minecraft.keyboardHandler.getClipboard();
        try {
            PaletteShareBundle bundle = codec.decode(clipboard);
            sources.add(source(text("clipboard").getString(), bundle));
        } catch (PaletteShareException ignored) {
            // Ordinary clipboard text is expected and is not presented as an error.
        }
        try {
            for (Path file : PaletteShareFiles.list(configDirectory)) {
                try {
                    sources.add(source(file.getFileName().toString(),
                            PaletteShareFiles.read(configDirectory, file, codec)));
                } catch (IOException | PaletteShareException exception) {
                    invalidFiles++;
                }
            }
        } catch (IOException exception) {
            status = Component.translatable("palette_import.lorian_arch_orbit.read_failed", exception.getMessage());
        }
        selected = sources.isEmpty() ? -1 : 0;
        scroll = 0;
        if (sources.isEmpty()) {
            status = text("empty");
        } else if (invalidFiles > 0) {
            status = Component.translatable("palette_import.lorian_arch_orbit.invalid_files", invalidFiles);
        } else {
            status = Component.empty();
        }
    }

    private ImportSource source(String label, PaletteShareBundle bundle) {
        int members = bundle.entries().stream().mapToInt(entry -> entry.group().members().size()).sum();
        int missing = (int) bundle.entries().stream().flatMap(entry -> entry.group().members().stream())
                .map(PaletteMember::itemId).filter(id -> !itemExists(id)).count();
        return new ImportSource(label, bundle, members, missing);
    }

    private static boolean itemExists(String id) {
        try {
            return BuiltInRegistries.ITEM.containsKey(Identifier.parse(id));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private void cyclePolicy() {
        policy = switch (policy) {
            case KEEP_BOTH -> PaletteImportConflictPolicy.REPLACE;
            case REPLACE -> PaletteImportConflictPolicy.SKIP;
            case SKIP -> PaletteImportConflictPolicy.KEEP_BOTH;
        };
        policyButton.setMessage(policyLabel());
    }

    private Component policyLabel() {
        return Component.translatable("palette_import.lorian_arch_orbit.policy."
                + policy.name().toLowerCase(java.util.Locale.ROOT));
    }

    private void confirmImport() {
        if (selected < 0 || selected >= sources.size()) {
            status = text("select_source");
            return;
        }
        parent.applyImport(sources.get(selected).bundle(), policy);
        minecraft.setScreenAndShow(parent);
    }

    private int visibleRows() {
        return Math.max(1, (height - 146) / ROW_HEIGHT);
    }

    private int maxScroll() {
        return Math.max(0, sources.size() - visibleRows());
    }

    private Component text(String suffix) {
        return Component.translatable("palette_import.lorian_arch_orbit." + suffix);
    }

    private static boolean inside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private record ImportSource(String label, PaletteShareBundle bundle, int members, int missing) {
    }
}
