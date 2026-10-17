package com.davidblackcn.lorianarchorbit.smartpick;

public record SmartPickScanStats(
        int visitedPositions,
        int loadedPositions,
        int validSamples,
        int uniqueCandidates,
        int scoredCandidates,
        long elapsedNanos
) {
}
