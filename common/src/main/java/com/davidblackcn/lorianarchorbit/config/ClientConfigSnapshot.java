package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Objects;

public final class ClientConfigSnapshot {
    private final JsonObject document;
    private final Map<String, Boolean> enabledFeatures;
    private final int reachDistance;
    private final PaletteAnimation paletteAnimation;
    private final PalettePreset primaryPalettePreset;
    private final PalettePreset secondaryPalettePreset;
    private final SmartPickMode smartPickMode;
    private final int smartPickRadius;
    private final int smartPickCandidateLimit;
    private final int smartPickHoldThresholdMs;
    private final boolean smartPickHistoryWeight;
    private final boolean smartPickDebugStats;
    private final boolean fixWalls;
    private final boolean fixBeds;
    private final boolean fixDoors;
    private final boolean invisibleBlocksVisible;
    private final boolean showBarriers;
    private final boolean showLightBlocks;
    private final boolean hudEnabled;

    ClientConfigSnapshot(
            JsonObject document,
            Map<String, Boolean> enabledFeatures,
            int reachDistance,
            PaletteAnimation paletteAnimation,
            PalettePreset primaryPalettePreset,
            PalettePreset secondaryPalettePreset,
            SmartPickMode smartPickMode,
            int smartPickRadius,
            int smartPickCandidateLimit,
            int smartPickHoldThresholdMs,
            boolean smartPickHistoryWeight,
            boolean smartPickDebugStats,
            boolean fixWalls,
            boolean fixBeds,
            boolean fixDoors,
            boolean invisibleBlocksVisible,
            boolean showBarriers,
            boolean showLightBlocks,
            boolean hudEnabled
    ) {
        this.document = document.deepCopy();
        this.enabledFeatures = Map.copyOf(enabledFeatures);
        this.reachDistance = reachDistance;
        this.paletteAnimation = Objects.requireNonNull(paletteAnimation, "paletteAnimation");
        this.primaryPalettePreset = Objects.requireNonNull(primaryPalettePreset, "primaryPalettePreset");
        this.secondaryPalettePreset = Objects.requireNonNull(secondaryPalettePreset, "secondaryPalettePreset");
        this.smartPickMode = Objects.requireNonNull(smartPickMode, "smartPickMode");
        this.smartPickRadius = smartPickRadius;
        this.smartPickCandidateLimit = smartPickCandidateLimit;
        this.smartPickHoldThresholdMs = smartPickHoldThresholdMs;
        this.smartPickHistoryWeight = smartPickHistoryWeight;
        this.smartPickDebugStats = smartPickDebugStats;
        this.fixWalls = fixWalls;
        this.fixBeds = fixBeds;
        this.fixDoors = fixDoors;
        this.invisibleBlocksVisible = invisibleBlocksVisible;
        this.showBarriers = showBarriers;
        this.showLightBlocks = showLightBlocks;
        this.hudEnabled = hudEnabled;
    }

    public boolean featureEnabled(String featureId) {
        Boolean enabled = enabledFeatures.get(featureId);
        if (enabled == null) {
            throw new IllegalArgumentException("Unknown client feature ID: " + featureId);
        }
        return enabled;
    }

    public Map<String, Boolean> enabledFeatures() {
        return enabledFeatures;
    }

    public int reachDistance() {
        return reachDistance;
    }

    public PaletteAnimation paletteAnimation() {
        return paletteAnimation;
    }

    public PalettePreset primaryPalettePreset() {
        return primaryPalettePreset;
    }

    public PalettePreset secondaryPalettePreset() {
        return secondaryPalettePreset;
    }

    public SmartPickMode smartPickMode() {
        return smartPickMode;
    }

    public int smartPickRadius() {
        return smartPickRadius;
    }

    public int smartPickCandidateLimit() {
        return smartPickCandidateLimit;
    }

    public int smartPickHoldThresholdMs() {
        return smartPickHoldThresholdMs;
    }

    public boolean smartPickHistoryWeight() {
        return smartPickHistoryWeight;
    }

    public boolean smartPickDebugStats() {
        return smartPickDebugStats;
    }

    public boolean fixWalls() {
        return fixWalls;
    }

    public boolean fixBeds() {
        return fixBeds;
    }

    public boolean fixDoors() {
        return fixDoors;
    }

    public boolean invisibleBlocksVisible() {
        return invisibleBlocksVisible;
    }

    public boolean showBarriers() {
        return showBarriers;
    }

    public boolean showLightBlocks() {
        return showLightBlocks;
    }

    public boolean hudEnabled() {
        return hudEnabled;
    }

    JsonObject document() {
        return document.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ClientConfigSnapshot that && document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return document.hashCode();
    }
}
