package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaletteTargetPositionConfigTest {
    @Test
    void oldAndInvalidSettingsFallBackWithoutLosingOtherSettings() {
        var codec = new ClientConfigCodec();
        var old = codec.encode(codec.defaults());
        var palette = old.getAsJsonObject("features").getAsJsonObject("palette_wheel");
        palette.remove("target_position");
        palette.addProperty("future_option", "preserved");
        assertEquals(PaletteTargetPosition.BOTTOM, codec.decode(old).snapshot().paletteTargetPosition());
        palette.addProperty("target_position", "diagonal");
        var invalid = codec.decode(old);
        assertEquals(PaletteTargetPosition.BOTTOM, invalid.snapshot().paletteTargetPosition());
        assertTrue(invalid.warnings().stream().anyMatch(w -> w.contains("target_position")));
        assertEquals("preserved", codec.encode(invalid.snapshot()).getAsJsonObject("features")
                .getAsJsonObject("palette_wheel").get("future_option").getAsString());
    }

    @Test
    void allDirectionsRoundTripNotifyPaletteAndResetToBottom() {
        var codec = new ClientConfigCodec();
        var defaults = codec.defaults();
        for (var position : PaletteTargetPosition.values()) {
            var draft = new ClientConfigDraft(defaults);
            draft.setPaletteTargetPosition(position);
            var restored = codec.decode(codec.encode(draft.snapshot())).snapshot();
            assertEquals(position, restored.paletteTargetPosition());
            assertEquals(position != PaletteTargetPosition.BOTTOM,
                    codec.changedNamespaces(defaults, restored).contains("palette_wheel"));
            assertEquals(defaults.smartPickMode(), restored.smartPickMode());
            draft.restoreDefaults();
            assertEquals(PaletteTargetPosition.BOTTOM, draft.paletteTargetPosition());
        }
    }
}
