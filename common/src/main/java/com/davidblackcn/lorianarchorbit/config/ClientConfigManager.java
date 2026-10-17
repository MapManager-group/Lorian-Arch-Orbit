package com.davidblackcn.lorianarchorbit.config;

import com.davidblackcn.lorianarchorbit.palette.BuiltinPalettePresets;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class ClientConfigManager implements AutoCloseable {
    private final Path directory;
    private final System.Logger logger;
    private final ConfigFile<ClientConfigSnapshot> client;
    private final ConfigFile<WheelConfigSnapshot> primaryWheel;
    private final ConfigFile<WheelConfigSnapshot> secondaryWheel;
    private final List<Consumer<ConfigChange>> listeners = new CopyOnWriteArrayList<>();
    private ConfigDirectoryWatcher watcher;

    public ClientConfigManager(Path directory, System.Logger logger) {
        this(directory, logger, new NioAtomicFileWriter(logger));
    }

    public ClientConfigManager(Path directory, System.Logger logger, AtomicFileWriter writer) {
        this.directory = directory.toAbsolutePath().normalize();
        this.logger = Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(writer, "writer");
        this.client = new ConfigFile<>(
                this.directory.resolve(ConfigConstants.CLIENT_FILE),
                new ClientConfigCodec(),
                writer,
                logger,
                this::publish
        );
        this.primaryWheel = new ConfigFile<>(
                this.directory.resolve(ConfigConstants.PRIMARY_WHEEL_FILE),
                new WheelConfigCodec(() -> BuiltinPalettePresets.groups(client.current().primaryPalettePreset())),
                writer,
                logger,
                this::publish
        );
        this.secondaryWheel = new ConfigFile<>(
                this.directory.resolve(ConfigConstants.SECONDARY_WHEEL_FILE),
                new WheelConfigCodec(() -> BuiltinPalettePresets.groups(client.current().secondaryPalettePreset())),
                writer,
                logger,
                this::publish
        );
    }

    public Map<Path, ConfigLoadResult> load() {
        Map<Path, ConfigLoadResult> results = new LinkedHashMap<>();
        results.put(client.path(), client.load());
        results.put(primaryWheel.path(), primaryWheel.load());
        results.put(secondaryWheel.path(), secondaryWheel.load());
        return Map.copyOf(results);
    }

    public ClientConfigSnapshot client() {
        return client.current();
    }

    public WheelConfigSnapshot primaryWheel() {
        return primaryWheel.current();
    }

    public WheelConfigSnapshot secondaryWheel() {
        return secondaryWheel.current();
    }

    public ConfigLoadResult save(ClientConfigDraft draft) {
        ClientConfigSnapshot previous = client.current();
        ConfigLoadResult result = client.save(Objects.requireNonNull(draft, "draft").snapshot());
        if (result.successful() && presetsChanged(previous, client.current())) {
            primaryWheel.reload();
            secondaryWheel.reload();
        }
        return result;
    }

    public ConfigLoadResult savePrimaryWheel(WheelConfigSnapshot snapshot) {
        return primaryWheel.save(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public ConfigLoadResult saveSecondaryWheel(WheelConfigSnapshot snapshot) {
        return secondaryWheel.save(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public Map<Path, ConfigLoadResult> reloadAll() {
        return reload(Set.of(client.path(), primaryWheel.path(), secondaryWheel.path()));
    }

    public Map<Path, ConfigLoadResult> reload(Set<Path> paths) {
        Map<Path, ConfigLoadResult> results = new LinkedHashMap<>();
        ClientConfigSnapshot previous = client.current();
        boolean clientReloaded = false;
        for (Path path : paths) {
            if (path.toAbsolutePath().normalize().equals(client.path())) {
                results.put(client.path(), client.reload());
                clientReloaded = true;
                break;
            }
        }
        boolean reloadWheels = clientReloaded && presetsChanged(previous, client.current());
        for (Path path : paths) {
            Path normalized = path.toAbsolutePath().normalize();
            if (normalized.equals(primaryWheel.path())) {
                results.put(normalized, primaryWheel.reload());
            } else if (normalized.equals(secondaryWheel.path())) {
                results.put(normalized, secondaryWheel.reload());
            }
        }
        if (reloadWheels) {
            results.computeIfAbsent(primaryWheel.path(), ignored -> primaryWheel.reload());
            results.computeIfAbsent(secondaryWheel.path(), ignored -> secondaryWheel.reload());
        }
        return Map.copyOf(results);
    }

    public void addListener(Consumer<ConfigChange> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void startWatching(Executor mainExecutor) throws IOException {
        if (watcher != null) {
            return;
        }
        watcher = new ConfigDirectoryWatcher(
                directory,
                Set.of(
                        ConfigConstants.CLIENT_FILE,
                        ConfigConstants.PRIMARY_WHEEL_FILE,
                        ConfigConstants.SECONDARY_WHEEL_FILE
                ),
                Duration.ofMillis(250),
                mainExecutor,
                this::reload,
                logger
        );
    }

    public Path directory() {
        return directory;
    }

    private void publish(ConfigChange change) {
        for (Consumer<ConfigChange> listener : listeners) {
            listener.accept(change);
        }
    }

    private static boolean presetsChanged(ClientConfigSnapshot previous, ClientConfigSnapshot next) {
        return previous.primaryPalettePreset() != next.primaryPalettePreset()
                || previous.secondaryPalettePreset() != next.secondaryPalettePreset();
    }

    @Override
    public void close() {
        if (watcher != null) {
            watcher.close();
            watcher = null;
        }
    }
}
