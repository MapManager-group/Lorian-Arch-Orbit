package com.davidblackcn.lorianarchorbit.config;

import com.google.gson.JsonObject;

public final class ServerConfigSnapshot {
    private final JsonObject document;
    private final boolean reachEnabled;
    private final int maximumDistance;
    private final boolean creativeOnly;
    private final int requiredPermissionLevel;
    private final int requestsPerSecond;

    ServerConfigSnapshot(
            JsonObject document,
            boolean reachEnabled,
            int maximumDistance,
            boolean creativeOnly,
            int requiredPermissionLevel,
            int requestsPerSecond
    ) {
        this.document = document.deepCopy();
        this.reachEnabled = reachEnabled;
        this.maximumDistance = maximumDistance;
        this.creativeOnly = creativeOnly;
        this.requiredPermissionLevel = requiredPermissionLevel;
        this.requestsPerSecond = requestsPerSecond;
    }

    public boolean reachEnabled() {
        return reachEnabled;
    }

    public int maximumDistance() {
        return maximumDistance;
    }

    public boolean creativeOnly() {
        return creativeOnly;
    }

    public int requiredPermissionLevel() {
        return requiredPermissionLevel;
    }

    public int requestsPerSecond() {
        return requestsPerSecond;
    }

    JsonObject document() {
        return document.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ServerConfigSnapshot that && document.equals(that.document);
    }

    @Override
    public int hashCode() {
        return document.hashCode();
    }
}
