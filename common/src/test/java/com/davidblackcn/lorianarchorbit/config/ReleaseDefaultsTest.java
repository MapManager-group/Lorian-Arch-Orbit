package com.davidblackcn.lorianarchorbit.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReleaseDefaultsTest {
    @Test void newMissingAndResetValuesUseNewDefaultsButExplicitChoicesSurvive() {
        var codec = new ClientConfigCodec();
        var defaults = codec.defaults();
        assertEquals(PaletteAnimation.EXPAND, defaults.paletteAnimation());
        assertEquals(PalettePreset.COLOR_CATEGORIES, defaults.primaryPalettePreset());
        assertEquals(PalettePreset.ITEM_TAG_B, defaults.secondaryPalettePreset());
        var document = codec.encode(defaults);
        var wheel = document.getAsJsonObject("features").getAsJsonObject("palette_wheel");
        wheel.remove("animation"); wheel.remove("primary_default_preset");
        var missing = codec.decode(document).snapshot();
        assertEquals(PaletteAnimation.EXPAND, missing.paletteAnimation());
        assertEquals(PalettePreset.COLOR_CATEGORIES, missing.primaryPalettePreset());
        wheel.addProperty("animation", "clockwise"); wheel.addProperty("primary_default_preset", "item_tag_a");
        var restored = codec.decode(document).snapshot();
        assertEquals(PaletteAnimation.CLOCKWISE, restored.paletteAnimation());
        assertEquals(PalettePreset.ITEM_TAG_A, restored.primaryPalettePreset());
        var draft = new ClientConfigDraft(restored); draft.restoreDefaults();
        assertEquals(defaults, draft.snapshot());
    }
}
