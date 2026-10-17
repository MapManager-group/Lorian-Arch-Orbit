package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class PaletteShareCodec {
    public static final String FORMAT = "lorian_arch_orbit_palette";
    public static final int VERSION = 1;
    public static final String CODE_PREFIX = "LAO-PALETTE-1:";
    public static final int MAX_JSON_BYTES = 4 * 1024 * 1024;
    public static final int MAX_CODE_LENGTH = 1_500_000;
    public static final int MAX_GROUPS = 256;
    public static final int MAX_MEMBERS = 16_384;
    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_ID_LENGTH = 160;
    private static final int MAX_ITEM_ID_LENGTH = 256;
    private static final int MAX_COMPONENT_BYTES = 64 * 1024;
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    public String encodeJson(PaletteShareBundle bundle) throws PaletteShareException {
        validate(bundle);
        String json = PRETTY_GSON.toJson(toJson(bundle));
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            throw new PaletteShareException("share JSON exceeds the size limit");
        }
        return json;
    }

    public String encodeCode(PaletteShareBundle bundle) throws PaletteShareException {
        byte[] source = encodeJson(bundle).getBytes(StandardCharsets.UTF_8);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
                gzip.write(source);
            }
            String code = CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray());
            if (code.length() > MAX_CODE_LENGTH) {
                throw new PaletteShareException("share code exceeds the size limit");
            }
            return code;
        } catch (IOException exception) {
            throw new PaletteShareException("could not encode share code", exception);
        }
    }

    public PaletteShareBundle decode(String input) throws PaletteShareException {
        if (input == null || input.isBlank()) {
            throw new PaletteShareException("share input is empty");
        }
        String stripped = input.strip();
        String json = stripped.startsWith(CODE_PREFIX) ? decodeCode(stripped) : stripped;
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES) {
            throw new PaletteShareException("share JSON exceeds the size limit");
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                throw new PaletteShareException("share root must be an object");
            }
            PaletteShareBundle bundle = fromJson(parsed.getAsJsonObject());
            validate(bundle);
            return bundle;
        } catch (JsonParseException | IllegalArgumentException exception) {
            throw new PaletteShareException("share JSON is invalid", exception);
        }
    }

    private String decodeCode(String code) throws PaletteShareException {
        if (code.length() > MAX_CODE_LENGTH) {
            throw new PaletteShareException("share code exceeds the size limit");
        }
        byte[] compressed;
        try {
            compressed = Base64.getUrlDecoder().decode(code.substring(CODE_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new PaletteShareException("share code uses invalid Base64", exception);
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = gzip.read(buffer)) >= 0) {
                if (output.size() + read > MAX_JSON_BYTES) {
                    throw new PaletteShareException("expanded share code exceeds the size limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new PaletteShareException("share code is not valid compressed data", exception);
        }
    }

    private static JsonObject toJson(PaletteShareBundle bundle) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", VERSION);
        root.addProperty("name", bundle.name());
        JsonArray groups = new JsonArray();
        for (PaletteShareEntry entry : bundle.entries()) {
            JsonObject group = entry.group().toJson();
            group.addProperty("layer", entry.layer().serializedName());
            groups.add(group);
        }
        root.add("groups", groups);
        return root;
    }

    private static PaletteShareBundle fromJson(JsonObject root) throws PaletteShareException {
        if (!FORMAT.equals(requiredString(root, "format", "share"))) {
            throw new PaletteShareException("unsupported share format");
        }
        if (!root.has("version") || !root.get("version").isJsonPrimitive()
                || root.get("version").getAsInt() != VERSION) {
            throw new PaletteShareException("unsupported share version");
        }
        String name = requiredString(root, "name", "share");
        JsonElement groupsElement = root.get("groups");
        if (groupsElement == null || !groupsElement.isJsonArray()) {
            throw new PaletteShareException("share.groups must be an array");
        }
        JsonArray groups = groupsElement.getAsJsonArray();
        if (groups.isEmpty() || groups.size() > MAX_GROUPS) {
            throw new PaletteShareException("share group count is outside the supported range");
        }
        List<PaletteShareEntry> entries = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            JsonElement element = groups.get(index);
            if (!element.isJsonObject()) {
                throw new PaletteShareException("share.groups[" + index + "] must be an object");
            }
            JsonObject group = element.getAsJsonObject();
            String path = "share.groups[" + index + "]";
            PaletteShareLayer layer;
            try {
                layer = PaletteShareLayer.parse(requiredString(group, "layer", path));
            } catch (RuntimeException exception) {
                throw new PaletteShareException(path + ".layer is invalid", exception);
            }
            String id = requiredString(group, "id", path);
            String displayName = requiredString(group, "display_name", path);
            String icon = requiredString(group, "icon", path);
            JsonElement membersElement = group.get("members");
            if (membersElement == null || !membersElement.isJsonArray()) {
                throw new PaletteShareException(path + ".members must be an array");
            }
            List<PaletteMember> members = new ArrayList<>();
            JsonArray memberArray = membersElement.getAsJsonArray();
            for (int memberIndex = 0; memberIndex < memberArray.size(); memberIndex++) {
                JsonElement memberElement = memberArray.get(memberIndex);
                String memberPath = path + ".members[" + memberIndex + "]";
                if (!memberElement.isJsonObject()) {
                    throw new PaletteShareException(memberPath + " must be an object");
                }
                JsonObject member = memberElement.getAsJsonObject();
                String item = requiredString(member, "item", memberPath);
                PaletteMatchMode match;
                try {
                    match = PaletteMatchMode.parse(member.has("match") ? member.get("match").getAsString() : null);
                } catch (RuntimeException exception) {
                    throw new PaletteShareException(memberPath + ".match is invalid", exception);
                }
                JsonElement components = member.get("components");
                try {
                    members.add(new PaletteMember(item, match, components));
                } catch (IllegalArgumentException exception) {
                    throw new PaletteShareException(memberPath + " is invalid", exception);
                }
            }
            entries.add(new PaletteShareEntry(layer, new PaletteGroup(id, displayName, icon, members)));
        }
        return new PaletteShareBundle(name, entries);
    }

    private static void validate(PaletteShareBundle bundle) throws PaletteShareException {
        if (bundle.name().length() > MAX_NAME_LENGTH) {
            throw new PaletteShareException("share name is too long");
        }
        if (bundle.entries().isEmpty() || bundle.entries().size() > MAX_GROUPS) {
            throw new PaletteShareException("share group count is outside the supported range");
        }
        int members = 0;
        Set<String> layerIds = new HashSet<>();
        for (PaletteShareEntry entry : bundle.entries()) {
            PaletteGroup group = entry.group();
            validateLength(group.id(), MAX_ID_LENGTH, "group id");
            validateLength(group.displayName(), MAX_NAME_LENGTH, "group name");
            validateLength(group.iconItemId(), MAX_ITEM_ID_LENGTH, "group icon");
            validateIdentifier(group.iconItemId(), "group icon");
            String layerId = entry.layer().serializedName() + ':' + group.id();
            if (!layerIds.add(layerId)) {
                throw new PaletteShareException("duplicate group id in layer: " + layerId);
            }
            members += group.members().size();
            if (members > MAX_MEMBERS) {
                throw new PaletteShareException("share contains too many members");
            }
            for (PaletteMember member : group.members()) {
                validateLength(member.itemId(), MAX_ITEM_ID_LENGTH, "member item id");
                validateIdentifier(member.itemId(), "member item id");
                JsonElement components = member.components();
                if (components != null
                        && components.toString().getBytes(StandardCharsets.UTF_8).length > MAX_COMPONENT_BYTES) {
                    throw new PaletteShareException("member components exceed the size limit");
                }
            }
        }
    }

    private static void validateLength(String value, int maximum, String field) throws PaletteShareException {
        if (value.length() > maximum) {
            throw new PaletteShareException(field + " is too long");
        }
    }

    private static void validateIdentifier(String value, String field) throws PaletteShareException {
        try {
            Identifier.parse(value);
        } catch (RuntimeException exception) {
            throw new PaletteShareException(field + " is not a valid resource identifier", exception);
        }
    }

    private static String requiredString(JsonObject object, String key, String path) throws PaletteShareException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                || value.getAsString().isBlank()) {
            throw new PaletteShareException(path + '.' + key + " must be a non-blank string");
        }
        return value.getAsString();
    }
}
