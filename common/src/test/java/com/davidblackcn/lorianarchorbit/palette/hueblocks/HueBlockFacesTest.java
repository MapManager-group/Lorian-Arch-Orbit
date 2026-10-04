package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class HueBlockFacesTest {
    private JsonElement entries(String item) throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/lorian_arch_orbit/hueblocks/block_faces.json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().get("minecraft:" + item);
        }
    }
    private HueBlockFaces.Surface texture(List<HueBlockFaces.Surface> surfaces, String name) {
        return surfaces.stream().filter(s -> s.texture().equals(name)).findFirst().orElseThrow();
    }
    @Test void unqualifiedModelAndTextureReferencesAreIncluded() throws Exception {
        var kelp = HueBlockFaces.resolve(entries("dried_kelp_block"), Map.of(), Map.of());
        assertEquals(Set.of("top"), texture(kelp, "dried_kelp_top.png").faces());
        assertEquals(Set.of("bottom"), texture(kelp, "dried_kelp_bottom.png").faces());
        assertEquals(Set.of("north", "south", "east", "west"), texture(kelp, "dried_kelp_side.png").faces());
    }
    @Test void sandstoneFaceMetadataIsSpecificToItemInsteadOfGlobalTexture() throws Exception {
        var sandstone = HueBlockFaces.resolve(entries("sandstone"), Map.of(), Map.of());
        var smooth = HueBlockFaces.resolve(entries("smooth_sandstone"), Map.of(), Map.of());
        assertEquals(Set.of("top"), texture(sandstone, "sandstone_top.png").faces());
        assertEquals(Set.copyOf(HueBlockFaces.DIRECTIONS), texture(smooth, "sandstone_top.png").faces());
    }
    @Test void indirectBeehiveModelsResolveWithoutTakingHoneyState() throws Exception {
        var surfaces = HueBlockFaces.resolve(entries("beehive"), Map.of("facing", "north", "honey_level", "0"),
                Map.of("facing", List.of("north", "south", "east", "west")));
        assertEquals(Set.of("top", "bottom"), texture(surfaces, "beehive_end.png").faces());
        assertEquals(Set.of("north"), texture(surfaces, "beehive_front.png").defaultFaces());
        assertTrue(surfaces.stream().noneMatch(s -> s.texture().contains("honey")));
    }
    @Test void observerMayRotateButNeverBecomesPowered() throws Exception {
        var surfaces = HueBlockFaces.resolve(entries("observer"), Map.of("facing", "north", "powered", "false"),
                Map.of("facing", HueBlockFaces.DIRECTIONS.stream().map(f -> f.equals("top") ? "up" : f.equals("bottom") ? "down" : f).toList()));
        var front = texture(surfaces, "observer_front.png");
        assertEquals(Set.copyOf(HueBlockFaces.DIRECTIONS), front.faces());
        assertEquals("facing=east", front.placements().get("east"));
        assertEquals("facing=up", front.placements().get("top"));
        assertTrue(surfaces.stream().noneMatch(s -> s.texture().equals("observer_back_on.png")));
    }
    @Test void axisPermutationsRetainDefaultFaceAndPlacementInstructions() throws Exception {
        var log = HueBlockFaces.resolve(entries("oak_log"), Map.of("axis", "y"), Map.of("axis", List.of("x", "y", "z")));
        var end = texture(log, "oak_log_top.png");
        assertEquals(Set.of("top", "bottom"), end.defaultFaces());
        assertEquals("axis=z", end.placements().get("north"));
        assertEquals("axis=x", end.placements().get("east"));
    }
    @Test void multipartConditionsRespectDefaultsAndAlternatives() {
        var predicate = JsonParser.parseString("{\"AND\":[{\"OR\":[{\"north\":\"true\"},{\"east\":\"true\"}]},{\"waterlogged\":\"false\"},{\"axis\":\"x|z\"}]}").getAsJsonObject();
        assertTrue(HueBlockFaces.matches(predicate, Map.of("north", "false", "east", "true", "waterlogged", "false", "axis", "z")));
        assertFalse(HueBlockFaces.matches(predicate, Map.of("north", "false", "east", "true", "waterlogged", "true", "axis", "z")));
    }
    @Test void rotationAllowlistCannotEnableLitState() throws Exception {
        var surfaces = HueBlockFaces.resolve(entries("redstone_lamp"), Map.of("lit", "false"), Map.of("lit", List.of("false", "true")));
        assertEquals(List.of("redstone_lamp.png"), surfaces.stream().map(HueBlockFaces.Surface::texture).toList());
    }
}
