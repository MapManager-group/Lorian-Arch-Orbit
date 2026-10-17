package com.davidblackcn.lorianarchorbit.feature;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class FeatureConfigSpec<T> {
    @FunctionalInterface
    public interface Migrator<T> {
        T migrate(int sourceVersion, T value);
    }

    private final Class<T> valueType;
    private final int currentVersion;
    private final String translationKey;
    private final Supplier<T> defaultValueFactory;
    private final UnaryOperator<T> validator;
    private final Migrator<T> migrator;

    public FeatureConfigSpec(
            Class<T> valueType,
            int currentVersion,
            String translationKey,
            Supplier<T> defaultValueFactory,
            UnaryOperator<T> validator,
            Migrator<T> migrator
    ) {
        this.valueType = Objects.requireNonNull(valueType, "valueType");
        if (currentVersion < 1) {
            throw new IllegalArgumentException("currentVersion must be at least 1");
        }
        this.currentVersion = currentVersion;
        this.translationKey = requireText(translationKey, "translationKey");
        this.defaultValueFactory = Objects.requireNonNull(defaultValueFactory, "defaultValueFactory");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.migrator = Objects.requireNonNull(migrator, "migrator");
        defaultValue();
    }

    public static FeatureConfigSpec<EmptyFeatureConfig> empty(String featureId) {
        return new FeatureConfigSpec<>(
                EmptyFeatureConfig.class,
                1,
                "feature.lorian_arch_orbit." + requireText(featureId, "featureId") + ".config",
                () -> EmptyFeatureConfig.INSTANCE,
                UnaryOperator.identity(),
                (sourceVersion, value) -> value
        );
    }

    public Class<T> valueType() {
        return valueType;
    }

    public int currentVersion() {
        return currentVersion;
    }

    public String translationKey() {
        return translationKey;
    }

    public T defaultValue() {
        return validate(defaultValueFactory.get());
    }

    public T validate(T value) {
        T typedValue = valueType.cast(Objects.requireNonNull(value, "value"));
        return valueType.cast(Objects.requireNonNull(validator.apply(typedValue), "validated value"));
    }

    public T migrate(int sourceVersion, T value) {
        if (sourceVersion < 1 || sourceVersion > currentVersion) {
            throw new IllegalArgumentException("Unsupported config version: " + sourceVersion);
        }
        return validate(migrator.migrate(sourceVersion, valueType.cast(value)));
    }

    Object validateObject(Object value) {
        return validate(valueType.cast(value));
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
