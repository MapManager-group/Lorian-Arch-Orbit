package com.davidblackcn.lorianarchorbit.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class FeatureManager {
    private final FeatureRegistry registry;
    private final RuntimeSide runtimeSide;
    private final FeatureContext context;
    private final Map<String, RuntimeFeature> runtimes = new LinkedHashMap<>();
    private final Map<String, Boolean> userEnabled = new LinkedHashMap<>();
    private final Map<String, Boolean> capabilityAvailable = new LinkedHashMap<>();
    private final Map<String, RuntimeException> failures = new LinkedHashMap<>();
    private final Set<String> worldParticipants = new LinkedHashSet<>();

    private boolean initialized;
    private boolean worldActive;
    private boolean shutdown;

    public FeatureManager(FeatureRegistry registry, RuntimeSide runtimeSide, FeatureServices services) {
        this.registry = Objects.requireNonNull(registry, "registry");
        if (!registry.isFrozen()) {
            throw new IllegalArgumentException("Feature registry must be frozen");
        }
        this.runtimeSide = Objects.requireNonNull(runtimeSide, "runtimeSide");
        Objects.requireNonNull(services, "services");
        this.context = new FeatureContext(runtimeSide, services, this::state);

        for (Feature<?> feature : registry.orderedFeatures()) {
            runtimes.put(feature.id(), new RuntimeFeature(feature, feature.configSpec().defaultValue()));
            userEnabled.put(feature.id(), feature.enabledByDefault());
            capabilityAvailable.put(feature.id(), true);
        }
        setPreInitializationStates();
    }

    public synchronized void initialize() {
        ensureActive();
        if (initialized) {
            return;
        }
        initialized = true;

        for (Feature<?> feature : registry.orderedFeatures()) {
            RuntimeFeature runtime = runtime(feature.id());
            if (!feature.side().supports(runtimeSide)) {
                runtime.state = FeatureState.UNAVAILABLE;
                continue;
            }
            runtime.lifecycleStarted = true;
            try {
                feature.onRegister(context);
            } catch (RuntimeException exception) {
                recordFailure(feature.id(), "registration", exception);
                runtime.state = FeatureState.BLOCKED;
            }
        }

        for (Feature<?> feature : registry.orderedFeatures()) {
            RuntimeFeature runtime = runtime(feature.id());
            if (!runtime.lifecycleStarted || failures.containsKey(feature.id())) {
                continue;
            }
            try {
                feature.onInitialize(context);
                runtime.initialized = true;
            } catch (RuntimeException exception) {
                recordFailure(feature.id(), "initialization", exception);
                runtime.state = FeatureState.BLOCKED;
            }
        }

        reconcileUntilStable();
    }

    public synchronized FeatureState state(String featureId) {
        return runtime(featureId).state;
    }

    public synchronized Map<String, FeatureState> states() {
        Map<String, FeatureState> result = new LinkedHashMap<>();
        runtimes.forEach((id, runtime) -> result.put(id, runtime.state));
        return Collections.unmodifiableMap(result);
    }

    public synchronized Optional<RuntimeException> failure(String featureId) {
        runtime(featureId);
        return Optional.ofNullable(failures.get(featureId));
    }

    public synchronized boolean userEnabled(String featureId) {
        runtime(featureId);
        return userEnabled.get(featureId);
    }

    public synchronized void setUserEnabled(String featureId, boolean enabled) {
        ensureActive();
        runtime(featureId);
        boolean changed = userEnabled.get(featureId) != enabled;
        userEnabled.put(featureId, enabled);
        if (changed && initialized) {
            reconcileUntilStable();
        }
    }

    public synchronized void setCapabilityAvailable(String featureId, boolean available) {
        ensureActive();
        runtime(featureId);
        boolean changed = capabilityAvailable.get(featureId) != available;
        capabilityAvailable.put(featureId, available);
        if (changed && initialized) {
            reconcileUntilStable();
        }
    }

    public synchronized <T> T updateConfig(String featureId, T value) {
        ensureActive();
        RuntimeFeature runtime = runtime(featureId);
        Object validated = runtime.feature.configSpec().validateObject(value);
        runtime.config = validated;

        if (initialized && runtime.initialized && runtime.state == FeatureState.ENABLED) {
            try {
                dispatchConfigChanged(runtime);
            } catch (RuntimeException exception) {
                recordFailure(featureId, "configuration update", exception);
                reconcileUntilStable();
            }
        }
        @SuppressWarnings("unchecked")
        T typedValue = (T) validated;
        return typedValue;
    }

    public synchronized <T> T config(String featureId, Class<T> valueType) {
        Objects.requireNonNull(valueType, "valueType");
        return valueType.cast(runtime(featureId).config);
    }

    public synchronized void onWorldJoin() {
        ensureActive();
        requireInitialized();
        if (worldActive) {
            return;
        }
        worldActive = true;

        for (Feature<?> feature : registry.orderedFeatures()) {
            RuntimeFeature runtime = runtime(feature.id());
            if (runtime.state != FeatureState.ENABLED || !requirementsEnabled(feature)) {
                continue;
            }
            worldParticipants.add(feature.id());
            try {
                feature.onWorldJoin(context);
            } catch (RuntimeException exception) {
                recordFailure(feature.id(), "world join", exception);
            }
        }
        reconcileUntilStable();
    }

    public synchronized void onWorldLeave() {
        ensureActive();
        requireInitialized();
        leaveWorld();
        reconcileUntilStable();
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        leaveWorld();

        List<Feature<?>> reverseOrder = reversedFeatures();
        for (Feature<?> feature : reverseOrder) {
            RuntimeFeature runtime = runtime(feature.id());
            if (runtime.state == FeatureState.ENABLED) {
                runtime.state = FeatureState.DISABLED;
                invokeDisable(runtime);
            }
        }
        for (Feature<?> feature : reverseOrder) {
            RuntimeFeature runtime = runtime(feature.id());
            if (!runtime.lifecycleStarted || runtime.shutdown) {
                continue;
            }
            runtime.shutdown = true;
            try {
                feature.onShutdown(context);
            } catch (RuntimeException exception) {
                recordFailure(feature.id(), "shutdown", exception);
            }
        }
    }

    private void reconcileUntilStable() {
        int previousFailureCount;
        do {
            previousFailureCount = failures.size();
            reconcilePass();
        } while (failures.size() > previousFailureCount);
    }

    private void reconcilePass() {
        Map<String, FeatureState> desiredStates = calculateDesiredStates();
        List<Feature<?>> reverseOrder = reversedFeatures();

        for (Feature<?> feature : reverseOrder) {
            RuntimeFeature runtime = runtime(feature.id());
            FeatureState desired = desiredStates.get(feature.id());
            if (runtime.state == FeatureState.ENABLED && desired != FeatureState.ENABLED) {
                runtime.state = desired;
                deactivate(runtime);
            }
        }

        for (Feature<?> feature : registry.orderedFeatures()) {
            RuntimeFeature runtime = runtime(feature.id());
            FeatureState desired = desiredStates.get(feature.id());
            if (desired != FeatureState.ENABLED) {
                runtime.state = desired;
                continue;
            }
            if (runtime.state == FeatureState.ENABLED) {
                continue;
            }
            if (!requirementsEnabled(feature)) {
                runtime.state = FeatureState.BLOCKED;
                continue;
            }
            activate(runtime);
        }
    }

    private Map<String, FeatureState> calculateDesiredStates() {
        Map<String, FeatureState> desired = new LinkedHashMap<>();
        List<Feature<?>> considered = new ArrayList<>();

        for (Feature<?> feature : registry.orderedFeatures()) {
            FeatureState state;
            if (!feature.side().supports(runtimeSide) || !capabilityAvailable.get(feature.id())) {
                state = FeatureState.UNAVAILABLE;
            } else if (failures.containsKey(feature.id())) {
                state = FeatureState.BLOCKED;
            } else if (!userEnabled.get(feature.id())) {
                state = FeatureState.DISABLED;
            } else if (feature.requires().stream().anyMatch(id -> desired.get(id) != FeatureState.ENABLED)) {
                state = FeatureState.BLOCKED;
            } else if (hasEnabledConflict(feature, considered, desired)) {
                state = FeatureState.BLOCKED;
            } else {
                state = FeatureState.ENABLED;
            }
            desired.put(feature.id(), state);
            considered.add(feature);
        }
        return desired;
    }

    private boolean hasEnabledConflict(
            Feature<?> feature,
            List<Feature<?>> considered,
            Map<String, FeatureState> desired
    ) {
        for (Feature<?> other : considered) {
            if (desired.get(other.id()) != FeatureState.ENABLED) {
                continue;
            }
            if (feature.conflicts().contains(other.id()) || other.conflicts().contains(feature.id())) {
                return true;
            }
        }
        return false;
    }

    private boolean requirementsEnabled(Feature<?> feature) {
        return feature.requires().stream().allMatch(id -> state(id) == FeatureState.ENABLED);
    }

    private void activate(RuntimeFeature runtime) {
        runtime.state = FeatureState.ENABLED;
        try {
            dispatchEnable(runtime);
            if (worldActive) {
                worldParticipants.add(runtime.feature.id());
                runtime.feature.onWorldJoin(context);
            }
        } catch (RuntimeException exception) {
            recordFailure(runtime.feature.id(), "enable", exception);
            runtime.state = FeatureState.BLOCKED;
            deactivate(runtime);
        }
    }

    private void deactivate(RuntimeFeature runtime) {
        if (worldParticipants.remove(runtime.feature.id())) {
            try {
                runtime.feature.onWorldLeave(context);
            } catch (RuntimeException exception) {
                recordFailure(runtime.feature.id(), "world leave", exception);
                runtime.state = FeatureState.BLOCKED;
            }
        }
        invokeDisable(runtime);
    }

    private void invokeDisable(RuntimeFeature runtime) {
        try {
            runtime.feature.onDisable(context);
        } catch (RuntimeException exception) {
            recordFailure(runtime.feature.id(), "disable", exception);
            runtime.state = FeatureState.BLOCKED;
        }
    }

    private void leaveWorld() {
        if (!worldActive) {
            return;
        }
        worldActive = false;
        for (Feature<?> feature : reversedFeatures()) {
            if (!worldParticipants.remove(feature.id())) {
                continue;
            }
            try {
                feature.onWorldLeave(context);
            } catch (RuntimeException exception) {
                recordFailure(feature.id(), "world leave", exception);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void dispatchEnable(RuntimeFeature runtime) {
        Feature<T> feature = (Feature<T>) runtime.feature;
        feature.onEnable(runtime.context(), feature.configSpec().valueType().cast(runtime.config));
    }

    @SuppressWarnings("unchecked")
    private void dispatchConfigChanged(RuntimeFeature runtime) {
        Feature<Object> feature = (Feature<Object>) runtime.feature;
        feature.onConfigChanged(context, runtime.config);
    }

    private void recordFailure(String featureId, String phase, RuntimeException exception) {
        failures.putIfAbsent(featureId, exception);
        context.logger().log(
                System.Logger.Level.ERROR,
                "Feature " + featureId + " failed during " + phase,
                exception
        );
    }

    private void setPreInitializationStates() {
        for (RuntimeFeature runtime : runtimes.values()) {
            runtime.state = runtime.feature.side().supports(runtimeSide)
                    ? FeatureState.DISABLED
                    : FeatureState.UNAVAILABLE;
        }
    }

    private RuntimeFeature runtime(String featureId) {
        Objects.requireNonNull(featureId, "featureId");
        RuntimeFeature runtime = runtimes.get(featureId);
        if (runtime == null) {
            throw new IllegalArgumentException("Unknown feature ID: " + featureId);
        }
        return runtime;
    }

    private List<Feature<?>> reversedFeatures() {
        List<Feature<?>> result = new ArrayList<>(registry.orderedFeatures());
        Collections.reverse(result);
        return result;
    }

    private void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Feature manager is not initialized");
        }
    }

    private void ensureActive() {
        if (shutdown) {
            throw new IllegalStateException("Feature manager is shut down");
        }
    }

    private final class RuntimeFeature {
        private final Feature<?> feature;
        private Object config;
        private FeatureState state;
        private boolean lifecycleStarted;
        private boolean initialized;
        private boolean shutdown;

        private RuntimeFeature(Feature<?> feature, Object config) {
            this.feature = feature;
            this.config = config;
        }

        private FeatureContext context() {
            return context;
        }
    }
}
