package com.davidblackcn.lorianarchorbit.client.invisible;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class InvisibleBlocksRuntimeTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void recognizesOnlySupportedInvisibleBlocks() {
        assertEquals(InvisibleBlockKind.BARRIER,
                InvisibleBlocksRuntime.kind(Blocks.BARRIER.defaultBlockState()));
        assertEquals(InvisibleBlockKind.LIGHT,
                InvisibleBlocksRuntime.kind(Blocks.LIGHT.defaultBlockState()));
        assertNull(InvisibleBlocksRuntime.kind(Blocks.STRUCTURE_VOID.defaultBlockState()));
        assertNull(InvisibleBlocksRuntime.kind(Blocks.AIR.defaultBlockState()));
        assertTrue(InvisibleBlocksRuntime.isSupportedState(Blocks.BARRIER.defaultBlockState()));
        assertTrue(InvisibleBlocksRuntime.isSupportedState(Blocks.LIGHT.defaultBlockState()));
        assertFalse(InvisibleBlocksRuntime.isSupportedState(Blocks.STRUCTURE_VOID.defaultBlockState()));
    }

    @Test
    void everySupportedBlockStateUsesItsMatchingMarkerTexture() throws Exception {
        assertMarkerModel("barrier", "lorian_arch_orbit:block/barrier", "minecraft:block/red_stained_glass");
        for (int level = 0; level <= 15; level++) {
            String model = "light_%02d".formatted(level);
            assertMarkerModel(model, "lorian_arch_orbit:block/" + model, "minecraft:block/yellow_stained_glass");
        }
    }

    @Test
    void everyMarkerTextureIsAddedToTheBlockAtlas() throws Exception {
        Set<String> expectedResources = new HashSet<>();
        Set<String> expectedSprites = new HashSet<>();
        expectedResources.add("minecraft:item/barrier");
        expectedSprites.add("lorian_arch_orbit:block/barrier");
        for (int level = 0; level <= 15; level++) {
            String model = "light_%02d".formatted(level);
            expectedResources.add("minecraft:item/" + model);
            expectedSprites.add("lorian_arch_orbit:block/" + model);
        }

        String resource = "assets/minecraft/atlases/blocks.json";
        boolean foundOverride = false;
        for (var url : Collections.list(InvisibleBlocksRuntimeTest.class.getClassLoader().getResources(resource))) {
            try (var stream = url.openStream()) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                Set<String> resources = new HashSet<>();
                Set<String> sprites = new HashSet<>();
                root.getAsJsonArray("sources").forEach(source -> {
                    JsonObject definition = source.getAsJsonObject();
                    if (definition.has("resource")) resources.add(definition.get("resource").getAsString());
                    if (definition.has("sprite")) sprites.add(definition.get("sprite").getAsString());
                });
                if (!resources.contains("minecraft:item/barrier")) continue;
                assertEquals(expectedResources, resources);
                assertEquals(expectedSprites, sprites);
                assertTrue(Collections.disjoint(resources, sprites));
                foundOverride = true;
            }
        }
        assertTrue(foundOverride, "Missing block-atlas marker sources");
    }

    private static void assertMarkerModel(String model, String marker, String background) throws Exception {
        String resource = "assets/minecraft/models/block/" + model + ".json";
        boolean foundOverride = false;
        for (var url : Collections.list(InvisibleBlocksRuntimeTest.class.getClassLoader().getResources(resource))) {
            try (var stream = url.openStream()) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                if (!root.has("parent") || !"lorian_arch_orbit:block/invisible_marker".equals(
                        root.get("parent").getAsString())) continue;
            JsonObject textures = root.getAsJsonObject("textures");
            assertEquals(marker, textures.get("marker").getAsString());
            JsonObject backgroundMaterial = textures.getAsJsonObject("background");
            assertEquals(background, backgroundMaterial.get("sprite").getAsString());
            assertTrue(backgroundMaterial.get("force_translucent").getAsBoolean());
            foundOverride = true;
            }
        }
        assertTrue(foundOverride, "Missing model override: " + resource);
    }
}
