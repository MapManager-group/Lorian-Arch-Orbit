package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaletteRotationDirectionConfigTest {
    @Test
    void missingAndInvalidValuesKeepLegacyDirectionAndUnknownSettings() {
        var codec = new ClientConfigCodec();
        var document = codec.encode(codec.defaults());
        var palette = document.getAsJsonObject("features").getAsJsonObject("palette_wheel");
        palette.remove("rotation_direction");
        palette.addProperty("future_setting", "preserved");
        var missing = codec.decode(document);
        assertEquals(PaletteRotationDirection.CLOCKWISE, missing.snapshot().paletteRotationDirection());
        assertFalse(missing.migrated());
        assertEquals(-1, missing.snapshot().paletteRotationDirection().selectionSteps(1));

        palette.addProperty("rotation_direction", "sideways");
        var invalid = codec.decode(document);
        assertEquals(PaletteRotationDirection.CLOCKWISE, invalid.snapshot().paletteRotationDirection());
        assertTrue(invalid.warnings().stream().anyMatch(w -> w.contains("rotation_direction")));
        assertEquals("preserved", codec.encode(invalid.snapshot()).getAsJsonObject("features")
                .getAsJsonObject("palette_wheel").get("future_setting").getAsString());
    }

    @Test
    void directionRoundTripsNotifiesOnlyPaletteAndRestoresDefault() {
        var codec = new ClientConfigCodec();
        var defaults = codec.defaults();
        var draft = new ClientConfigDraft(defaults);
        draft.setPaletteRotationDirection(PaletteRotationDirection.COUNTERCLOCKWISE);
        var encoded = codec.encode(draft.snapshot());
        assertEquals("counterclockwise", encoded.getAsJsonObject("features").getAsJsonObject("palette_wheel")
                .get("rotation_direction").getAsString());
        var restored = codec.decode(encoded).snapshot();
        assertEquals(PaletteRotationDirection.COUNTERCLOCKWISE, restored.paletteRotationDirection());
        assertEquals(Set.of("palette_wheel"), codec.changedNamespaces(defaults, restored));
        assertEquals(defaults.paletteAnimation(), restored.paletteAnimation());
        assertEquals(defaults.paletteTargetPosition(), restored.paletteTargetPosition());
        assertEquals(defaults.smartPickMode(), restored.smartPickMode());
        draft.restoreDefaults();
        assertEquals(defaults, draft.snapshot());
    }
}
