package com.davidblackcn.lorianarchorbit.palette;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

public record PaletteGroup(String id, String displayName, String iconItemId, List<PaletteMember> members) {
    public PaletteGroup {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("group id must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("group displayName must not be blank");
        }
        if (iconItemId == null || iconItemId.isBlank()) {
            throw new IllegalArgumentException("group icon must not be blank");
        }
        members = List.copyOf(members);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("display_name", displayName);
        json.addProperty("icon", iconItemId);
        JsonArray memberArray = new JsonArray();
        members.forEach(member -> memberArray.add(member.toJson()));
        json.add("members", memberArray);
        return json;
    }
}
