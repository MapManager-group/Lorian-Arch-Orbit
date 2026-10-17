package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ClientInputCoordinatorTest {
    @Test
    public void tracksRebindingDisableAndRegistrationCleanup() {
        ClientInputCoordinator coordinator = new ClientInputCoordinator();
        AtomicReference<String> binding = new AtomicReference<>("r");
        AtomicBoolean down = new AtomicBoolean();
        AtomicBoolean enabled = new AtomicBoolean(true);
        List<InputGesture> events = new ArrayList<>();
        Object world = new Object();
        GestureRegistration registration = coordinator.register(
                "palette",
                new PressTiming(180, 200),
                binding::get,
                down::get,
                enabled::get,
                event -> events.add(event.gesture())
        );

        coordinator.tick(0, true, world);
        down.set(true);
        coordinator.tick(10, true, world);
        binding.set("v");
        coordinator.tick(20, true, world);
        down.set(false);
        coordinator.tick(30, true, world);
        down.set(true);
        coordinator.tick(40, true, world);
        enabled.set(false);
        coordinator.tick(50, true, world);

        assertEquals(List.of(
                InputGesture.PRESSED,
                InputGesture.CANCELLED,
                InputGesture.PRESSED,
                InputGesture.CANCELLED
        ), events);
        registration.close();
        registration.close();
        assertFalse(registration.active());
        assertEquals(0, coordinator.size());
    }

    @Test
    public void rejectsDuplicateOwners() {
        ClientInputCoordinator coordinator = new ClientInputCoordinator();
        coordinator.register("owner", new PressTiming(1, 1), () -> "key", () -> false, () -> true, event -> { });

        assertThrows(IllegalArgumentException.class, () -> coordinator.register(
                "owner", new PressTiming(1, 1), () -> "key", () -> false, () -> true, event -> { }
        ));
    }

    @Test
    public void listenerMayUnregisterItselfDuringDispatch() {
        ClientInputCoordinator coordinator = new ClientInputCoordinator();
        AtomicBoolean down = new AtomicBoolean();
        AtomicReference<GestureRegistration> registration = new AtomicReference<>();
        Object world = new Object();
        registration.set(coordinator.register(
                "self_closing",
                new PressTiming(180, 200),
                () -> "key",
                down::get,
                () -> true,
                event -> registration.get().close()
        ));
        coordinator.tick(0, true, world);
        down.set(true);

        coordinator.tick(10, true, world);

        assertEquals(0, coordinator.size());
        assertFalse(registration.get().active());
    }
}
