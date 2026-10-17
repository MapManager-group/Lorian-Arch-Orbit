package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.*;

/** Resolves exported model faces against actual registered defaults, not texture-name hints. */
public final class HueBlockFaces {
    public static final List<String> DIRECTIONS = List.of("top", "bottom", "north", "south", "east", "west");
    private static final Set<String> ROTATION = Set.of("facing", "horizontal_facing", "axis");
    public record Surface(String texture, Set<String> defaultFaces, Map<String, String> placements) {
        public Surface { defaultFaces = Set.copyOf(defaultFaces); placements = Map.copyOf(placements); }
        public Set<String> faces() { return placements.keySet(); }
    }
    private HueBlockFaces() { }

    public static List<Surface> resolve(JsonElement entries, Map<String, String> defaults,
                                        Map<String, List<String>> rotations) {
        List<Map<String, String>> states = new ArrayList<>();
        states.add(new TreeMap<>(defaults));
        for (var entry : rotations.entrySet()) {
            if (!ROTATION.contains(entry.getKey())) continue;
            var previous = List.copyOf(states);
            for (var state : previous) for (String value : entry.getValue()) {
                var next = new TreeMap<>(state); next.put(entry.getKey(), value);
                if (!states.contains(next)) states.add(next);
            }
        }
        Map<String, Set<String>> original = new TreeMap<>();
        Map<String, Map<String, String>> reachable = new TreeMap<>();
        for (var state : states) for (JsonElement element : entries.getAsJsonArray()) {
            var record = element.getAsJsonObject();
            if (!matches(record.getAsJsonObject("when"), state)) continue;
            String placement = state.entrySet().stream().filter(e -> ROTATION.contains(e.getKey()))
                    .map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining(", "));
            for (var texture : record.getAsJsonObject("textures").entrySet()) for (var face : texture.getValue().getAsJsonArray()) {
                String direction = face.getAsString();
                if (!DIRECTIONS.contains(direction)) throw new IllegalArgumentException("Invalid model face");
                reachable.computeIfAbsent(texture.getKey(), k -> new TreeMap<>()).putIfAbsent(direction, placement);
                if (state.equals(defaults)) original.computeIfAbsent(texture.getKey(), k -> new HashSet<>()).add(direction);
            }
        }
        return reachable.entrySet().stream().map(e -> new Surface(e.getKey(), original.getOrDefault(e.getKey(), Set.of()), e.getValue())).toList();
    }

    static boolean matches(JsonObject condition, Map<String, String> state) {
        for (var entry : condition.entrySet()) {
            if (entry.getKey().equals("OR") || entry.getKey().equals("AND")) {
                boolean all = entry.getKey().equals("AND"), value = all;
                for (var term : entry.getValue().getAsJsonArray()) {
                    if (all) value &= matches(term.getAsJsonObject(), state);
                    else value |= matches(term.getAsJsonObject(), state);
                }
                if (!value) return false;
            } else if (!Arrays.asList(entry.getValue().getAsString().split("\\|")).contains(state.get(entry.getKey()))) return false;
        }
        return true;
    }

    public static boolean matchesFace(Set<String> faces, String requested) {
        return requested.equals("all") || (requested.equals("sides")
                ? faces.stream().anyMatch(f -> !f.equals("top") && !f.equals("bottom")) : faces.contains(requested));
    }
}
