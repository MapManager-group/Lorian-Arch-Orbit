package com.davidblackcn.lorianarchorbit.feature;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class FeatureManagerTest {
    @Test
    public void filtersByRuntimeSideAndTracksUserPreferenceSeparately() {
        RecordingFeature client = new RecordingFeature("client", FeatureSide.CLIENT, true);
        RecordingFeature both = new RecordingFeature("both", FeatureSide.BOTH, false);
        FeatureManager manager = manager(RuntimeSide.SERVER, client, both);

        manager.initialize();

        assertEquals(FeatureState.UNAVAILABLE, manager.state("client"));
        assertTrue(manager.userEnabled("client"));
        assertEquals(FeatureState.DISABLED, manager.state("both"));

        manager.setUserEnabled("both", true);
        manager.setUserEnabled("both", true);
        assertEquals(FeatureState.ENABLED, manager.state("both"));
        assertEquals(1, both.count("enable"));
    }

    @Test
    public void blocksDependentsAndRecoversWhenDependencyReturns() {
        RecordingFeature provider = new RecordingFeature("provider", FeatureSide.BOTH, true);
        RecordingFeature consumer = new RecordingFeature(
                "consumer", FeatureSide.BOTH, true, Set.of("provider"), Set.of()
        );
        FeatureManager manager = manager(RuntimeSide.CLIENT, consumer, provider);
        manager.initialize();

        assertEquals(FeatureState.ENABLED, manager.state("provider"));
        assertEquals(FeatureState.ENABLED, manager.state("consumer"));

        manager.setCapabilityAvailable("provider", false);
        assertEquals(FeatureState.UNAVAILABLE, manager.state("provider"));
        assertEquals(FeatureState.BLOCKED, manager.state("consumer"));
        assertEquals(1, consumer.count("disable"));
        assertEquals(1, provider.count("disable"));

        manager.setCapabilityAvailable("provider", true);
        assertEquals(FeatureState.ENABLED, manager.state("provider"));
        assertEquals(FeatureState.ENABLED, manager.state("consumer"));
        assertEquals(2, provider.count("enable"));
        assertEquals(2, consumer.count("enable"));
    }

    @Test
    public void resolvesConflictsDeterministicallyByRegistryOrder() {
        RecordingFeature first = new RecordingFeature(
                "first", FeatureSide.BOTH, true, Set.of(), Set.of("second")
        );
        RecordingFeature second = new RecordingFeature("second", FeatureSide.BOTH, true);
        FeatureManager manager = manager(RuntimeSide.CLIENT, first, second);

        manager.initialize();

        assertEquals(FeatureState.ENABLED, manager.state("first"));
        assertEquals(FeatureState.BLOCKED, manager.state("second"));
    }

    @Test
    public void keepsLifecycleIdempotentAndCleansWorldStateInReverseOrder() {
        List<String> sharedEvents = new ArrayList<>();
        RecordingFeature provider = new RecordingFeature(
                "provider", FeatureSide.BOTH, true, Set.of(), Set.of(), sharedEvents
        );
        RecordingFeature consumer = new RecordingFeature(
                "consumer", FeatureSide.BOTH, true, Set.of("provider"), Set.of(), sharedEvents
        );
        FeatureManager manager = manager(RuntimeSide.CLIENT, consumer, provider);

        manager.initialize();
        manager.initialize();
        manager.onWorldJoin();
        manager.onWorldJoin();
        manager.onWorldLeave();
        manager.onWorldLeave();
        manager.shutdown();
        manager.shutdown();

        assertEquals(1, provider.count("register"));
        assertEquals(1, provider.count("initialize"));
        assertEquals(1, provider.count("enable"));
        assertEquals(1, provider.count("join"));
        assertEquals(1, provider.count("leave"));
        assertEquals(1, provider.count("disable"));
        assertEquals(1, provider.count("shutdown"));
        assertTrue(sharedEvents.indexOf("provider:register") < sharedEvents.indexOf("consumer:register"));
        assertTrue(sharedEvents.indexOf("consumer:register") < sharedEvents.indexOf("provider:initialize"));
        assertTrue(sharedEvents.indexOf("consumer:leave") < sharedEvents.indexOf("provider:leave"));
        assertTrue(sharedEvents.indexOf("consumer:disable") < sharedEvents.indexOf("provider:disable"));
        assertThrows(IllegalStateException.class, () -> manager.setUserEnabled("provider", false));
    }

    @Test
    public void isolatesInitializationFailuresAndBlocksOnlyDependents() {
        RecordingFeature broken = new RecordingFeature("broken", FeatureSide.BOTH, true);
        broken.failInitialization = true;
        RecordingFeature dependent = new RecordingFeature(
                "dependent", FeatureSide.BOTH, true, Set.of("broken"), Set.of()
        );
        RecordingFeature independent = new RecordingFeature("independent", FeatureSide.BOTH, true);
        FeatureManager manager = manager(RuntimeSide.CLIENT, broken, dependent, independent);

        manager.initialize();

        assertEquals(FeatureState.BLOCKED, manager.state("broken"));
        assertEquals(FeatureState.BLOCKED, manager.state("dependent"));
        assertEquals(FeatureState.ENABLED, manager.state("independent"));
        assertTrue(manager.failure("broken").isPresent());
        assertFalse(manager.failure("independent").isPresent());
    }

    @Test
    public void dispatchesValidatedConfigurationOnlyToItsOwner() {
        RecordingFeature first = new RecordingFeature("first", FeatureSide.BOTH, true);
        RecordingFeature second = new RecordingFeature("second", FeatureSide.BOTH, true);
        FeatureManager manager = manager(RuntimeSide.CLIENT, first, second);
        manager.initialize();

        assertEquals("changed", manager.updateConfig("first", "  changed  "));

        assertEquals("changed", manager.config("first", String.class));
        assertEquals(1, first.count("config:changed"));
        assertEquals(0, second.count("config:changed"));
        assertThrows(ClassCastException.class, () -> manager.updateConfig("first", 3));
    }

    @Test
    public void disabledFeaturesDoNotAcquireRuntimeOrWorldResources() {
        RecordingFeature disabled = new RecordingFeature("disabled", FeatureSide.BOTH, false);
        FeatureManager manager = manager(RuntimeSide.CLIENT, disabled);

        manager.initialize();
        manager.onWorldJoin();
        manager.onWorldLeave();

        assertEquals(FeatureState.DISABLED, manager.state("disabled"));
        assertEquals(0, disabled.count("enable"));
        assertEquals(0, disabled.count("join"));
        assertEquals(0, disabled.count("leave"));
        assertEquals(0, disabled.count("disable"));
    }

    @Test
    public void disablingInsideAWorldReleasesWorldResourcesBeforeRuntimeResources() {
        RecordingFeature feature = new RecordingFeature("active", FeatureSide.BOTH, true);
        FeatureManager manager = manager(RuntimeSide.CLIENT, feature);
        manager.initialize();
        manager.onWorldJoin();

        manager.setUserEnabled("active", false);
        manager.setUserEnabled("active", false);

        assertEquals(FeatureState.DISABLED, manager.state("active"));
        assertEquals(1, feature.count("leave"));
        assertEquals(1, feature.count("disable"));
        assertTrue(feature.events.indexOf("active:leave") < feature.events.indexOf("active:disable"));
    }

    @Test
    public void disabledFeaturesUseTheLatestConfigOnlyWhenEnabled() {
        RecordingFeature feature = new RecordingFeature("disabled", FeatureSide.BOTH, false);
        FeatureManager manager = manager(RuntimeSide.CLIENT, feature);
        manager.initialize();

        manager.updateConfig("disabled", "  latest  ");
        assertEquals(0, feature.count("config:latest"));

        manager.setUserEnabled("disabled", true);
        assertEquals("latest", feature.lastEnabledConfig);
        assertEquals(1, feature.count("enable"));
    }

    private static FeatureManager manager(RuntimeSide side, RecordingFeature... features) {
        FeatureRegistry registry = new FeatureRegistry();
        for (RecordingFeature feature : features) {
            registry.register(feature);
        }
        registry.freeze();
        return new FeatureManager(
                registry,
                side,
                FeatureServices.noOp(SilentLogger.INSTANCE)
        );
    }

    private enum SilentLogger implements System.Logger {
        INSTANCE;

        @Override
        public String getName() {
            return "feature-manager-test";
        }

        @Override
        public boolean isLoggable(Level level) {
            return false;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String message, Throwable thrown) {
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
        }
    }

    private static final class RecordingFeature implements Feature<String> {
        private final String id;
        private final FeatureSide side;
        private final boolean enabledByDefault;
        private final Set<String> requirements;
        private final Set<String> conflicts;
        private final List<String> events;
        private final FeatureConfigSpec<String> configSpec;
        private boolean failInitialization;
        private String lastEnabledConfig;

        private RecordingFeature(String id, FeatureSide side, boolean enabledByDefault) {
            this(id, side, enabledByDefault, Set.of(), Set.of());
        }

        private RecordingFeature(
                String id,
                FeatureSide side,
                boolean enabledByDefault,
                Set<String> requirements,
                Set<String> conflicts
        ) {
            this(id, side, enabledByDefault, requirements, conflicts, new ArrayList<>());
        }

        private RecordingFeature(
                String id,
                FeatureSide side,
                boolean enabledByDefault,
                Set<String> requirements,
                Set<String> conflicts,
                List<String> events
        ) {
            this.id = id;
            this.side = side;
            this.enabledByDefault = enabledByDefault;
            this.requirements = requirements;
            this.conflicts = conflicts;
            this.events = events;
            this.configSpec = FeatureRegistryTest.stringConfig(id);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public FeatureSide side() {
            return side;
        }

        @Override
        public boolean enabledByDefault() {
            return enabledByDefault;
        }

        @Override
        public FeatureConfigSpec<String> configSpec() {
            return configSpec;
        }

        @Override
        public Set<String> requires() {
            return requirements;
        }

        @Override
        public Set<String> conflicts() {
            return conflicts;
        }

        @Override
        public void onRegister(FeatureContext context) {
            event("register");
        }

        @Override
        public void onInitialize(FeatureContext context) {
            event("initialize");
            if (failInitialization) {
                throw new IllegalStateException("expected test failure");
            }
        }

        @Override
        public void onEnable(FeatureContext context, String config) {
            assertTrue(context.isEnabled(id));
            lastEnabledConfig = config;
            event("enable");
        }

        @Override
        public void onDisable(FeatureContext context) {
            assertFalse(context.isEnabled(id));
            event("disable");
        }

        @Override
        public void onConfigChanged(FeatureContext context, String config) {
            event("config:" + config);
        }

        @Override
        public void onWorldJoin(FeatureContext context) {
            event("join");
        }

        @Override
        public void onWorldLeave(FeatureContext context) {
            event("leave");
        }

        @Override
        public void onShutdown(FeatureContext context) {
            event("shutdown");
        }

        private void event(String event) {
            events.add(id + ":" + event);
        }

        private long count(String suffix) {
            return events.stream().filter(event -> event.equals(id + ":" + suffix)).count();
        }
    }
}
