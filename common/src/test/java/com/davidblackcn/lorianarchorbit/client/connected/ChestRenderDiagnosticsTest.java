package com.davidblackcn.lorianarchorbit.client.connected;

import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ChestRenderDiagnosticsTest {
    @AfterEach
    void restoreDiagnosticState() {
        ChestRenderDiagnostics.setForceSealed(false);
        ChestRenderDiagnostics.resetCounters();
    }

    @Test
    public void reportTracksRedirectsRuntimeGatingAndReturnedModels() {
        ChestRenderDiagnostics.resetCounters();
        ChestRenderDiagnostics.onRedirect(ChestType.SINGLE);
        ChestRenderDiagnostics.recordRuntimeEnabled(ChestType.SINGLE, true);
        ChestRenderDiagnostics.recordVanillaReturn(ChestType.SINGLE, true);
        ChestRenderDiagnostics.onRedirect(ChestType.LEFT);
        ChestRenderDiagnostics.recordRuntimeEnabled(ChestType.LEFT, true);
        ChestRenderDiagnostics.recordSealedReturn(ChestType.LEFT, true);
        ChestRenderDiagnostics.onRedirect(ChestType.RIGHT);
        ChestRenderDiagnostics.recordRuntimeEnabled(ChestType.RIGHT, false);
        ChestRenderDiagnostics.recordVanillaReturn(ChestType.RIGHT, false);

        String report = ChestRenderDiagnostics.statusReport();
        assertTrue(report.contains("build: chest-production-diagnosis-1"));
        assertTrue(report.contains("redirectHits: 3"));
        assertTrue(report.contains("vanillaReturns: 2"));
        assertTrue(report.contains("SINGLE:\nseen: 1\nvanilla: 1"));
        assertTrue(report.contains("LEFT:\nseen: 1\nvanilla: 0\nsealed: 1"));
        assertTrue(report.contains("RIGHT:\nseen: 1\nvanilla: 1\nsealed: 0"));
        assertTrue(report.contains("lastType: RIGHT"));
        assertTrue(report.contains("lastRuntimeEnabled: false"));
    }

    @Test
    public void resetClearsOnlyCountersAndRetainsForceState() {
        ChestRenderDiagnostics.setForceSealed(true);
        ChestRenderDiagnostics.onRedirect(ChestType.LEFT);
        ChestRenderDiagnostics.recordRuntimeEnabled(ChestType.LEFT, false);
        ChestRenderDiagnostics.recordSealedReturn(ChestType.LEFT, false);

        ChestRenderDiagnostics.resetCounters();

        String report = ChestRenderDiagnostics.statusReport();
        assertTrue(ChestRenderDiagnostics.forceSealed());
        assertTrue(report.contains("forceSealed: true"));
        assertTrue(report.contains("redirectHits: 0"));
        assertTrue(report.contains("LEFT:\nseen: 0\nvanilla: 0\nsealed: 0"));
        assertFalse(report.contains("lastType: NONE"));
    }
}
