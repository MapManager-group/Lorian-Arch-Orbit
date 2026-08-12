package com.davidblackcn.lorianarchorbit.reach;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ReachProtocolTest {
    @Test
    void clampsIllegalAndBoundaryDistancesAgainstServerMaximum() {
        assertEquals(5, ReachProtocol.clamp(Integer.MIN_VALUE, 128));
        assertEquals(5, ReachProtocol.clamp(5, 128));
        assertEquals(64, ReachProtocol.clamp(64, 128));
        assertEquals(128, ReachProtocol.clamp(Integer.MAX_VALUE, 128));
        assertEquals(64, ReachProtocol.clamp(128, 64));
    }

    @Test
    void packetCodecsRoundTripProtocolAndDecisionFields() {
        assertEquals(new ReachPackets.Hello(1), roundTrip(
                new ReachPackets.Hello(1), ReachPackets.Hello.CODEC));
        assertEquals(new ReachPackets.Capabilities(1, true, 96, 8, ReachDecision.ACCEPTED), roundTrip(
                new ReachPackets.Capabilities(1, true, 96, 8, ReachDecision.ACCEPTED),
                ReachPackets.Capabilities.CODEC));
        assertEquals(new ReachPackets.SetDistance(1, 128), roundTrip(
                new ReachPackets.SetDistance(1, 128), ReachPackets.SetDistance.CODEC));
        assertEquals(new ReachPackets.Result(1, false, 5, ReachDecision.NO_PERMISSION), roundTrip(
                new ReachPackets.Result(1, false, 5, ReachDecision.NO_PERMISSION), ReachPackets.Result.CODEC));
        assertEquals(ReachDecision.INCOMPATIBLE, ReachDecision.byId(999));
    }

    @Test
    void rateLimiterUsesIndependentOneSecondPlayerWindows() {
        ReachRateLimiter limiter = new ReachRateLimiter();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(limiter.allow(first, 1_000, 2));
        assertTrue(limiter.allow(first, 1_100, 2));
        assertFalse(limiter.allow(first, 1_200, 2));
        assertTrue(limiter.allow(second, 1_200, 2));
        assertTrue(limiter.allow(first, 2_000, 2));
        limiter.remove(first);
        assertTrue(limiter.allow(first, 2_001, 2));
    }

    private static <T> T roundTrip(T value, net.minecraft.network.codec.StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        codec.encode(buffer, value);
        return codec.decode(buffer);
    }
}
