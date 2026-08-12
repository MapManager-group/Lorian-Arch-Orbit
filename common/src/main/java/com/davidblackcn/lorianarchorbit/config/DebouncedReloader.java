package com.davidblackcn.lorianarchorbit.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class DebouncedReloader implements AutoCloseable {
    private final long delayMillis;
    private final ScheduledExecutorService scheduler;
    private final Executor targetExecutor;
    private final Consumer<Set<Path>> action;
    private final Set<Path> pending = new LinkedHashSet<>();
    private ScheduledFuture<?> scheduled;
    private boolean closed;

    public DebouncedReloader(
            Duration delay,
            ScheduledExecutorService scheduler,
            Executor targetExecutor,
            Consumer<Set<Path>> action
    ) {
        this.delayMillis = Objects.requireNonNull(delay, "delay").toMillis();
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.targetExecutor = Objects.requireNonNull(targetExecutor, "targetExecutor");
        this.action = Objects.requireNonNull(action, "action");
    }

    public synchronized void signal(Path path) {
        if (closed) {
            return;
        }
        pending.add(path.toAbsolutePath().normalize());
        if (scheduled != null) {
            scheduled.cancel(false);
        }
        scheduled = scheduler.schedule(this::submit, delayMillis, TimeUnit.MILLISECONDS);
    }

    private void submit() {
        Set<Path> batch;
        synchronized (this) {
            if (closed || pending.isEmpty()) {
                return;
            }
            batch = Set.copyOf(pending);
            pending.clear();
            scheduled = null;
        }
        targetExecutor.execute(() -> action.accept(batch));
    }

    @Override
    public synchronized void close() {
        closed = true;
        pending.clear();
        if (scheduled != null) {
            scheduled.cancel(false);
            scheduled = null;
        }
    }
}
