package com.davidblackcn.lorianarchorbit.config;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class ServerConfigManager implements AutoCloseable {
    private final Path directory;
    private final System.Logger logger;
    private final ConfigFile<ServerConfigSnapshot> server;
    private ConfigDirectoryWatcher watcher;

    public ServerConfigManager(Path directory, System.Logger logger) {
        this(directory, logger, new NioAtomicFileWriter(logger), change -> { });
    }

    public ServerConfigManager(
            Path directory,
            System.Logger logger,
            AtomicFileWriter writer,
            Consumer<ConfigChange> listener
    ) {
        Objects.requireNonNull(directory, "directory");
        this.directory = directory.toAbsolutePath().normalize();
        this.logger = Objects.requireNonNull(logger, "logger");
        this.server = new ConfigFile<>(
                this.directory.resolve(ConfigConstants.SERVER_FILE),
                new ServerConfigCodec(),
                writer,
                this.logger,
                listener
        );
    }

    public ConfigLoadResult load() {
        return server.load();
    }

    public ConfigLoadResult reload() {
        return server.reload();
    }

    public ServerConfigSnapshot current() {
        return server.current();
    }

    public Path path() {
        return server.path();
    }

    public synchronized void startWatching(Executor mainExecutor) throws IOException {
        if (watcher != null) return;
        watcher = new ConfigDirectoryWatcher(
                directory,
                Set.of(ConfigConstants.SERVER_FILE),
                Duration.ofMillis(250),
                mainExecutor,
                ignored -> reload(),
                logger
        );
    }

    @Override
    public synchronized void close() {
        if (watcher == null) return;
        watcher.close();
        watcher = null;
    }
}
