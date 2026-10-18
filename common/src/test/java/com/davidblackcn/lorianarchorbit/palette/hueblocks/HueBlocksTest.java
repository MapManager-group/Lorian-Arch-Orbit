package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HueBlocksTest {
    private static final String BLOCKS = """
            ["Generated at: test", {"name":"Black","texture":"black.png","rgb":[0,0,0],
            "lab":["0","0","0"],"sides":["north","top"]},
            {"name":"White","texture":"white.png","rgb":[255,255,255],
            "lab":["1","0","0"],"sides":["top"]}]
            """;
    private static final String PALETTES = """
            [{"name":"Test","textures":["black.png","white.png"],"count":999}]
            """;

    @Test
    void publishedStringLabAndMetadataAreDecodedWithoutTrustingCounts() {
        HueBlocksData data = decode(BLOCKS);
        assertEquals(2, data.blocks().size());
        assertEquals(2, data.palettes().getFirst().textures().size());
        assertTrue(data.blocks().getFirst().faces("sides"));
        assertFalse(data.blocks().getLast().faces("sides"));
        assertThrows(UnsupportedOperationException.class, () -> data.blocks().clear());
        assertThrows(IllegalArgumentException.class, () -> decode(BLOCKS.replace("black.png", "../black.png")));
        assertThrows(IllegalArgumentException.class, () -> decode(BLOCKS.replace("\"0\",\"0\",\"0\"", "\"NaN\",\"0\",\"0\"")));
        assertThrows(IllegalArgumentException.class, () -> decode(BLOCKS.replace("[0,0,0]", "[-1,0,0]")));
        assertThrows(IllegalArgumentException.class, () -> decode(BLOCKS.replace("\"north\"", "\"wrong\"")));
    }

    @Test
    void gradientsShareStopsRespectPinnedBlocksAndHaveBoundedInputs() {
        HueBlocksData data = decode(BLOCKS);
        var black = new HueGradient.Candidate("minecraft:black_concrete", data.blocks().getFirst());
        var white = new HueGradient.Candidate("minecraft:white_concrete", data.blocks().getLast());
        var choices = List.of(black, white);
        List<HueGradient.Stop> stops = List.of(new HueGradient.Stop(0, 3, black),
                new HueGradient.Stop(0xFFFFFF, 3, white), new HueGradient.Stop(0, 2, black));
        for (boolean lab : List.of(true, false)) {
            List<HueGradient.Candidate> result = HueGradient.generate(stops, choices, lab);
            assertEquals(5, result.size()); // Shared white endpoint occurs only once.
            assertEquals(black, result.getFirst());
            assertEquals(white, result.get(2));
            assertEquals(black, result.getLast());
        }
        assertTrue(HueGradient.generate(stops, List.of(), true).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new HueGradient.Stop(0, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new HueGradient.Stop(0, 129, null));
        assertThrows(IllegalArgumentException.class, () -> HueGradient.generate(stops.subList(0, 1), choices, true));
        assertEquals(1, HueColor.fromRgb(0xFFFFFF).l(), .000001);
        assertEquals(0, HueColor.fromRgb(0).l());
        assertEquals(.5, HueColor.fromRgb(0).mix(HueColor.fromRgb(0xFFFFFF), .5).l(), .000001);
    }

    @Test
    void tenSamplesRemainTenEvenWhenOnlyOneOrTwoBlocksMatch() {
        var data = decode(BLOCKS);
        var black = new HueGradient.Candidate("minecraft:black_concrete", data.blocks().getFirst());
        var white = new HueGradient.Candidate("minecraft:white_concrete", data.blocks().getLast());
        for (boolean lab : List.of(true, false)) {
            var stops = List.of(new HueGradient.Stop(0, 10, black), new HueGradient.Stop(0xFFFFFF, 2, white));
            var result = HueGradient.generate(stops, List.of(black, white), lab);
            assertEquals(10, result.size());
            assertEquals(black, result.getFirst());
            assertEquals(white, result.getLast());
            assertTrue(result.stream().distinct().count() < result.size());
            assertEquals(10, HueGradient.generate(stops, List.of(black), lab).size());
        }
    }

    @Test
    void previewToggleOnlyHidesConsecutiveIdenticalTexturesAndCanRestoreAllSamples() {
        var data = decode(BLOCKS);
        var black = new HueGradient.Candidate("minecraft:black_concrete", data.blocks().getFirst());
        var sameItemOtherTexture = new HueGradient.Candidate(black.itemId(), data.blocks().getLast());
        var white = new HueGradient.Candidate("minecraft:white_concrete", data.blocks().getLast());
        var samples = List.of(black, black, sameItemOtherTexture, white, white, black);
        assertEquals(List.of(black, sameItemOtherTexture, white, black), HueGradient.preview(samples, true));
        assertEquals(samples, HueGradient.preview(samples, false));
        assertEquals(6, samples.size());
        assertEquals(List.of(black, sameItemOtherTexture, white, black), HueGradient.preview(samples, true));
        assertTrue(HueGradient.preview(List.of(), true).isEmpty());
        assertEquals(List.of(black), HueGradient.preview(java.util.Collections.nCopies(10, black), true));
    }

    private static HueBlocksData decode(String blocks) {
        return HueBlocksData.decode("26.2", JsonParser.parseString(blocks).getAsJsonArray(),
                JsonParser.parseString(PALETTES).getAsJsonArray());
    }

}
