package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PaletteShareFilesTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsUniqueSafeFilesAndReadsOnlyFromShareDirectory() throws Exception {
        PaletteShareCodec codec = new PaletteShareCodec();
        PaletteShareBundle bundle = new PaletteShareBundle("My / Palette", List.of(
                new PaletteShareEntry(PaletteShareLayer.PRIMARY, new PaletteGroup(
                        "stone", "Stone", "minecraft:stone", List.of(new PaletteMember("minecraft:stone"))
                ))
        ));

        Path first = PaletteShareFiles.export(temporaryDirectory, bundle, codec);
        Path second = PaletteShareFiles.export(temporaryDirectory, bundle, codec);

        assertTrue(first.startsWith(PaletteShareFiles.shareDirectory(temporaryDirectory)));
        assertTrue(!first.equals(second));
        assertEquals(bundle, PaletteShareFiles.read(temporaryDirectory, first, codec));
        assertEquals(2, PaletteShareFiles.list(temporaryDirectory).size());
        assertThrows(PaletteShareException.class,
                () -> PaletteShareFiles.read(temporaryDirectory, temporaryDirectory.resolve("outside.json"), codec));
    }
}
