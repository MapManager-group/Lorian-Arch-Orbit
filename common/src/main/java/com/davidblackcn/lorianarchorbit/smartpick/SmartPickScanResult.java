package com.davidblackcn.lorianarchorbit.smartpick;

import java.util.List;

public record SmartPickScanResult<T>(List<SmartPickCandidate<T>> candidates, SmartPickScanStats stats) {
    public SmartPickScanResult {
        candidates = List.copyOf(candidates);
    }
}
