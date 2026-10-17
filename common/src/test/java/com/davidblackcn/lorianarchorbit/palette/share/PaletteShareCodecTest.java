package com.davidblackcn.lorianarchorbit.palette.share;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PaletteShareCodecTest {
    private final PaletteShareCodec codec = new PaletteShareCodec();

    @Test
    void jsonAndShareCodeRoundTripBothLayersAndExactComponents() throws Exception {
        JsonObject components = new JsonObject();
        components.addProperty("minecraft:custom_name", "A named block");
        PaletteShareBundle bundle = new PaletteShareBundle("Builder pack", List.of(
                new PaletteShareEntry(PaletteShareLayer.PRIMARY, group("stone", new PaletteMember("minecraft:stone"))),
                new PaletteShareEntry(PaletteShareLayer.SECONDARY, group("named", new PaletteMember(
                        "minecraft:oak_log", PaletteMatchMode.EXACT_COMPONENTS, components
                )))
        ));

        assertEquals(bundle, codec.decode(codec.encodeJson(bundle)));
        String code = codec.encodeCode(bundle);
        assertTrue(code.startsWith(PaletteShareCodec.CODE_PREFIX));
        assertEquals(bundle, codec.decode(code));
    }

    @Test
    void rejectsUnknownVersionsDuplicateLayerIdsAndInvalidCompressedData() {
        String unknown = """
                {"format":"lorian_arch_orbit_palette","version":2,"name":"x","groups":[]}
                """;
        assertThrows(PaletteShareException.class, () -> codec.decode(unknown));

        String duplicate = """
                {
                  "format":"lorian_arch_orbit_palette","version":1,"name":"x","groups":[
                    {"layer":"primary","id":"same","display_name":"A","icon":"minecraft:stone","members":[]},
                    {"layer":"primary","id":"same","display_name":"B","icon":"minecraft:dirt","members":[]}
                  ]
                }
                """;
        assertThrows(PaletteShareException.class, () -> codec.decode(duplicate));
        assertThrows(PaletteShareException.class,
                () -> codec.decode(PaletteShareCodec.CODE_PREFIX + "not-compressed"));

        String invalidItem = """
                {
                  "format":"lorian_arch_orbit_palette","version":1,"name":"x","groups":[
                    {"layer":"primary","id":"bad","display_name":"Bad","icon":"not valid","members":[]}
                  ]
                }
                """;
        assertThrows(PaletteShareException.class, () -> codec.decode(invalidItem));
    }

    private static PaletteGroup group(String id, PaletteMember member) {
        return new PaletteGroup(id, id, member.itemId(), List.of(member));
    }
}
