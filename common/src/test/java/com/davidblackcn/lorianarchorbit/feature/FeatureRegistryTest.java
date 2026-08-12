package com.davidblackcn.lorianarchorbit.feature;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class FeatureRegistryTest {
    @Test
    public void rejectsDuplicateIds() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(feature("duplicate", Set.of()));

        assertThrows(IllegalArgumentException.class,
                () -> registry.register(feature("duplicate", Set.of())));
    }

    @Test
    public void rejectsRegistrationAfterFreeze() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(feature("first", Set.of()));
        registry.freeze();

        assertThrows(IllegalStateException.class,
                () -> registry.register(feature("second", Set.of())));
    }

    @Test
    public void rejectsMissingRequiredDependencies() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(feature("dependent", Set.of("missing")));

        assertThrows(IllegalStateException.class, registry::freeze);
    }

    @Test
    public void rejectsDependencyCycles() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(feature("first", Set.of("second")));
        registry.register(feature("second", Set.of("first")));

        assertThrows(IllegalStateException.class, registry::freeze);
    }

    @Test
    public void ordersRequiredDependenciesBeforeConsumers() {
        FeatureRegistry registry = new FeatureRegistry();
        registry.register(feature("consumer", Set.of("provider")));
        registry.register(feature("provider", Set.of()));
        registry.freeze();

        assertEquals(
                java.util.List.of("provider", "consumer"),
                registry.orderedFeatures().stream().map(Feature::id).toList()
        );
    }

    private static Feature<String> feature(String id, Set<String> requirements) {
        return new Feature<>() {
            private final FeatureConfigSpec<String> config = stringConfig(id);

            @Override
            public String id() {
                return id;
            }

            @Override
            public FeatureSide side() {
                return FeatureSide.BOTH;
            }

            @Override
            public boolean enabledByDefault() {
                return true;
            }

            @Override
            public FeatureConfigSpec<String> configSpec() {
                return config;
            }

            @Override
            public Set<String> requires() {
                return requirements;
            }
        };
    }

    static FeatureConfigSpec<String> stringConfig(String id) {
        return new FeatureConfigSpec<>(
                String.class,
                1,
                "test." + id,
                () -> "default",
                String::trim,
                (sourceVersion, value) -> value
        );
    }
}
