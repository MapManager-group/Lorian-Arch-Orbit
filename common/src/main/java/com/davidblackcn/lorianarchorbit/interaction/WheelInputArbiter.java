package com.davidblackcn.lorianarchorbit.interaction;

import java.util.Objects;
import java.util.Optional;

public final class WheelInputArbiter {
    private Claim current;
    private long generation;

    public Optional<WheelLease> claim(
            String ownerId,
            WheelPriority priority,
            WheelScrollHandler handler,
            Runnable revoked
    ) {
        requireOwner(ownerId);
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(revoked, "revoked");
        Claim previous;
        Claim claim;
        synchronized (this) {
            if (current != null && !current.ownerId.equals(ownerId)
                    && current.priority.value() >= priority.value()) {
                return Optional.empty();
            }
            previous = current;
            if (previous != null) {
                previous.active = false;
            }
            claim = new Claim(ownerId, priority, handler, revoked, ++generation);
            current = claim;
        }
        if (previous != null) {
            previous.revoked.run();
        }
        return Optional.of(new Lease(claim.ownerId, claim.generation));
    }

    public boolean dispatch(double amountX, double amountY) {
        Claim claim;
        synchronized (this) {
            claim = current;
        }
        return claim != null && (amountX != 0.0 || amountY != 0.0)
                && claim.handler.onScroll(amountX, amountY);
    }

    public synchronized Optional<String> ownerId() {
        return current == null ? Optional.empty() : Optional.of(current.ownerId);
    }

    public void releaseOwner(String ownerId) {
        requireOwner(ownerId);
        Claim released = null;
        synchronized (this) {
            if (current != null && current.ownerId.equals(ownerId)) {
                released = current;
                released.active = false;
                current = null;
            }
        }
        if (released != null) {
            released.revoked.run();
        }
    }

    public void clear() {
        Claim released;
        synchronized (this) {
            released = current;
            if (released != null) {
                released.active = false;
            }
            current = null;
        }
        if (released != null) {
            released.revoked.run();
        }
    }

    private void close(String ownerId, long leaseGeneration) {
        Claim released = null;
        synchronized (this) {
            if (current != null && current.ownerId.equals(ownerId) && current.generation == leaseGeneration) {
                released = current;
                released.active = false;
                current = null;
            }
        }
        if (released != null) {
            released.revoked.run();
        }
    }

    private static void requireOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
    }

    private final class Lease implements WheelLease {
        private final String ownerId;
        private final long leaseGeneration;

        private Lease(String ownerId, long leaseGeneration) {
            this.ownerId = ownerId;
            this.leaseGeneration = leaseGeneration;
        }

        @Override
        public String ownerId() {
            return ownerId;
        }

        @Override
        public boolean active() {
            synchronized (WheelInputArbiter.this) {
                return current != null && current.active && current.ownerId.equals(ownerId)
                        && current.generation == leaseGeneration;
            }
        }

        @Override
        public void close() {
            WheelInputArbiter.this.close(ownerId, leaseGeneration);
        }
    }

    private static final class Claim {
        private final String ownerId;
        private final WheelPriority priority;
        private final WheelScrollHandler handler;
        private final Runnable revoked;
        private final long generation;
        private boolean active = true;

        private Claim(
                String ownerId,
                WheelPriority priority,
                WheelScrollHandler handler,
                Runnable revoked,
                long generation
        ) {
            this.ownerId = ownerId;
            this.priority = priority;
            this.handler = handler;
            this.revoked = revoked;
            this.generation = generation;
        }
    }
}
