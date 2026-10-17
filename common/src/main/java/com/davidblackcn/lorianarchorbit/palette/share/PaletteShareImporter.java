package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PaletteShareImporter {
    private PaletteShareImporter() {
    }

    public static PaletteImportResult merge(
            List<PaletteGroup> primary,
            List<PaletteGroup> secondary,
            PaletteShareBundle bundle,
            PaletteImportConflictPolicy policy
    ) {
        List<PaletteGroup> nextPrimary = new ArrayList<>(primary);
        List<PaletteGroup> nextSecondary = new ArrayList<>(secondary);
        int imported = 0;
        int renamed = 0;
        int replaced = 0;
        int skipped = 0;
        for (PaletteShareEntry entry : bundle.entries()) {
            List<PaletteGroup> target = entry.layer() == PaletteShareLayer.PRIMARY ? nextPrimary : nextSecondary;
            PaletteGroup incoming = entry.group();
            int existing = indexOf(target, incoming.id());
            if (existing < 0) {
                target.add(incoming);
                imported++;
                continue;
            }
            switch (Objects.requireNonNull(policy, "policy")) {
                case KEEP_BOTH -> {
                    target.add(withUniqueId(incoming, target));
                    imported++;
                    renamed++;
                }
                case REPLACE -> {
                    target.set(existing, incoming);
                    imported++;
                    replaced++;
                }
                case SKIP -> skipped++;
            }
        }
        return new PaletteImportResult(nextPrimary, nextSecondary, imported, renamed, replaced, skipped);
    }

    private static int indexOf(List<PaletteGroup> groups, String id) {
        for (int index = 0; index < groups.size(); index++) {
            if (groups.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    private static PaletteGroup withUniqueId(PaletteGroup group, List<PaletteGroup> target) {
        String base = group.id() + "_imported";
        String candidate = base;
        int suffix = 2;
        while (indexOf(target, candidate) >= 0) {
            candidate = base + '_' + suffix++;
        }
        return new PaletteGroup(candidate, group.displayName() + " (Imported)", group.iconItemId(), group.members());
    }
}
