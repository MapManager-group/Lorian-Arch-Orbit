package com.davidblackcn.lorianarchorbit.palette.hueblocks;

/** Stable texture identities shared by local resources and versioned exports. */
public final class HueResourceId {
    private HueResourceId() { }
    public static String resource(String value) {
        String id = value.contains(":") ? value : "minecraft:" + value;
        if (id.length() > 240 || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                || id.contains("..") || id.contains("//") || id.endsWith("/"))
            throw new IllegalArgumentException("Invalid resource: " + value);
        return id;
    }
    public static String texture(String value) {
        return value.matches("[a-z0-9_]+\\.png")
                ? "minecraft:block/" + value.substring(0, value.length() - 4) : resource(value);
    }
    public static String candidate(String value) {
        String[] parts = value.split("\\|", -1);
        if (parts.length != 2 || value.length() > 512) throw new IllegalArgumentException("Invalid candidate reference");
        return resource(parts[0]) + "|" + texture(parts[1]);
    }
}
