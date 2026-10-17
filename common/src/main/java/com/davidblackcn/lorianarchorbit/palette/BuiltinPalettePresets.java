package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.PalettePreset;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BuiltinPalettePresets {
    private static final String RESOURCE = "/assets/lorian_arch_orbit/palette_presets/default.json";
    private static final PresetData DATA = load();

    private BuiltinPalettePresets() {
    }

    public static List<PaletteGroup> groups(PalettePreset preset) {
        return switch (Objects.requireNonNull(preset, "preset")) {
            case ITEM_TAG_A -> DATA.primary();
            case ITEM_TAG_B -> DATA.secondary();
            case COLOR_CATEGORIES -> DATA.colorOnly();
        };
    }

    private static PresetData load() {
        try (InputStream input = BuiltinPalettePresets.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing built-in palette preset resource: " + RESOURCE);
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            List<PaletteGroup> primary = new ArrayList<>();
            List<PaletteGroup> secondary = new ArrayList<>();
            List<PaletteGroup> colorOnly = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("matrices")) {
                Matrix matrix = parseMatrix(element.getAsJsonObject());
                List<PaletteGroup> rows = matrix.rowsAsGroups();
                List<PaletteGroup> columns = matrix.columnsAsGroups();
                primary.addAll(rows);
                secondary.addAll(columns);
            }
            colorOnly.addAll(parseExplicit(root.getAsJsonArray("color_explicit")));
            primary.addAll(parseExplicit(root.getAsJsonArray("primary_explicit")));
            secondary.addAll(parseExplicit(root.getAsJsonArray("secondary_explicit")));
            return new PresetData(List.copyOf(primary), List.copyOf(secondary), List.copyOf(colorOnly));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load built-in palette presets", exception);
        }
    }

    private static Matrix parseMatrix(JsonObject json) {
        String id = string(json, "id");
        String primaryPrefix = string(json, "primary_prefix");
        String secondaryPrefix = string(json, "secondary_prefix");
        List<AxisEntry> columns = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("columns")) {
            JsonObject column = element.getAsJsonObject();
            columns.add(new AxisEntry(string(column, "id"), string(column, "name")));
        }
        List<MatrixRow> rows = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("rows")) {
            JsonObject row = element.getAsJsonObject();
            Map<String, String> cells = new LinkedHashMap<>();
            if (row.has("cells")) {
                row.getAsJsonObject("cells").entrySet().forEach(entry ->
                        cells.put(entry.getKey(), entry.getValue().getAsString()));
            }
            rows.add(new MatrixRow(
                    string(row, "id"),
                    string(row, "name"),
                    row.has("template") ? row.get("template").getAsString() : null,
                    Map.copyOf(cells),
                    row.has("primary_members")
                            ? row.getAsJsonArray("primary_members").asList().stream()
                            .map(JsonElement::getAsString).toList()
                            : List.of()
            ));
        }
        return new Matrix(id, primaryPrefix, secondaryPrefix, List.copyOf(columns), List.copyOf(rows));
    }

    private static List<PaletteGroup> parseExplicit(JsonArray groups) {
        List<PaletteGroup> result = new ArrayList<>();
        for (JsonElement element : groups) {
            JsonObject group = element.getAsJsonObject();
            List<PaletteMember> members = new ArrayList<>();
            for (JsonElement member : group.getAsJsonArray("members")) {
                members.add(new PaletteMember(member.getAsString()));
            }
            if (members.size() >= 2) {
                result.add(new PaletteGroup(
                        string(group, "id"), string(group, "name"), members.getFirst().itemId(), members
                ));
            }
        }
        return List.copyOf(result);
    }

    private static String string(JsonObject json, String key) {
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }
        return value;
    }

    private record PresetData(
            List<PaletteGroup> primary,
            List<PaletteGroup> secondary,
            List<PaletteGroup> colorOnly
    ) {
    }

    private record AxisEntry(String id, String name) {
    }

    private record MatrixRow(
            String id,
            String name,
            String template,
            Map<String, String> cells,
            List<String> primaryMembers
    ) {
        private String item(String column) {
            if (cells.containsKey(column)) {
                String explicit = cells.get(column);
                return explicit.isBlank() ? null : explicit;
            }
            return template == null ? null : template.replace("{column}", column);
        }
    }

    private record Matrix(
            String id,
            String primaryPrefix,
            String secondaryPrefix,
            List<AxisEntry> columns,
            List<MatrixRow> rows
    ) {
        private List<PaletteGroup> rowsAsGroups() {
            List<PaletteGroup> result = new ArrayList<>();
            for (MatrixRow row : rows) {
                List<PaletteMember> members = new ArrayList<>();
                row.primaryMembers().stream().map(PaletteMember::new).forEach(members::add);
                columns.stream().map(column -> row.item(column.id())).filter(Objects::nonNull)
                        .map(PaletteMember::new).forEach(members::add);
                if (members.size() >= 2) {
                    result.add(new PaletteGroup(
                            primaryPrefix + row.id(), row.name(), members.getFirst().itemId(), members
                    ));
                }
            }
            return List.copyOf(result);
        }

        private List<PaletteGroup> columnsAsGroups() {
            List<PaletteGroup> result = new ArrayList<>();
            for (AxisEntry column : columns) {
                List<PaletteMember> members = rows.stream()
                        .map(row -> row.item(column.id()))
                        .filter(Objects::nonNull)
                        .map(PaletteMember::new)
                        .toList();
                if (members.size() >= 2) {
                    result.add(new PaletteGroup(
                            secondaryPrefix + column.id(), column.name(), members.getFirst().itemId(), members
                    ));
                }
            }
            return List.copyOf(result);
        }
    }
}
