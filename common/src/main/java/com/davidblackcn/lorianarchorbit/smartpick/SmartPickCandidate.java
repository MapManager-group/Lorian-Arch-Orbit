package com.davidblackcn.lorianarchorbit.smartpick;

public record SmartPickCandidate<T>(
        T value,
        String registryId,
        int occurrences,
        int nearestDistanceSquared,
        int score,
        boolean center
) {
}
