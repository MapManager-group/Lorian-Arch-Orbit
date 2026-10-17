package com.davidblackcn.lorianarchorbit.smartpick;

import com.davidblackcn.lorianarchorbit.config.SmartPickMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SmartPickScannerTest {
    private static final SmartPickDirection FRONT = new SmartPickDirection(0, 0, 1);

    @Test
    void adjacentModeVisitsExactlySixFacesAndTwelveEdges() {
        AtomicInteger calls = new AtomicInteger();
        SmartPickScanResult<String> result = scan(
                SmartPickMode.ADJACENT, 3, 12,
                (x, y, z) -> {
                    calls.incrementAndGet();
                    return SmartPickSample.value(x == 0 && y == 0 && z == 0 ? "center" : "neighbor");
                }
        );

        assertEquals(19, calls.get());
        assertEquals(18, result.stats().visitedPositions());
        assertEquals(18, SmartPickScanner.adjacentOffsets().size());
        assertEquals("center", result.candidates().getFirst().value());
        assertEquals(18, result.candidates().get(1).occurrences());
    }

    @Test
    void rangeModeNeverExceedsTheConfiguredCubeAndKeepsEveryUniqueCandidate() {
        AtomicInteger calls = new AtomicInteger();
        SmartPickScanResult<String> result = scan(
                SmartPickMode.RANGE, 3, 1,
                (x, y, z) -> {
                    calls.incrementAndGet();
                    assertTrue(Math.abs(x) <= 3 && Math.abs(y) <= 3 && Math.abs(z) <= 3);
                    return SmartPickSample.value(x + ":" + y + ":" + z);
                }
        );

        assertEquals(343, calls.get());
        assertEquals(343, result.stats().visitedPositions());
        assertEquals(343, result.candidates().size());
        assertEquals("0:0:0", result.candidates().getFirst().value());
    }

    @Test
    void contextModeRanksWallThenDecorationBeforeBehindFillAndLimitsOutput() {
        Map<Position, String> fixture = new HashMap<>();
        fixture.put(new Position(0, 0, 0), "target");
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                if (x != 0 || y != 0) fixture.put(new Position(x, y, 0), "wall");
            }
        }
        fixture.put(new Position(1, 0, 1), "decoration");
        fixture.put(new Position(0, 0, -1), "fill");
        fixture.put(new Position(1, 0, -1), "fill");

        SmartPickScanResult<String> result = scan(
                SmartPickMode.CONTEXT, 3, 4,
                (x, y, z) -> value(fixture.get(new Position(x, y, z)))
        );

        assertEquals(List.of("target", "wall", "decoration", "fill"),
                result.candidates().stream().map(SmartPickCandidate::value).toList());
        assertEquals(4, result.stats().scoredCandidates());
    }

    @Test
    void unloadedAndEmptyPositionsAreExcludedWhileHistoryBreaksOtherwiseEqualScores() {
        SmartPickScanResult<String> result = SmartPickScanner.scan(
                SmartPickMode.CONTEXT, 1, 12, FRONT,
                (x, y, z) -> {
                    if (x < 0) return SmartPickSample.unloaded();
                    if (x == 0 && y == 0 && z == 0) return SmartPickSample.value("center");
                    if (x == 1 && y == 0 && z == 0) return SmartPickSample.value("recent");
                    if (x == 0 && y == 1 && z == 0) return SmartPickSample.value("other");
                    return SmartPickSample.empty();
                },
                value -> value,
                value -> "minecraft:" + value,
                value -> value.equals("recent") ? 1 : 0
        );

        assertEquals(List.of("center", "recent", "other"),
                result.candidates().stream().map(SmartPickCandidate::value).toList());
        assertTrue(result.stats().loadedPositions() < result.stats().visitedPositions());
    }

    @Test
    void componentAwareIdentityKeepsDistinctStacksAndMergesExactMatches() {
        FixtureItem center = new FixtureItem("minecraft:stone", "center");
        FixtureItem named = new FixtureItem("minecraft:stone", "named");
        FixtureItem plain = new FixtureItem("minecraft:stone", "plain");
        SmartPickScanResult<FixtureItem> result = SmartPickScanner.scan(
                SmartPickMode.RANGE, 1, 12, FRONT,
                (x, y, z) -> {
                    if (x == 0 && y == 0 && z == 0) return SmartPickSample.value(center);
                    if ((x == 1 && y == 0 && z == 0) || (x == 0 && y == 1 && z == 0)) {
                        return SmartPickSample.value(named);
                    }
                    if (x == -1 && y == 0 && z == 0) return SmartPickSample.value(plain);
                    return SmartPickSample.empty();
                },
                value -> value,
                FixtureItem::registryId,
                value -> 0
        );

        assertEquals(3, result.candidates().size());
        assertEquals(2, result.candidates().stream()
                .filter(candidate -> candidate.value().equals(named)).findFirst().orElseThrow().occurrences());
    }

    @Test
    void exactTiesUseRegistryIdForDeterministicOrdering() {
        SmartPickScanResult<String> result = scan(
                SmartPickMode.CONTEXT, 1, 12,
                (x, y, z) -> {
                    if (x == 0 && y == 0 && z == 0) return SmartPickSample.value("center");
                    if (x == 1 && y == 0 && z == 0) return SmartPickSample.value("zeta");
                    if (x == -1 && y == 0 && z == 0) return SmartPickSample.value("alpha");
                    return SmartPickSample.empty();
                }
        );

        assertEquals(List.of("center", "alpha", "zeta"),
                result.candidates().stream().map(SmartPickCandidate::value).toList());
    }

    private static SmartPickScanResult<String> scan(
            SmartPickMode mode,
            int radius,
            int limit,
            SmartPickSampleSource<String> source
    ) {
        return SmartPickScanner.scan(mode, radius, limit, FRONT, source,
                value -> value, value -> "minecraft:" + value, value -> 0);
    }

    private static SmartPickSample<String> value(String value) {
        return value == null ? SmartPickSample.empty() : SmartPickSample.value(value);
    }

    private record Position(int x, int y, int z) {
    }

    private record FixtureItem(String registryId, String component) {
    }
}
