package com.davidblackcn.lorianarchorbit.config;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public final class ConfigDirectoryWatcher implements AutoCloseable {
    private final Path directory;
    private final Set<String> watchedNames;
    private final System.Logger logger;
    private final WatchService watchService;
    private final ScheduledExecutorService scheduler;
    private final DebouncedReloader reloader;
    private final Thread thread;
    private volatile boolean closed;

    public ConfigDirectoryWatcher(
            Path directory,
            Set<String> watchedNames,
            Duration debounce,
            Executor mainExecutor,
            Consumer<Set<Path>> action,
            System.Logger logger
    ) throws IOException {
        this.directory = directory.toAbsolutePath().normalize();
        this.watchedNames = Set.copyOf(watchedNames);
        this.logger = Objects.requireNonNull(logger, "logger");
        Files.createDirectories(this.directory);
        this.watchService = FileSystems.getDefault().newWatchService();
        this.directory.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread worker = new Thread(runnable, "Lorian Arch Orbit config debounce");
            worker.setDaemon(true);
            return worker;
        });
        this.reloader = new DebouncedReloader(debounce, scheduler, mainExecutor, action);
        this.thread = new Thread(this::watch, "Lorian Arch Orbit config watcher");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private void watch() {
        try {
            while (!closed) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW || !(event.context() instanceof Path relative)) {
                        continue;
                    }
                    if (watchedNames.contains(relative.getFileName().toString())) {
                        reloader.signal(directory.resolve(relative));
                    }
                }
                if (!key.reset()) {
                    logger.log(System.Logger.Level.WARNING, "Configuration directory watcher became invalid: " + directory);
                    return;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (ClosedWatchServiceException ignored) {
            // Normal shutdown.
        } catch (RuntimeException exception) {
            logger.log(System.Logger.Level.ERROR, "Configuration watcher failed for " + directory, exception);
        }
    }

    @Override
    public void close() {
        closed = true;
        reloader.close();
        scheduler.shutdownNow();
        try {
            watchService.close();
        } catch (IOException exception) {
            logger.log(System.Logger.Level.WARNING, "Could not close configuration watcher for " + directory, exception);
        }
        thread.interrupt();
    }
}
