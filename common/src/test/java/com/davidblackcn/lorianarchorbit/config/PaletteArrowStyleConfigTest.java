package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PaletteArrowStyleConfigTest {
    @Test
    void missingAndInvalidStylesUseShiftAndPreserveUnrelatedFields() {
        var codec = new ClientConfigCodec();
        assertEquals(PaletteArrowStyle.SHIFT, codec.defaults().paletteArrowStyle());
        var document = codec.encode(codec.defaults());
        var palette = document.getAsJsonObject("features").getAsJsonObject("palette_wheel");
        palette.remove("arrow_style");
        palette.addProperty("target_position", "left");
        palette.addProperty("future_option", "preserved");
        assertEquals(PaletteArrowStyle.SHIFT, codec.decode(document).snapshot().paletteArrowStyle());
        palette.addProperty("arrow_style", "unknown");
        var decoded = codec.decode(document);
        assertEquals(PaletteArrowStyle.SHIFT, decoded.snapshot().paletteArrowStyle());
        assertEquals(PaletteTargetPosition.LEFT, decoded.snapshot().paletteTargetPosition());
        assertTrue(decoded.warnings().stream().anyMatch(w -> w.contains("arrow_style")));
        assertEquals("preserved", codec.encode(decoded.snapshot()).getAsJsonObject("features")
                .getAsJsonObject("palette_wheel").get("future_option").getAsString());
    }

    @Test
    void stylesRoundTripIndependentlyOfDirectionAndNotifyOnlyPalette() {
        var codec = new ClientConfigCodec();
        for (var position : PaletteTargetPosition.values()) {
            var draft = new ClientConfigDraft(codec.defaults());
            draft.setPaletteTargetPosition(position);
            var before = draft.snapshot();
            for (var style : PaletteArrowStyle.values()) {
                draft.setPaletteArrowStyle(style);
                var encoded = codec.encode(draft.snapshot());
                assertEquals(style.name().toLowerCase(java.util.Locale.ROOT), encoded.getAsJsonObject("features")
                        .getAsJsonObject("palette_wheel").get("arrow_style").getAsString());
                var restored = codec.decode(encoded).snapshot();
                assertEquals(style, restored.paletteArrowStyle());
                assertEquals(position, restored.paletteTargetPosition());
                assertEquals(style == PaletteArrowStyle.SHIFT ? Set.of() : Set.of("palette_wheel"),
                        codec.changedNamespaces(before, restored));
                assertEquals(before.paletteAnimation(), restored.paletteAnimation());
            }
            draft.setPaletteArrowStyle(PaletteArrowStyle.POINTER);
            draft.restoreDefaults();
            assertEquals(PaletteArrowStyle.SHIFT, draft.paletteArrowStyle());
        }
    }
}
