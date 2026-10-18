package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Standard JSON models only. Unknown loaders and tinted/sloped surfaces are diagnosed. */
public final class LocalModelFaces {
    @FunctionalInterface public interface Resources { JsonObject read(String id) throws IOException; }
    private record Model(Map<String, String> textures, JsonArray elements) { }
    private final Resources resources;
    private final Map<String, Model> models = new HashMap<>();
    private final Map<String, Integer> skipped = new TreeMap<>();
    public LocalModelFaces(Resources resources) { this.resources = resources; }
    public Map<String, Integer> skipped() { return Map.copyOf(skipped); }
    private void skip(String reason) { skipped.merge(reason, 1, Integer::sum); }

    public JsonArray extract(JsonObject blockstate) throws IOException {
        JsonArray result = new JsonArray();
        if (blockstate.has("loader")) throw new IOException("Custom blockstate loader");
        if (blockstate.has("variants")) for (var entry : blockstate.getAsJsonObject("variants").entrySet()) {
            JsonObject when = new JsonObject();
            if (!entry.getKey().isEmpty()) for (String term : entry.getKey().split(",")) {
                String[] pair = term.split("=", -1);
                if (pair.length != 2) throw new IOException("Invalid state selector");
                when.addProperty(pair[0], pair[1]);
            }
            append(result, when, entry.getValue());
        }
        if (blockstate.has("multipart")) for (var element : blockstate.getAsJsonArray("multipart")) {
            var part = element.getAsJsonObject();
            append(result, part.has("when") ? part.getAsJsonObject("when") : new JsonObject(), part.get("apply"));
        }
        return result;
    }

    private void append(JsonArray result, JsonObject when, JsonElement application) throws IOException {
        if (application.isJsonArray()) {
            for (var entry : application.getAsJsonArray()) append(result, when, entry);
            return;
        }
        JsonObject variant = application.getAsJsonObject();
        int x = variant.has("x") ? variant.get("x").getAsInt() : 0;
        int y = variant.has("y") ? variant.get("y").getAsInt() : 0;
        if (x % 90 != 0 || y % 90 != 0) throw new IOException("Unsupported model rotation");
        Model model = model(HueResourceId.resource(variant.get("model").getAsString()), new HashSet<>());
        Map<String, Set<String>> faces = new TreeMap<>();
        Set<String> tintedDirections = new HashSet<>();
        for (var value : model.elements()) {
            JsonObject element = value.getAsJsonObject();
            if (element.has("rotation") && element.getAsJsonObject("rotation").get("angle").getAsDouble() != 0) {
                skip("sloped"); continue;
            }
            if (!element.has("faces")) continue;
            for (var face : element.getAsJsonObject("faces").entrySet()) {
                JsonObject info = face.getValue().getAsJsonObject();
                String direction = rotate(face.getKey(), x, y);
                if (info.has("tintindex") && info.get("tintindex").getAsInt() >= 0) {
                    skip("tinted"); tintedDirections.add(direction); continue;
                }
                String texture = info.get("texture").getAsString();
                Set<String> seen = new HashSet<>();
                while (texture.startsWith("#")) {
                    if (!seen.add(texture) || !model.textures().containsKey(texture.substring(1)))
                        throw new IOException("Unresolved texture reference");
                    texture = model.textures().get(texture.substring(1));
                }
                faces.computeIfAbsent(HueResourceId.resource(texture), key -> new TreeSet<>()).add(direction);
            }
        }
        // A tinted overlay changes the underlying face too (e.g. grass sides).
        JsonObject textures = new JsonObject();
        faces.forEach((texture, directions) -> {
            directions.removeAll(tintedDirections);
            if (!directions.isEmpty()) { JsonArray array = new JsonArray(); directions.forEach(array::add); textures.add(texture, array); }
        });
        if (!textures.isEmpty()) {
            JsonObject record = new JsonObject(); record.add("when", when); record.add("textures", textures); result.add(record);
        }
    }

    private Model model(String id, Set<String> ancestors) throws IOException {
        if (models.containsKey(id)) return models.get(id);
        if (ancestors.size() >= 64 || !ancestors.add(id)) throw new IOException("Cyclic or excessively deep model inheritance");
        String[] parts = id.split(":", 2);
        JsonObject json = resources.read(parts[0] + ":models/" + parts[1] + ".json");
        if (json.has("loader")) throw new IOException("Custom model loader");
        Map<String, String> textures = new TreeMap<>(); JsonArray elements = new JsonArray();
        if (json.has("parent")) {
            Model parent = model(HueResourceId.resource(json.get("parent").getAsString()), ancestors);
            textures.putAll(parent.textures()); elements = parent.elements();
        }
        if (json.has("textures")) for (var entry : json.getAsJsonObject("textures").entrySet()) {
            var value = entry.getValue();
            if (value.isJsonObject()) {
                var object = value.getAsJsonObject();
                if (object.has("atlas") && !object.get("atlas").getAsString().equals("minecraft:blocks"))
                    throw new IOException("Non-block texture atlas");
                value = object.get("sprite");
            }
            textures.put(entry.getKey(), value.getAsString());
        }
        if (json.has("elements")) elements = json.getAsJsonArray("elements");
        ancestors.remove(id);
        Model result = new Model(Map.copyOf(textures), elements); models.put(id, result); return result;
    }

    static String rotate(String face, int x, int y) {
        int[] vector = switch (face) {
            case "down" -> new int[]{0,-1,0}; case "up" -> new int[]{0,1,0};
            case "north" -> new int[]{0,0,-1}; case "south" -> new int[]{0,0,1};
            case "west" -> new int[]{-1,0,0}; case "east" -> new int[]{1,0,0};
            default -> throw new IllegalArgumentException("Unknown face");
        };
        for (int i = 0; i < Math.floorMod(x, 360) / 90; i++) { int b = vector[1]; vector[1] = vector[2]; vector[2] = -b; }
        for (int i = 0; i < Math.floorMod(y, 360) / 90; i++) { int a = vector[0]; vector[0] = -vector[2]; vector[2] = a; }
        return vector[1] < 0 ? "bottom" : vector[1] > 0 ? "top" : vector[0] < 0 ? "west" : vector[0] > 0 ? "east" : vector[2] < 0 ? "north" : "south";
    }
}
