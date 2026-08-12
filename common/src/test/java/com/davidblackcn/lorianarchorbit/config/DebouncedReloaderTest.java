package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DebouncedReloaderTest {
    @Test
    public void mergesRapidEventsIntoOneMainThreadSubmission() throws Exception {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger invocations = new AtomicInteger();
        AtomicReference<Set<Path>> batch = new AtomicReference<>();
        CountDownLatch completed = new CountDownLatch(1);
        DebouncedReloader reloader = new DebouncedReloader(
                Duration.ofMillis(30),
                scheduler,
                Runnable::run,
                paths -> {
                    invocations.incrementAndGet();
                    batch.set(paths);
                    completed.countDown();
                }
        );
        try {
            Path client = Path.of("client.json");
            Path wheel = Path.of("wheel.json");
            reloader.signal(client);
            reloader.signal(client);
            reloader.signal(wheel);

            assertTrue(completed.await(2, TimeUnit.SECONDS));
            assertEquals(1, invocations.get());
            assertEquals(Set.of(client.toAbsolutePath().normalize(), wheel.toAbsolutePath().normalize()), batch.get());
        } finally {
            reloader.close();
            scheduler.shutdownNow();
        }
    }
}
