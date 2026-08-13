package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ClientConfigDraftTest {
    private static final System.Logger LOGGER = System.getLogger("lorian_arch_orbit.draft.test");

    @TempDir
    Path temporaryDirectory;

    @Test
    public void editingAndCancellingDraftDoesNotChangeFileOrRuntimeSnapshot() throws Exception {
        ClientConfigManager manager = new ClientConfigManager(temporaryDirectory, LOGGER);
        manager.load();
        ClientConfigSnapshot runtime = manager.client();
        byte[] file = Files.readAllBytes(temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE));
        ClientConfigDraft draft = new ClientConfigDraft(runtime);

        draft.setFeatureEnabled("smart_pick", false);
        draft.setReachDistance(64);

        assertEquals(runtime, manager.client());
        assertTrue(java.util.Arrays.equals(file, Files.readAllBytes(
                temporaryDirectory.resolve(ConfigConstants.CLIENT_FILE)
        )));
    }

    @Test
    public void saveAppliesDraftAndRestoreDefaultsIsExplicit() {
        ClientConfigManager manager = new ClientConfigManager(temporaryDirectory, LOGGER);
        manager.load();
        ClientConfigDraft draft = new ClientConfigDraft(manager.client());
        draft.setFeatureEnabled("palette_wheel", false);
        draft.setReachDistance(64);
        draft.setPrimaryPalettePreset(PalettePreset.COLOR_CATEGORIES);
        draft.setSecondaryPalettePreset(PalettePreset.ITEM_TAG_A);
        draft.setSmartPickDebugStats(true);
        draft.setInvisibleBlocksVisible(true);
        draft.setShowLightBlocks(false);

        assertTrue(manager.save(draft).successful());
        assertFalse(manager.client().featureEnabled("palette_wheel"));
        assertEquals(64, manager.client().reachDistance());
        assertEquals(PalettePreset.COLOR_CATEGORIES, manager.client().primaryPalettePreset());
        assertEquals(PalettePreset.ITEM_TAG_A, manager.client().secondaryPalettePreset());
        assertTrue(manager.client().smartPickDebugStats());
        assertTrue(manager.client().invisibleBlocksVisible());
        assertTrue(manager.client().showBarriers());
        assertFalse(manager.client().showLightBlocks());

        ClientConfigDraft reset = new ClientConfigDraft(manager.client());
        reset.restoreDefaults();
        assertTrue(manager.save(reset).successful());
        assertTrue(manager.client().featureEnabled("palette_wheel"));
        assertEquals(5, manager.client().reachDistance());
        assertEquals(PalettePreset.ITEM_TAG_A, manager.client().primaryPalettePreset());
        assertEquals(PalettePreset.ITEM_TAG_B, manager.client().secondaryPalettePreset());
        assertFalse(manager.client().smartPickDebugStats());
        assertFalse(manager.client().invisibleBlocksVisible());
        assertTrue(manager.client().showBarriers());
        assertTrue(manager.client().showLightBlocks());
    }
}
