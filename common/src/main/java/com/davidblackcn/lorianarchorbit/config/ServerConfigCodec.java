package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ServerConfigCodec implements ConfigCodec<ServerConfigSnapshot> {
    @Override
    public ServerConfigSnapshot defaults() {
        JsonObject root = new JsonObject();
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        return decode(root).snapshot();
    }

    @Override
    public DecodedConfig<ServerConfigSnapshot> decode(JsonObject input) {
        JsonObject root = input.deepCopy();
        int version = JsonConfigSupport.version(root);
        JsonConfigSupport.requireSupportedVersion(version);
        boolean migrated = version < ConfigConstants.CURRENT_VERSION;
        if (version == 0) {
            migrateVersionZero(root);
        }
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        List<String> warnings = new ArrayList<>();
        JsonObject features = JsonConfigSupport.object(root, "features", warnings);
        JsonObject reach = JsonConfigSupport.object(features, "reach_extension", warnings);
        boolean enabled = JsonConfigSupport.bool(
                reach, "enabled", false, "features.reach_extension.enabled", warnings
        );
        int maximumDistance = JsonConfigSupport.integer(
                reach, "maximum_distance", 128, 5, 128, "features.reach_extension.maximum_distance", warnings
        );
        boolean creativeOnly = JsonConfigSupport.bool(
                reach, "creative_only", true, "features.reach_extension.creative_only", warnings
        );
        int permissionLevel = JsonConfigSupport.integer(
                reach, "required_permission_level", 0, 0, 4,
                "features.reach_extension.required_permission_level", warnings
        );
        int requestsPerSecond = JsonConfigSupport.integer(
                reach, "requests_per_second", 10, 1, 40,
                "features.reach_extension.requests_per_second", warnings
        );
        return new DecodedConfig<>(new ServerConfigSnapshot(
                root, enabled, maximumDistance, creativeOnly, permissionLevel, requestsPerSecond
        ), migrated, warnings);
    }

    @Override
    public JsonObject encode(ServerConfigSnapshot snapshot) {
        return snapshot.document();
    }

    @Override
    public Set<String> changedNamespaces(ServerConfigSnapshot previous, ServerConfigSnapshot next) {
        JsonElement before = previous.document().getAsJsonObject("features").get("reach_extension");
        JsonElement after = next.document().getAsJsonObject("features").get("reach_extension");
        return before.equals(after) ? Set.of() : Set.of("reach_extension");
    }

    private static void migrateVersionZero(JsonObject root) {
        JsonObject features = root.has("features") && root.get("features").isJsonObject()
                ? root.getAsJsonObject("features")
                : new JsonObject();
        root.add("features", features);
        JsonElement oldReach = features.get("reach_extension");
        if (oldReach != null && oldReach.isJsonPrimitive() && oldReach.getAsJsonPrimitive().isBoolean()) {
            JsonObject reach = new JsonObject();
            reach.addProperty("enabled", oldReach.getAsBoolean());
            features.add("reach_extension", reach);
        }
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
    }
}
