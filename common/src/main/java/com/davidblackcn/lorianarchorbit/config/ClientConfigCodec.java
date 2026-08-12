package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ClientConfigCodec implements ConfigCodec<ClientConfigSnapshot> {
    private static final Map<String, Boolean> FEATURE_DEFAULTS = Map.of(
            "reach_extension", false,
            "palette_wheel", true,
            "smart_pick", true,
            "connected_texture_fix", true,
            "invisible_blocks", true
    );

    @Override
    public ClientConfigSnapshot defaults() {
        return decode(defaultDocument()).snapshot();
    }

    @Override
    public DecodedConfig<ClientConfigSnapshot> decode(JsonObject input) {
        JsonObject root = input.deepCopy();
        int version = JsonConfigSupport.version(root);
        JsonConfigSupport.requireSupportedVersion(version);
        boolean migrated = version < ConfigConstants.CURRENT_VERSION;
        if (version == 0) {
            migrateVersionZero(root);
        }
        if (version < 2) {
            migrateVersionOne(root);
        }

        List<String> warnings = new ArrayList<>();
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        JsonObject features = JsonConfigSupport.object(root, "features", warnings);
        Map<String, Boolean> enabled = new LinkedHashMap<>();
        for (String featureId : ConfigConstants.CLIENT_FEATURE_IDS) {
            JsonObject feature = JsonConfigSupport.object(features, featureId, warnings);
            enabled.put(featureId, JsonConfigSupport.bool(
                    feature,
                    "enabled",
                    FEATURE_DEFAULTS.get(featureId),
                    "features." + featureId + ".enabled",
                    warnings
            ));
        }

        JsonObject reach = features.getAsJsonObject("reach_extension");
        int reachDistance = JsonConfigSupport.integer(
                reach, "distance", 5, 5, 128, "features.reach_extension.distance", warnings
        );

        JsonObject palette = features.getAsJsonObject("palette_wheel");
        migrateColorPresetName(palette, "primary_default_preset");
        migrateColorPresetName(palette, "secondary_default_preset");
        PaletteAnimation animation = JsonConfigSupport.enumValue(
                palette,
                "animation",
                PaletteAnimation.CLOCKWISE,
                PaletteAnimation.class,
                "features.palette_wheel.animation",
                warnings
        );
        PalettePreset primaryPreset = JsonConfigSupport.enumValue(
                palette,
                "primary_default_preset",
                PalettePreset.ITEM_TAG_A,
                PalettePreset.class,
                "features.palette_wheel.primary_default_preset",
                warnings
        );
        PalettePreset secondaryPreset = JsonConfigSupport.enumValue(
                palette,
                "secondary_default_preset",
                PalettePreset.ITEM_TAG_B,
                PalettePreset.class,
                "features.palette_wheel.secondary_default_preset",
                warnings
        );

        JsonObject smartPick = features.getAsJsonObject("smart_pick");
        SmartPickMode smartPickMode = JsonConfigSupport.enumValue(
                smartPick,
                "mode",
                SmartPickMode.CONTEXT,
                SmartPickMode.class,
                "features.smart_pick.mode",
                warnings
        );
        int radius = JsonConfigSupport.integer(
                smartPick, "scan_radius", 3, 1, 3, "features.smart_pick.scan_radius", warnings
        );
        int candidateLimit = JsonConfigSupport.integer(
                smartPick, "candidate_limit", 12, 8, 24, "features.smart_pick.candidate_limit", warnings
        );
        int holdThreshold = JsonConfigSupport.integer(
                smartPick, "hold_threshold_ms", 180, 50, 1000, "features.smart_pick.hold_threshold_ms", warnings
        );
        boolean historyWeight = JsonConfigSupport.bool(
                smartPick, "history_weight", true, "features.smart_pick.history_weight", warnings
        );
        boolean debugStats = JsonConfigSupport.bool(
                smartPick, "debug_stats", false, "features.smart_pick.debug_stats", warnings
        );

        JsonObject connected = features.getAsJsonObject("connected_texture_fix");
        boolean fixWalls = JsonConfigSupport.bool(
                connected, "walls", true, "features.connected_texture_fix.walls", warnings
        );
        boolean fixBeds = JsonConfigSupport.bool(
                connected, "beds", true, "features.connected_texture_fix.beds", warnings
        );
        boolean fixDoors = JsonConfigSupport.bool(
                connected, "doors", true, "features.connected_texture_fix.doors", warnings
        );

        JsonObject invisible = features.getAsJsonObject("invisible_blocks");
        boolean currentlyVisible = JsonConfigSupport.bool(
                invisible, "currently_visible", false, "features.invisible_blocks.currently_visible", warnings
        );
        boolean showBarriers = JsonConfigSupport.bool(
                invisible, "show_barriers", true, "features.invisible_blocks.show_barriers", warnings
        );
        boolean showLightBlocks = JsonConfigSupport.bool(
                invisible, "show_light_blocks", true, "features.invisible_blocks.show_light_blocks", warnings
        );

        JsonObject ui = JsonConfigSupport.object(root, "ui", warnings);
        boolean hudEnabled = JsonConfigSupport.bool(ui, "hud_enabled", true, "ui.hud_enabled", warnings);

        ClientConfigSnapshot snapshot = new ClientConfigSnapshot(
                root,
                enabled,
                reachDistance,
                animation,
                primaryPreset,
                secondaryPreset,
                smartPickMode,
                radius,
                candidateLimit,
                holdThreshold,
                historyWeight,
                debugStats,
                fixWalls,
                fixBeds,
                fixDoors,
                currentlyVisible,
                showBarriers,
                showLightBlocks,
                hudEnabled
        );
        return new DecodedConfig<>(snapshot, migrated, warnings);
    }

    @Override
    public JsonObject encode(ClientConfigSnapshot snapshot) {
        return snapshot.document();
    }

    @Override
    public Set<String> changedNamespaces(ClientConfigSnapshot previous, ClientConfigSnapshot next) {
        Set<String> changed = new LinkedHashSet<>();
        for (String id : ConfigConstants.CLIENT_FEATURE_IDS) {
            if (featureChanged(id, previous, next)) {
                changed.add(id);
            }
        }
        if (previous.hudEnabled() != next.hudEnabled()) {
            changed.add("ui");
        }
        return Set.copyOf(changed);
    }

    private static boolean featureChanged(
            String id,
            ClientConfigSnapshot previous,
            ClientConfigSnapshot next
    ) {
        if (previous.featureEnabled(id) != next.featureEnabled(id)) {
            return true;
        }
        return switch (id) {
            case "reach_extension" -> previous.reachDistance() != next.reachDistance();
            case "palette_wheel" -> previous.paletteAnimation() != next.paletteAnimation()
                    || previous.primaryPalettePreset() != next.primaryPalettePreset()
                    || previous.secondaryPalettePreset() != next.secondaryPalettePreset();
            case "smart_pick" -> previous.smartPickMode() != next.smartPickMode()
                    || previous.smartPickRadius() != next.smartPickRadius()
                    || previous.smartPickCandidateLimit() != next.smartPickCandidateLimit()
                    || previous.smartPickHoldThresholdMs() != next.smartPickHoldThresholdMs()
                    || previous.smartPickHistoryWeight() != next.smartPickHistoryWeight()
                    || previous.smartPickDebugStats() != next.smartPickDebugStats();
            case "invisible_blocks" -> previous.invisibleBlocksVisible() != next.invisibleBlocksVisible()
                    || previous.showBarriers() != next.showBarriers()
                    || previous.showLightBlocks() != next.showLightBlocks();
            case "connected_texture_fix" -> previous.fixWalls() != next.fixWalls()
                    || previous.fixBeds() != next.fixBeds()
                    || previous.fixDoors() != next.fixDoors();
            default -> throw new IllegalArgumentException("Unknown client feature ID: " + id);
        };
    }

    private static JsonObject defaultDocument() {
        JsonObject root = new JsonObject();
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
        return root;
    }

    private static void migrateColorPresetName(JsonObject palette, String key) {
        JsonElement value = palette.get(key);
        if (value != null && value.isJsonPrimitive()
                && "color_placeholder".equalsIgnoreCase(value.getAsString())) {
            palette.addProperty(key, "color_categories");
        }
    }

    private static void migrateVersionZero(JsonObject root) {
        JsonObject features;
        JsonElement existing = root.get("features");
        if (existing != null && existing.isJsonObject()) {
            features = existing.getAsJsonObject();
        } else {
            features = new JsonObject();
            root.add("features", features);
        }
        for (String id : ConfigConstants.CLIENT_FEATURE_IDS) {
            JsonElement oldValue = features.get(id);
            if (oldValue != null && oldValue.isJsonPrimitive() && oldValue.getAsJsonPrimitive().isBoolean()) {
                JsonObject namespace = new JsonObject();
                namespace.addProperty("enabled", oldValue.getAsBoolean());
                features.add(id, namespace);
            }
        }
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
    }

    private static void migrateVersionOne(JsonObject root) {
        JsonObject features = root.has("features") && root.get("features").isJsonObject()
                ? root.getAsJsonObject("features")
                : new JsonObject();
        root.add("features", features);
        JsonElement previous = features.remove("wall_visual_fix");
        if (!features.has("connected_texture_fix")) {
            JsonObject connected = previous != null && previous.isJsonObject()
                    ? previous.getAsJsonObject()
                    : new JsonObject();
            if (previous != null && previous.isJsonPrimitive()
                    && previous.getAsJsonPrimitive().isBoolean()) {
                connected.addProperty("enabled", previous.getAsBoolean());
            }
            connected.addProperty("walls", true);
            connected.addProperty("beds", true);
            connected.addProperty("doors", true);
            features.add("connected_texture_fix", connected);
        }
        root.addProperty("config_version", ConfigConstants.CURRENT_VERSION);
    }
}
