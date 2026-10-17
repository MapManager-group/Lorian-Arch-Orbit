package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable, validated upstream data. The first blockdata entry is metadata, not a texture. */
public record HueBlocksData(String version, String generatedAt, List<Block> blocks, List<Palette> palettes) {
    private static final Set<String> FACES = Set.of("top", "bottom", "north", "south", "east", "west");

    public HueBlocksData {
        blocks = List.copyOf(blocks);
        palettes = List.copyOf(palettes);
    }

    public static HueBlocksData decode(String version, JsonArray blockdata, JsonArray paletteData) {
        if (blockdata.size() < 2 || blockdata.size() > 10001 || paletteData.size() > 100) {
            throw new IllegalArgumentException("Invalid HueBlocks dataset size");
        }
        String generatedAt = blockdata.get(0).getAsString();
        List<Block> blocks = new ArrayList<>();
        Set<String> textures = new HashSet<>();
        for (int i = 1; i < blockdata.size(); i++) {
            JsonObject object = blockdata.get(i).getAsJsonObject();
            String texture = texture(object.get("texture").getAsString());
            if (!textures.add(texture)) throw new IllegalArgumentException("Duplicate texture: " + texture);
            JsonArray rgb = object.getAsJsonArray("rgb");
            JsonArray lab = object.getAsJsonArray("lab");
            if (rgb.size() != 3 || lab.size() != 3) throw new IllegalArgumentException("Invalid color components");
            int color = 0;
            for (JsonElement channel : rgb) {
                double value = channel.getAsDouble();
                if (!Double.isFinite(value) || value < 0 || value > 255 || value != Math.rint(value)) {
                    throw new IllegalArgumentException("Invalid RGB channel");
                }
                color = (color << 8) | (int) value;
            }
            // Published lab channels are decimal strings; Gson accepts both strings and numbers.
            HueColor oklab = new HueColor(lab.get(0).getAsDouble(), lab.get(1).getAsDouble(), lab.get(2).getAsDouble());
            if (oklab.l() < 0 || oklab.l() > 1 || Math.abs(oklab.a()) > 1 || Math.abs(oklab.b()) > 1) {
                throw new IllegalArgumentException("Invalid OkLAB range");
            }
            Set<String> sides = new HashSet<>();
            for (JsonElement face : object.getAsJsonArray("sides")) {
                String value = face.getAsString();
                if (!FACES.contains(value)) throw new IllegalArgumentException("Unknown facing");
                sides.add(value);
            }
            if (sides.isEmpty()) throw new IllegalArgumentException("Empty facing");
            blocks.add(new Block(texture, color, oklab, Set.copyOf(sides)));
        }
        List<Palette> palettes = new ArrayList<>();
        for (JsonElement element : paletteData) {
            if (!element.isJsonObject()) continue; // Upstream can include separator entries.
            JsonObject object = element.getAsJsonObject();
            if (!object.has("textures")) continue;
            String name = object.get("name").getAsString();
            if (name.isBlank() || name.length() > 160) throw new IllegalArgumentException("Invalid palette name");
            Set<String> members = new HashSet<>();
            JsonArray array = object.getAsJsonArray("textures");
            if (array.size() > 10000) throw new IllegalArgumentException("Palette too large");
            for (JsonElement entry : array) {
                String value = texture(entry.getAsString());
                if (textures.contains(value)) members.add(value);
            }
            if (!members.isEmpty()) palettes.add(new Palette(name, Set.copyOf(members)));
        }
        return new HueBlocksData(version, generatedAt, blocks, palettes);
    }

    private static String texture(String name) {
        if (!name.matches("[a-z0-9_]+\\.png")) throw new IllegalArgumentException("Invalid texture name");
        return name;
    }

    public record Block(String texture, int rgb, HueColor lab, Set<String> sides) {
        public Block { sides = Set.copyOf(sides); }
        public boolean faces(String face) {
            return face.equals("all") || (face.equals("sides")
                    ? sides.stream().anyMatch(s -> !s.equals("top") && !s.equals("bottom")) : sides.contains(face));
        }
    }
    public record Palette(String name, Set<String> textures) {
        public Palette { textures = Set.copyOf(textures); }
    }
}
