package com.davidblackcn.lorianarchorbit.config;

import java.nio.file.Path;
import java.util.Set;

public record ConfigChange(Path path, Set<String> namespaces) {
    public ConfigChange {
        path = path.toAbsolutePath().normalize();
        namespaces = Set.copyOf(namespaces);
    }
}
