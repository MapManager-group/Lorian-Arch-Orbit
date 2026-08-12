package com.davidblackcn.lorianarchorbit.feature;

import java.util.Set;

public interface Feature<T> {
    String id();

    FeatureSide side();

    boolean enabledByDefault();

    FeatureConfigSpec<T> configSpec();

    default String displayNameKey() {
        return "feature.lorian_arch_orbit." + id() + ".name";
    }

    default String descriptionKey() {
        return "feature.lorian_arch_orbit." + id() + ".description";
    }

    default Set<String> requires() {
        return Set.of();
    }

    default Set<String> optionalDependencies() {
        return Set.of();
    }

    default Set<String> conflicts() {
        return Set.of();
    }

    default void onRegister(FeatureContext context) {
    }

    default void onInitialize(FeatureContext context) {
    }

    default void onEnable(FeatureContext context, T config) {
    }

    default void onDisable(FeatureContext context) {
    }

    default void onConfigChanged(FeatureContext context, T config) {
    }

    default void onWorldJoin(FeatureContext context) {
    }

    default void onWorldLeave(FeatureContext context) {
    }

    default void onShutdown(FeatureContext context) {
    }
}
