package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef;
import com.google.gson.*;
import java.util.*;

/** Explicit user exports only. Never writes editor sessions or changes wheel/config formats. */
public final class GradientRecipe {
    public static final int MAX_LENGTH = 262144;
    public record Node(String hex, int steps, String candidate) { }
    public record Recipe(boolean paletteOnly, List<Node> nodes, boolean oklab, String face,
                         String palette, GroupRef group, Set<String> excluded) {
        public Recipe { nodes = List.copyOf(nodes); excluded = Set.copyOf(excluded); }
        public List<String> missing(List<HueGradient.Candidate> available) {
            var keys = available.stream().map(HueGradient.Candidate::key).collect(java.util.stream.Collectors.toSet());
            return nodes.stream().map(Node::candidate).filter(Objects::nonNull).filter(k -> !keys.contains(k)).distinct().toList();
        }
        /** Caller must approve replacement and missing references before this atomic mutation. */
        public void apply(GradientWorkbench model, List<HueGradient.Candidate> available) {
            var candidates = new HashMap<String, HueGradient.Candidate>();
            available.forEach(c -> candidates.put(c.key(), c));
            var replacement = new ArrayList<GradientWorkbench.Node>();
            for (var node : nodes) {
                var value = new GradientWorkbench.Node(node.hex(), Integer.toString(node.steps()));
                value.pinned = candidates.get(node.candidate()); replacement.add(value);
            }
            Runnable change = () -> {
                model.face = face; model.palette = palette; model.sourceGroup = group;
                model.excluded.clear(); model.excluded.addAll(excluded);
                if (!paletteOnly) {
                    model.nodes.clear(); model.nodes.addAll(replacement); model.selectedNode = 0; model.oklab = oklab;
                }
            };
            if (paletteOnly) { change.run(); model.invalidate(); }
            else model.structural(change);
        }
    }
    private GradientRecipe() { }
    public static String encode(GradientWorkbench model, boolean paletteOnly) {
        if (!paletteOnly && (!model.valid() || model.pendingSteps())) throw new IllegalArgumentException("Invalid gradient parameters");
        JsonObject root = new JsonObject();
        root.addProperty("format", paletteOnly ? "lorian-gradient-palette" : "lorian-gradient-recipe");
        root.addProperty("version", 1); root.addProperty("oklab", model.oklab);
        root.addProperty("face", model.face); root.addProperty("palette", model.palette);
        if (model.sourceGroup != null) {
            JsonObject group = new JsonObject(); group.addProperty("primary", model.sourceGroup.primary()); group.addProperty("id", model.sourceGroup.id()); root.add("group", group);
        }
        JsonArray nodes = new JsonArray();
        if (!paletteOnly) for (var n : model.nodes) {
            JsonObject node = new JsonObject(); node.addProperty("hex", n.hex.replace("#", "")); node.addProperty("steps", Integer.parseInt(n.steps));
            if (n.pinned != null) node.addProperty("candidate", n.pinned.key()); nodes.add(node);
        }
        root.add("nodes", nodes);
        JsonArray excluded = new JsonArray(); model.excluded.stream().sorted().forEach(excluded::add); root.add("excluded", excluded);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
    public static Recipe decode(String text) {
        if (text == null || text.length() > MAX_LENGTH) throw new IllegalArgumentException("Gradient file too large");
        try {
            JsonObject root = JsonParser.parseString(text).getAsJsonObject();
            String format = string(root, "format", 40);
            boolean paletteOnly = format.equals("lorian-gradient-palette");
            if (!paletteOnly && !format.equals("lorian-gradient-recipe")) throw new IllegalArgumentException("Not a gradient file");
            if (!root.get("version").getAsString().equals("1")) throw new IllegalArgumentException("Unsupported gradient format version");
            String face = string(root, "face", 16), palette = string(root, "palette", 160);
            if (!face.equals("all") && !face.equals("sides") && !HueBlockFaces.DIRECTIONS.contains(face)) throw new IllegalArgumentException("Invalid face");
            GroupRef group = null;
            if (root.has("group")) {
                var ref = root.getAsJsonObject("group"); group = new GroupRef(bool(ref, "primary"), string(ref, "id", 160));
            }
            List<Node> nodes = new ArrayList<>();
            var array = root.getAsJsonArray("nodes");
            if (array.size() > 16 || (paletteOnly ? array.size() != 0 : array.size() < 2)) throw new IllegalArgumentException("Invalid node count");
            for (var entry : array) {
                var object = entry.getAsJsonObject(); String hex = string(object, "hex", 6);
                if (!hex.matches("[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Invalid color");
                String stepsText = object.get("steps").getAsString();
                if (!stepsText.matches("[0-9]{1,3}")) throw new IllegalArgumentException("Invalid count");
                int steps = Integer.parseInt(stepsText);
                if (steps < 2 || steps > 128) throw new IllegalArgumentException("Invalid count");
                nodes.add(new Node(hex, steps, object.has("candidate") ? key(object.get("candidate").getAsString()) : null));
            }
            var excluded = new HashSet<String>(); var list = root.getAsJsonArray("excluded");
            if (list.size() > 10000) throw new IllegalArgumentException("Too many exclusions");
            for (var entry : list) excluded.add(key(entry.getAsString()));
            return new Recipe(paletteOnly, nodes, bool(root, "oklab"), face, palette, group, excluded);
        } catch (JsonParseException | IllegalStateException | NullPointerException | ClassCastException | UnsupportedOperationException ex) {
            throw new IllegalArgumentException("Invalid gradient JSON", ex);
        }
    }
    private static boolean bool(JsonObject object, String name) {
        if (!object.getAsJsonPrimitive(name).isBoolean()) throw new IllegalArgumentException("Invalid boolean");
        return object.get(name).getAsBoolean();
    }
    private static String string(JsonObject object, String name, int max) {
        String value = object.get(name).getAsString();
        if (value.isBlank() || value.length() > max) throw new IllegalArgumentException("Invalid " + name);
        return value;
    }
    private static String key(String value) {
        if (value.length() > 256 || !value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+\\|[a-z0-9_]+\\.png")) throw new IllegalArgumentException("Invalid candidate reference");
        return value;
    }
}
