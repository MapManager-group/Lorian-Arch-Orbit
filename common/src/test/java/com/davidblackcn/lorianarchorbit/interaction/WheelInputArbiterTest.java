package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class WheelInputArbiterTest {
    @Test
    public void onlyConsumesWhenTheActiveOwnerHandlesTheEvent() {
        WheelInputArbiter arbiter = new WheelInputArbiter();
        AtomicInteger calls = new AtomicInteger();
        WheelLease lease = arbiter.claim(
                "reach", WheelPriority.REACH_ADJUSTMENT,
                (x, y) -> {
                    calls.incrementAndGet();
                    return y != 0.0;
                },
                () -> { }
        ).orElseThrow();

        assertTrue(arbiter.dispatch(0.0, -1.0));
        assertFalse(arbiter.dispatch(1.0, 0.0));
        assertEquals(2, calls.get());
        lease.close();
        assertFalse(arbiter.dispatch(0.0, 1.0));
    }

    @Test
    public void higherPriorityPreemptsAndStaleLeaseCannotReleaseNewOwner() {
        WheelInputArbiter arbiter = new WheelInputArbiter();
        AtomicInteger reachRevoked = new AtomicInteger();
        WheelLease reach = arbiter.claim(
                "reach", WheelPriority.REACH_ADJUSTMENT, (x, y) -> true, reachRevoked::incrementAndGet
        ).orElseThrow();

        assertTrue(arbiter.claim(
                "palette", WheelPriority.PALETTE_WHEEL, (x, y) -> true, () -> { }
        ).isPresent());
        assertEquals(1, reachRevoked.get());
        assertFalse(reach.active());
        reach.close();
        assertEquals("palette", arbiter.ownerId().orElseThrow());
        assertTrue(arbiter.dispatch(0.0, 1.0));
    }

    @Test
    public void lowerOrEqualPriorityCannotStealAnotherOwner() {
        WheelInputArbiter arbiter = new WheelInputArbiter();
        arbiter.claim("palette", WheelPriority.PALETTE_WHEEL, (x, y) -> true, () -> { }).orElseThrow();

        assertTrue(arbiter.claim(
                "smart", WheelPriority.SMART_PICK, (x, y) -> true, () -> { }
        ).isEmpty());
        assertTrue(arbiter.claim(
                "other_palette", WheelPriority.PALETTE_WHEEL, (x, y) -> true, () -> { }
        ).isEmpty());
        assertEquals("palette", arbiter.ownerId().orElseThrow());
    }

    @Test
    public void clearRevokesTheOwnerAndRestoresPassThrough() {
        WheelInputArbiter arbiter = new WheelInputArbiter();
        AtomicInteger revoked = new AtomicInteger();
        arbiter.claim(
                "smart", WheelPriority.SMART_PICK, (x, y) -> true, revoked::incrementAndGet
        ).orElseThrow();

        arbiter.clear();

        assertEquals(1, revoked.get());
        assertTrue(arbiter.ownerId().isEmpty());
        assertFalse(arbiter.dispatch(0.0, -1.0));
    }
}
