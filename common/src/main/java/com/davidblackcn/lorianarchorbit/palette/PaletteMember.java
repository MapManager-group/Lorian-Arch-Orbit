package com.davidblackcn.lorianarchorbit.palette;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Objects;

public record PaletteMember(String itemId, PaletteMatchMode matchMode, JsonElement components) {
    public PaletteMember {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        matchMode = Objects.requireNonNull(matchMode, "matchMode");
        components = components == null ? null : components.deepCopy();
        if (matchMode == PaletteMatchMode.EXACT_COMPONENTS && components == null) {
            throw new IllegalArgumentException("exact component members require components");
        }
    }

    public PaletteMember(String itemId) {
        this(itemId, PaletteMatchMode.ITEM, null);
    }

    @Override
    public JsonElement components() {
        return components == null ? null : components.deepCopy();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("item", itemId);
        json.addProperty("match", matchMode.serializedName());
        if (components != null) {
            json.add("components", components.deepCopy());
        }
        return json;
    }
}
