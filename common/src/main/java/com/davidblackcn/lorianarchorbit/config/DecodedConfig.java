package com.davidblackcn.lorianarchorbit.config;

import java.util.List;

public record DecodedConfig<T>(T snapshot, boolean migrated, List<String> warnings) {
    public DecodedConfig {
        warnings = List.copyOf(warnings);
    }
}
