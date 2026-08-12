package com.davidblackcn.lorianarchorbit.config;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class NioAtomicFileWriter implements AtomicFileWriter {
    private final System.Logger logger;

    public NioAtomicFileWriter(System.Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void write(Path target, byte[] content) throws IOException {
        Path parent = target.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("Configuration path has no parent: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException exception) {
                logger.log(
                        System.Logger.Level.WARNING,
                        "Atomic replacement is not supported for " + target + "; using same-directory replacement"
                );
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
