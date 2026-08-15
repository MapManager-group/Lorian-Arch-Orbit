package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonObject;

import java.util.Objects;

public final class ClientConfigDraft {
    private static final ClientConfigCodec CODEC = new ClientConfigCodec();
    private JsonObject document;

    public ClientConfigDraft(ClientConfigSnapshot source) {
        this.document = CODEC.encode(Objects.requireNonNull(source, "source"));
    }

    public boolean featureEnabled(String featureId) {
        return snapshot().featureEnabled(featureId);
    }

    public void setFeatureEnabled(String featureId, boolean enabled) {
        if (!ConfigConstants.CLIENT_FEATURE_IDS.contains(featureId)) {
            throw new IllegalArgumentException("Unknown client feature ID: " + featureId);
        }
        feature(featureId).addProperty("enabled", enabled);
    }

    public int reachDistance() {
        return snapshot().reachDistance();
    }

    public void setReachDistance(int distance) {
        feature("reach_extension").addProperty("distance", distance);
    }

    public PaletteAnimation paletteAnimation() {
        return snapshot().paletteAnimation();
    }

    public void setPaletteAnimation(PaletteAnimation animation) {
        feature("palette_wheel").addProperty("animation", animation.name().toLowerCase(java.util.Locale.ROOT));
    }

    public PalettePreset primaryPalettePreset() {
        return snapshot().primaryPalettePreset();
    }

    public void setPrimaryPalettePreset(PalettePreset preset) {
        feature("palette_wheel").addProperty(
                "primary_default_preset", preset.name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    public PalettePreset secondaryPalettePreset() {
        return snapshot().secondaryPalettePreset();
    }

    public void setSecondaryPalettePreset(PalettePreset preset) {
        feature("palette_wheel").addProperty(
                "secondary_default_preset", preset.name().toLowerCase(java.util.Locale.ROOT)
        );
    }

    public SmartPickMode smartPickMode() {
        return snapshot().smartPickMode();
    }

    public void setSmartPickMode(SmartPickMode mode) {
        feature("smart_pick").addProperty("mode", mode.name().toLowerCase(java.util.Locale.ROOT));
    }

    public int smartPickRadius() {
        return snapshot().smartPickRadius();
    }

    public void setSmartPickRadius(int radius) {
        feature("smart_pick").addProperty("scan_radius", radius);
    }

    public int smartPickCandidateLimit() {
        return snapshot().smartPickCandidateLimit();
    }

    public void setSmartPickCandidateLimit(int limit) {
        feature("smart_pick").addProperty("candidate_limit", limit);
    }

    public int smartPickHoldThresholdMs() {
        return snapshot().smartPickHoldThresholdMs();
    }

    public void setSmartPickHoldThresholdMs(int threshold) {
        feature("smart_pick").addProperty("hold_threshold_ms", threshold);
    }

    public boolean smartPickHistoryWeight() {
        return snapshot().smartPickHistoryWeight();
    }

    public void setSmartPickHistoryWeight(boolean enabled) {
        feature("smart_pick").addProperty("history_weight", enabled);
    }

    public boolean smartPickDebugStats() {
        return snapshot().smartPickDebugStats();
    }

    public boolean fixWalls() {
        return snapshot().fixWalls();
    }

    public void setFixWalls(boolean enabled) {
        feature("connected_texture_fix").addProperty("walls", enabled);
    }

    public boolean fixBeds() {
        return snapshot().fixBeds();
    }

    public void setFixBeds(boolean enabled) {
        feature("connected_texture_fix").addProperty("beds", enabled);
    }

    public boolean fixDoors() {
        return snapshot().fixDoors();
    }

    public void setFixDoors(boolean enabled) {
        feature("connected_texture_fix").addProperty("doors", enabled);
    }

    public boolean fixPistons() {
        return snapshot().fixPistons();
    }

    public void setFixPistons(boolean enabled) {
        feature("connected_texture_fix").addProperty("pistons", enabled);
    }

    public boolean fixNetherPortals() {
        return snapshot().fixNetherPortals();
    }

    public void setFixNetherPortals(boolean enabled) {
        feature("connected_texture_fix").addProperty("nether_portals", enabled);
    }

    public boolean fixEndPortals() {
        return snapshot().fixEndPortals();
    }

    public void setFixEndPortals(boolean enabled) {
        feature("connected_texture_fix").addProperty("end_portals", enabled);
    }

    public void setSmartPickDebugStats(boolean enabled) {
        feature("smart_pick").addProperty("debug_stats", enabled);
    }

    public boolean invisibleBlocksVisible() {
        return snapshot().invisibleBlocksVisible();
    }

    public void setInvisibleBlocksVisible(boolean visible) {
        feature("invisible_blocks").addProperty("currently_visible", visible);
    }

    public boolean showBarriers() {
        return snapshot().showBarriers();
    }

    public void setShowBarriers(boolean show) {
        feature("invisible_blocks").addProperty("show_barriers", show);
    }

    public boolean showLightBlocks() {
        return snapshot().showLightBlocks();
    }

    public void setShowLightBlocks(boolean show) {
        feature("invisible_blocks").addProperty("show_light_blocks", show);
    }

    public boolean hudEnabled() {
        return snapshot().hudEnabled();
    }

    public void setHudEnabled(boolean enabled) {
        document.getAsJsonObject("ui").addProperty("hud_enabled", enabled);
    }

    public void restoreDefaults() {
        ClientConfigSnapshot defaults = CODEC.defaults();
        for (String featureId : ConfigConstants.CLIENT_FEATURE_IDS) {
            setFeatureEnabled(featureId, defaults.featureEnabled(featureId));
        }
        setReachDistance(defaults.reachDistance());
        setPaletteAnimation(defaults.paletteAnimation());
        setPrimaryPalettePreset(defaults.primaryPalettePreset());
        setSecondaryPalettePreset(defaults.secondaryPalettePreset());
        setSmartPickMode(defaults.smartPickMode());
        setSmartPickRadius(defaults.smartPickRadius());
        setSmartPickCandidateLimit(defaults.smartPickCandidateLimit());
        setSmartPickHoldThresholdMs(defaults.smartPickHoldThresholdMs());
        setSmartPickHistoryWeight(defaults.smartPickHistoryWeight());
        setSmartPickDebugStats(defaults.smartPickDebugStats());
        setFixWalls(defaults.fixWalls());
        setFixBeds(defaults.fixBeds());
        setFixDoors(defaults.fixDoors());
        setFixPistons(defaults.fixPistons());
        setFixNetherPortals(defaults.fixNetherPortals());
        setFixEndPortals(defaults.fixEndPortals());
        setInvisibleBlocksVisible(defaults.invisibleBlocksVisible());
        setShowBarriers(defaults.showBarriers());
        setShowLightBlocks(defaults.showLightBlocks());
        setHudEnabled(defaults.hudEnabled());
    }

    public ClientConfigSnapshot snapshot() {
        return CODEC.decode(document).snapshot();
    }

    private JsonObject feature(String id) {
        return document.getAsJsonObject("features").getAsJsonObject(id);
    }
}
