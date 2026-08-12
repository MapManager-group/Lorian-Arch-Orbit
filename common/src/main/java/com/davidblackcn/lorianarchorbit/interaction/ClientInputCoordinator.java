package com.davidblackcn.lorianarchorbit.interaction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ClientInputCoordinator {
    private final Map<String, TrackedInput> trackedInputs = new LinkedHashMap<>();

    public synchronized GestureRegistration register(
            String ownerId,
            PressTiming timing,
            Supplier<?> bindingToken,
            BooleanSupplier down,
            BooleanSupplier enabled,
            Consumer<InputGestureEvent> listener
    ) {
        requireOwner(ownerId);
        if (trackedInputs.containsKey(ownerId)) {
            throw new IllegalArgumentException("Input owner is already registered: " + ownerId);
        }
        TrackedInput tracked = new TrackedInput(
                new PressGestureStateMachine(Objects.requireNonNull(timing, "timing")),
                Objects.requireNonNull(bindingToken, "bindingToken"),
                Objects.requireNonNull(down, "down"),
                Objects.requireNonNull(enabled, "enabled"),
                Objects.requireNonNull(listener, "listener")
        );
        trackedInputs.put(ownerId, tracked);
        return new Registration(ownerId, tracked);
    }

    public void tick(long nowMillis, boolean focused, Object worldToken) {
        List<TrackedInput> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(trackedInputs.values());
        }
        for (TrackedInput tracked : snapshot) {
            if (!tracked.active) {
                continue;
            }
            for (InputGestureEvent event : tracked.state.update(
                    nowMillis,
                    tracked.down.getAsBoolean(),
                    tracked.bindingToken.get(),
                    tracked.enabled.getAsBoolean(),
                    focused,
                    worldToken
            )) {
                tracked.listener.accept(event);
                if (!tracked.active) {
                    break;
                }
            }
        }
    }

    public void reset(long nowMillis) {
        List<TrackedInput> snapshot;
        synchronized (this) {
            snapshot = List.copyOf(trackedInputs.values());
        }
        for (TrackedInput tracked : snapshot) {
            if (!tracked.active) {
                continue;
            }
            for (InputGestureEvent event : tracked.state.reset(nowMillis)) {
                tracked.listener.accept(event);
                if (!tracked.active) {
                    break;
                }
            }
        }
    }

    public synchronized int size() {
        return trackedInputs.size();
    }

    private synchronized void unregister(String ownerId, TrackedInput expected) {
        if (trackedInputs.get(ownerId) == expected) {
            expected.active = false;
            trackedInputs.remove(ownerId);
        }
    }

    private static void requireOwner(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
    }

    private static final class TrackedInput {
        private final PressGestureStateMachine state;
        private final Supplier<?> bindingToken;
        private final BooleanSupplier down;
        private final BooleanSupplier enabled;
        private final Consumer<InputGestureEvent> listener;
        private volatile boolean active = true;

        private TrackedInput(
                PressGestureStateMachine state,
                Supplier<?> bindingToken,
                BooleanSupplier down,
                BooleanSupplier enabled,
                Consumer<InputGestureEvent> listener
        ) {
            this.state = state;
            this.bindingToken = bindingToken;
            this.down = down;
            this.enabled = enabled;
            this.listener = listener;
        }
    }

    private final class Registration implements GestureRegistration {
        private final String ownerId;
        private final TrackedInput tracked;
        private boolean active = true;

        private Registration(String ownerId, TrackedInput tracked) {
            this.ownerId = ownerId;
            this.tracked = tracked;
        }

        @Override
        public String ownerId() {
            return ownerId;
        }

        @Override
        public synchronized boolean active() {
            return active;
        }

        @Override
        public synchronized void close() {
            if (!active) {
                return;
            }
            active = false;
            ClientInputCoordinator.this.unregister(ownerId, tracked);
        }
    }
}
