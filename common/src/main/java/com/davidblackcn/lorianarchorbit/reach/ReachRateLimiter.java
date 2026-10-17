package com.davidblackcn.lorianarchorbit.reach;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ReachRateLimiter {
    private final Map<UUID, Window> windows = new HashMap<>();

    public synchronized boolean allow(UUID player, long nowMillis, int requestsPerSecond) {
        Window current = windows.get(player);
        if (current == null || nowMillis - current.startedAt >= 1_000L) {
            windows.put(player, new Window(nowMillis, 1));
            return true;
        }
        if (current.count >= requestsPerSecond) {
            return false;
        }
        windows.put(player, new Window(current.startedAt, current.count + 1));
        return true;
    }

    public synchronized void remove(UUID player) {
        windows.remove(player);
    }

    private record Window(long startedAt, int count) {
    }
}
