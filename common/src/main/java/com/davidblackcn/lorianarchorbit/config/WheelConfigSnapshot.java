package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;

import java.util.List;

public final class WheelConfigSnapshot {
    private final JsonObject document;
    private final List<PaletteGroup> overrideGroups;
    private final List<PaletteGroup> typedGroups;

    WheelConfigSnapshot(
            JsonObject document,
            List<PaletteGroup> overrideGroups,
            List<PaletteGroup> typedGroups
    ) {
        this.document = document.deepCopy();
        this.overrideGroups = List.copyOf(overrideGroups);
        this.typedGroups = List.copyOf(typedGroups);
    }

    public JsonArray groups() {
        JsonArray groups = new JsonArray();
        typedGroups.forEach(group -> groups.add(group.toJson()));
        return groups;
    }

    public List<PaletteGroup> overrideGroups() {
        return overrideGroups;
    }

    public List<PaletteGroup> typedGroups() {
        return typedGroups;
    }

    JsonObject document() {
        return document.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof WheelConfigSnapshot that && document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return document.hashCode();
    }
}
