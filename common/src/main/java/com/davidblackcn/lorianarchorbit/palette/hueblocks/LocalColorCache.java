package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Content-addressed colors only. Model mappings are rebuilt from the active resources. */
public final class LocalColorCache {
    private final Map<String, LocalTextureColor.Color> previous = new HashMap<>(), used = new TreeMap<>();
    public LocalColorCache(Path path) {
        if (!Files.isRegularFile(path)) return;
        try {
            if (Files.size(path) > 8 * 1024 * 1024) return;
            var root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.get("algorithm").getAsInt() != LocalTextureColor.ALGORITHM) return;
            for (var entry : root.getAsJsonObject("colors").entrySet()) {
                var value = entry.getValue().getAsJsonObject(); int rgb = value.get("rgb").getAsInt();
                if (!entry.getKey().matches("[a-f0-9]{64}") || rgb < 0 || rgb > 0xFFFFFF) throw new IllegalArgumentException("Invalid color cache");
                previous.put(entry.getKey(), new LocalTextureColor.Color(rgb, value.get("opaque").getAsBoolean()));
            }
        } catch (IOException | RuntimeException ex) { previous.clear(); }
    }
    public LocalTextureColor.Color color(byte[] png, JsonObject animation) throws IOException {
        String key = fingerprint(png, animation);
        var value = used.get(key);
        if (value == null) value = previous.get(key);
        if (value == null) value = LocalTextureColor.calculate(png, animation);
        used.put(key, value); return value;
    }
    public static String fingerprint(byte[] png, JsonObject animation) {
        try {
            var digest = MessageDigest.getInstance("SHA-256"); digest.update(png);
            digest.update(("\n" + LocalTextureColor.ALGORITHM + "\n" + animation).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    public void save(Path path) throws IOException {
        JsonObject root = new JsonObject(), colors = new JsonObject(); root.addProperty("algorithm", LocalTextureColor.ALGORITHM);
        used.forEach((key, value) -> { JsonObject item = new JsonObject(); item.addProperty("rgb", value.rgb()); item.addProperty("opaque", value.opaque()); colors.add(key, item); });
        root.add("colors", colors);
        new com.davidblackcn.lorianarchorbit.config.NioAtomicFileWriter(System.getLogger("lorian_arch_orbit.local_colors"))
                .write(path, root.toString().getBytes(StandardCharsets.UTF_8));
    }
}
