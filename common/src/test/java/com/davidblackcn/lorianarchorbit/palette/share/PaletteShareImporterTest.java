package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class PaletteShareImporterTest {
    @Test
    void keepBothRenamesOnlyConflictsInTheirOwnLayer() {
        PaletteGroup existing = group("stone", "minecraft:stone");
        PaletteShareBundle bundle = new PaletteShareBundle("pack", List.of(
                new PaletteShareEntry(PaletteShareLayer.PRIMARY, group("stone", "minecraft:dirt")),
                new PaletteShareEntry(PaletteShareLayer.SECONDARY, group("stone", "minecraft:granite"))
        ));

        PaletteImportResult result = PaletteShareImporter.merge(
                List.of(existing), List.of(), bundle, PaletteImportConflictPolicy.KEEP_BOTH
        );

        assertEquals(List.of("stone", "stone_imported"), result.primary().stream().map(PaletteGroup::id).toList());
        assertEquals(List.of("stone"), result.secondary().stream().map(PaletteGroup::id).toList());
        assertEquals(2, result.imported());
        assertEquals(1, result.renamed());
    }

    @Test
    void replaceAndSkipReportTheirActions() {
        PaletteGroup existing = group("stone", "minecraft:stone");
        PaletteShareBundle bundle = new PaletteShareBundle("pack", List.of(
                new PaletteShareEntry(PaletteShareLayer.PRIMARY, group("stone", "minecraft:dirt"))
        ));

        PaletteImportResult replaced = PaletteShareImporter.merge(
                List.of(existing), List.of(), bundle, PaletteImportConflictPolicy.REPLACE
        );
        assertEquals("minecraft:dirt", replaced.primary().getFirst().iconItemId());
        assertEquals(1, replaced.replaced());

        PaletteImportResult skipped = PaletteShareImporter.merge(
                List.of(existing), List.of(), bundle, PaletteImportConflictPolicy.SKIP
        );
        assertEquals(existing, skipped.primary().getFirst());
        assertEquals(1, skipped.skipped());
    }

    private static PaletteGroup group(String id, String item) {
        return new PaletteGroup(id, id, item, List.of(new PaletteMember(item)));
    }
}
