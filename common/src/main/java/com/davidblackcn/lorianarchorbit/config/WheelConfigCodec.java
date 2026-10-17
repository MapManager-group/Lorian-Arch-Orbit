package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.function.Supplier;

public final class WheelConfigCodec implements ConfigCodec<WheelConfigSnapshot> {
    private final Supplier<List<PaletteGroup>> defaultGroups;

    public WheelConfigCodec() {
        this(List::of);
    }

    public WheelConfigCodec(Supplier<List<PaletteGroup>> defaultGroups) {
        this.defaultGroups = Objects.requireNonNull(defaultGroups, "defaultGroups");
    }

    @Override
    public WheelConfigSnapshot defaults() {
        JsonObject root = new JsonObject();
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        root.add("groups", new JsonArray());
        List<PaletteGroup> builtins = List.copyOf(defaultGroups.get());
        return new WheelConfigSnapshot(root, List.of(), builtins);
    }

    @Override
    public DecodedConfig<WheelConfigSnapshot> decode(JsonObject input) {
        JsonObject root = input.deepCopy();
        int version = JsonConfigSupport.version(root);
        JsonConfigSupport.requireSupportedVersion(version);
        boolean migrated = version < ConfigConstants.CURRENT_VERSION;
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        List<String> warnings = new ArrayList<>();
        JsonElement groups = root.get("groups");
        if (groups == null) {
            root.add("groups", new JsonArray());
        } else if (!groups.isJsonArray()) {
            throw new ConfigException("groups must be an array");
        }
        List<PaletteGroup> overrides = decodeGroups(root.getAsJsonArray("groups"), warnings);
        List<PaletteGroup> effective = mergeOverrides(overrides, List.copyOf(defaultGroups.get()));
        return new DecodedConfig<>(new WheelConfigSnapshot(root, overrides, effective), migrated, warnings);
    }

    @Override
    public JsonObject encode(WheelConfigSnapshot snapshot) {
        return snapshot.document();
    }

    public WheelConfigSnapshot fromGroups(List<PaletteGroup> groups) {
        return fromGroups(defaults(), groups);
    }

    public WheelConfigSnapshot fromGroups(WheelConfigSnapshot base, List<PaletteGroup> groups) {
        List<PaletteGroup> builtins = List.copyOf(defaultGroups.get());
        JsonObject root = base.document();
        List<PaletteGroup> overrides = collectOverrides(groups, builtins, root.getAsJsonArray("groups"));
        JsonArray array = new JsonArray();
        for (PaletteGroup group : overrides) {
            JsonObject encoded = originalGroup(root.getAsJsonArray("groups"), group.id());
            JsonObject known = group.toJson();
            known.entrySet().forEach(entry -> encoded.add(entry.getKey(), entry.getValue().deepCopy()));
            array.add(encoded);
        }
        root.add("groups", array);
        return new WheelConfigSnapshot(root, overrides, mergeOverrides(overrides, builtins));
    }

    static List<PaletteGroup> mergeOverrides(List<PaletteGroup> overrides, List<PaletteGroup> builtins) {
        Map<String, PaletteGroup> builtinById = new LinkedHashMap<>();
        builtins.forEach(group -> builtinById.put(group.id(), group));
        Map<String, PaletteGroup> overrideById = new LinkedHashMap<>();
        overrides.forEach(group -> overrideById.put(group.id(), group));
        List<PaletteGroup> effective = new ArrayList<>();
        for (PaletteGroup override : overrides) {
            if (!builtinById.containsKey(override.id())) {
                effective.add(override);
            }
        }
        for (PaletteGroup builtin : builtins) {
            effective.add(overrideById.getOrDefault(builtin.id(), builtin));
        }
        return List.copyOf(effective);
    }

    private static List<PaletteGroup> collectOverrides(
            List<PaletteGroup> groups,
            List<PaletteGroup> builtins,
            JsonArray originalGroups
    ) {
        Map<String, PaletteGroup> builtinById = new LinkedHashMap<>();
        builtins.forEach(group -> builtinById.put(group.id(), group));
        List<PaletteGroup> overrides = new ArrayList<>();
        for (PaletteGroup group : groups) {
            PaletteGroup builtin = builtinById.get(group.id());
            JsonObject original = originalGroup(originalGroups, group.id());
            if (builtin == null || !builtin.equals(group) || hasUnknownGroupFields(original)) {
                overrides.add(group);
            }
        }
        return List.copyOf(overrides);
    }

    private static boolean hasUnknownGroupFields(JsonObject group) {
        return group.keySet().stream().anyMatch(key -> !Set.of("id", "display_name", "icon", "members").contains(key));
    }

    private static JsonObject originalGroup(JsonArray groups, String id) {
        for (JsonElement element : groups) {
            if (element.isJsonObject()) {
                JsonObject group = element.getAsJsonObject();
                if (group.has("id") && group.get("id").isJsonPrimitive()
                        && id.equals(group.get("id").getAsString())) {
                    return group.deepCopy();
                }
            }
        }
        return new JsonObject();
    }

    private static List<PaletteGroup> decodeGroups(JsonArray groups, List<String> warnings) {
        List<PaletteGroup> decoded = new ArrayList<>();
        Set<String> ids = new java.util.HashSet<>();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            JsonElement element = groups.get(groupIndex);
            if (!element.isJsonObject()) {
                throw new ConfigException("groups[" + groupIndex + "] must be an object");
            }
            JsonObject group = element.getAsJsonObject();
            String id = requiredString(group, "id", "groups[" + groupIndex + "]");
            if (!ids.add(id)) {
                throw new ConfigException("duplicate palette group id: " + id);
            }
            String name = requiredString(group, "display_name", "groups[" + groupIndex + "]");
            String icon = requiredString(group, "icon", "groups[" + groupIndex + "]");
            JsonElement membersElement = group.get("members");
            if (membersElement == null || !membersElement.isJsonArray()) {
                throw new ConfigException("groups[" + groupIndex + "].members must be an array");
            }
            List<PaletteMember> members = new ArrayList<>();
            JsonArray memberArray = membersElement.getAsJsonArray();
            for (int memberIndex = 0; memberIndex < memberArray.size(); memberIndex++) {
                JsonElement memberElement = memberArray.get(memberIndex);
                if (!memberElement.isJsonObject()) {
                    throw new ConfigException("groups[" + groupIndex + "].members[" + memberIndex + "] must be an object");
                }
                JsonObject member = memberElement.getAsJsonObject();
                String path = "groups[" + groupIndex + "].members[" + memberIndex + "]";
                String item = requiredString(member, "item", path);
                PaletteMatchMode mode;
                try {
                    mode = PaletteMatchMode.parse(member.has("match") ? member.get("match").getAsString() : null);
                } catch (RuntimeException exception) {
                    throw new ConfigException(path + ".match is invalid", exception);
                }
                JsonElement components = member.get("components");
                try {
                    members.add(new PaletteMember(item, mode, components));
                } catch (IllegalArgumentException exception) {
                    throw new ConfigException(path + ": " + exception.getMessage(), exception);
                }
            }
            decoded.add(new PaletteGroup(id, name, icon, members));
        }
        return List.copyOf(decoded);
    }

    private static String requiredString(JsonObject object, String key, String path) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new ConfigException(path + "." + key + " must be a non-blank string");
        }
        return value.getAsString();
    }

    @Override
    public Set<String> changedNamespaces(WheelConfigSnapshot previous, WheelConfigSnapshot next) {
        return previous.equals(next) ? Set.of() : Set.of("palette_wheel");
    }
}
