package com.davidblackcn.lorianarchorbit.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class FeatureRegistry {
    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_.-]+");

    private final Map<String, Feature<?>> features = new LinkedHashMap<>();
    private List<Feature<?>> orderedFeatures = List.of();
    private boolean frozen;

    public void register(Feature<?> feature) {
        requireMutable();
        Objects.requireNonNull(feature, "feature");
        String id = requireValidId(feature.id());
        if (features.putIfAbsent(id, feature) != null) {
            throw new IllegalArgumentException("Duplicate feature ID: " + id);
        }
    }

    public void freeze() {
        if (frozen) {
            return;
        }
        validateReferences();
        orderedFeatures = Collections.unmodifiableList(topologicalOrder());
        frozen = true;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Feature<?> get(String id) {
        Feature<?> feature = features.get(id);
        if (feature == null) {
            throw new IllegalArgumentException("Unknown feature ID: " + id);
        }
        return feature;
    }

    public List<Feature<?>> orderedFeatures() {
        if (!frozen) {
            throw new IllegalStateException("Feature registry must be frozen first");
        }
        return orderedFeatures;
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(features.keySet());
    }

    private void validateReferences() {
        for (Feature<?> feature : features.values()) {
            validateIds(feature, feature.requires(), "required dependency", true);
            validateIds(feature, feature.optionalDependencies(), "optional dependency", false);
            validateIds(feature, feature.conflicts(), "conflict", true);
        }
    }

    private void validateIds(Feature<?> owner, Set<String> ids, String kind, boolean mustExist) {
        Objects.requireNonNull(ids, owner.id() + " " + kind + " set");
        for (String id : ids) {
            requireValidId(id);
            if (owner.id().equals(id)) {
                throw new IllegalStateException("Feature " + owner.id() + " declares itself as a " + kind);
            }
            if (mustExist && !features.containsKey(id)) {
                throw new IllegalStateException("Feature " + owner.id() + " has missing " + kind + ": " + id);
            }
        }
    }

    private List<Feature<?>> topologicalOrder() {
        List<Feature<?>> result = new ArrayList<>(features.size());
        Set<String> visited = new LinkedHashSet<>();
        Set<String> visiting = new LinkedHashSet<>();
        for (Feature<?> feature : features.values()) {
            visit(feature, visited, visiting, result);
        }
        return result;
    }

    private void visit(
            Feature<?> feature,
            Set<String> visited,
            Set<String> visiting,
            List<Feature<?>> result
    ) {
        if (visited.contains(feature.id())) {
            return;
        }
        if (!visiting.add(feature.id())) {
            throw new IllegalStateException("Feature dependency cycle: " + String.join(" -> ", visiting)
                    + " -> " + feature.id());
        }
        for (String dependencyId : feature.requires()) {
            visit(features.get(dependencyId), visited, visiting, result);
        }
        visiting.remove(feature.id());
        visited.add(feature.id());
        result.add(feature);
    }

    private void requireMutable() {
        if (frozen) {
            throw new IllegalStateException("Feature registry is frozen");
        }
    }

    private static String requireValidId(String id) {
        Objects.requireNonNull(id, "feature ID");
        if (!VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid feature ID: " + id);
        }
        return id;
    }
}
