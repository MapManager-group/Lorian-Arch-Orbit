package com.davidblackcn.lorianarchorbit.config;

import java.util.List;
import java.util.Optional;

public record ConfigLoadResult(boolean successful, boolean changed, List<String> warnings, String error) {
    public ConfigLoadResult {
        warnings = List.copyOf(warnings);
    }

    public static ConfigLoadResult success(boolean changed, List<String> warnings) {
        return new ConfigLoadResult(true, changed, warnings, null);
    }

    public static ConfigLoadResult failure(String error) {
        return new ConfigLoadResult(false, false, List.of(), error);
    }

    public Optional<String> errorMessage() {
        return Optional.ofNullable(error);
    }
}
