package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
    void previewToggleOnlyHidesConsecutiveItemRepeatsAndCanRestoreAllSamples() {
        var data = decode(BLOCKS);
        var black = new HueGradient.Candidate("minecraft:black_concrete", data.blocks().getFirst());
        var sameItemOtherTexture = new HueGradient.Candidate(black.itemId(), data.blocks().getLast());
        var white = new HueGradient.Candidate("minecraft:white_concrete", data.blocks().getLast());
        var samples = List.of(black, black, sameItemOtherTexture, white, white, black);
        assertEquals(List.of(black, white, black), HueGradient.preview(samples, true));
        assertEquals(samples, HueGradient.preview(samples, false));
        assertEquals(6, samples.size());
        assertEquals(List.of(black, white, black), HueGradient.preview(samples, true));
        assertTrue(HueGradient.preview(List.of(), true).isEmpty());
        assertEquals(List.of(black), HueGradient.preview(java.util.Collections.nCopies(10, black), true));
    }

    @Test
    void refreshUsesConditionalRequestsAndFailedPartialUpdatesPreserveCache(@TempDir Path directory) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicBoolean corrupt = new AtomicBoolean();
        AtomicInteger unchanged = new AtomicInteger();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String etag = path.endsWith("_palettes.json") ? "\"palettes\"" : "\"blocks\"";
            String body = path.endsWith("_blocksets.json")
                    ? "[{\"name\":\"Minecraft 26.2\",\"dir\":\"Minecraft 26.2\",\"count\":999}]"
                    : path.endsWith("_palettes.json") ? (corrupt.get() ? "broken json" : PALETTES) : BLOCKS;
            exchange.getResponseHeaders().set("ETag", etag);
            if (!corrupt.get() && !path.endsWith("_blocksets.json")
                    && etag.equals(exchange.getRequestHeaders().getFirst("If-None-Match"))) {
                unchanged.incrementAndGet();
                exchange.sendResponseHeaders(304, -1);
            } else {
                byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
        URI source = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
        try {
            try (HueBlocksRepository repository = new HueBlocksRepository(directory, "26.2", source)) {
                refresh(repository);
                assertEquals(HueBlocksRepository.State.READY, repository.state());
                assertEquals(2, repository.data().blocks().size());
                Path cache = directory.resolve("hueblocks/26.2.json");
                String valid = Files.readString(cache);
                refresh(repository);
                assertEquals(2, unchanged.get());
                corrupt.set(true);
                refresh(repository);
                assertEquals(HueBlocksRepository.State.CACHED, repository.state());
                assertEquals(valid, Files.readString(cache));
                assertEquals(2, repository.data().blocks().size());
            }
            server.stop(0);
            try (HueBlocksRepository repository = new HueBlocksRepository(directory, "26.2", source)) {
                refresh(repository);
                assertEquals(HueBlocksRepository.State.CACHED, repository.state());
                assertEquals(2, repository.data().blocks().size());
            }
            Files.writeString(directory.resolve("hueblocks/26.2.json"), "broken cache");
            try (HueBlocksRepository repository = new HueBlocksRepository(directory, "26.2", source)) {
                refresh(repository);
                assertEquals(HueBlocksRepository.State.FAILED, repository.state());
                assertNull(repository.data());
            }
        } finally {
            server.stop(0);
        }
    }

    private static HueBlocksData decode(String blocks) {
        return HueBlocksData.decode("26.2", JsonParser.parseString(blocks).getAsJsonArray(),
                JsonParser.parseString(PALETTES).getAsJsonArray());
    }

    private static void refresh(HueBlocksRepository repository) throws InterruptedException {
        repository.refresh();
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (repository.checking() && System.nanoTime() < deadline) Thread.sleep(10);
        assertFalse(repository.checking(), "Refresh did not finish in five seconds");
    }
}
