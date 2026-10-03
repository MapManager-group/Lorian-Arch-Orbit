package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.davidblackcn.lorianarchorbit.config.PalettePreset;
import com.davidblackcn.lorianarchorbit.palette.BuiltinPalettePresets;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.BlockItem;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HueBlocksPresetTest {
    @Test
    void mappingsOnlyExposeRealBlockItemsAndExcludeSpecialStateTextures() throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        try (var input = getClass().getResourceAsStream("/assets/lorian_arch_orbit/hueblocks/texture_blocks.json")) {
            assertNotNull(input);
            var mapping = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertTrue(mapping.size() > 300);
            assertFalse(mapping.has("copper_bulb_lit.png"));
            assertTrue(mapping.getAsJsonArray("ancient_debris_side.png").asList().stream()
                    .anyMatch(value -> value.getAsString().equals("minecraft:ancient_debris")));
            for (var entry : mapping.entrySet()) {
                for (var value : entry.getValue().getAsJsonArray()) {
                    Identifier id = Identifier.parse(value.getAsString());
                    assertTrue(BuiltInRegistries.ITEM.containsKey(id), id.toString());
                    assertInstanceOf(BlockItem.class, BuiltInRegistries.ITEM.getValue(id), id.toString());
                }
            }
        }
    }

    @Test
    void optimizedGroupsPreserveMembershipMissingSlotsAndImproveKnownColorTransitions() throws Exception {
        Path root = Path.of(System.getProperty("user.dir"));
        while (root != null && !Files.exists(root.resolve("docs/HUEBLOCKS_PRESET_REPORT.json"))) root = root.getParent();
        assertNotNull(root);
        var report = JsonParser.parseString(Files.readString(root.resolve("docs/HUEBLOCKS_PRESET_REPORT.json"))).getAsJsonObject();
        var groups = BuiltinPalettePresets.groups(PalettePreset.COLOR_CATEGORIES);
        assertEquals(8, report.getAsJsonArray("groups").size());
        for (var entry : report.getAsJsonArray("groups")) {
            var object = entry.getAsJsonObject();
            String id = object.get("id").getAsString();
            List<String> original = object.getAsJsonArray("original_members").asList().stream().map(v -> v.getAsString()).toList();
            List<String> optimized = groups.stream().filter(g -> g.id().equals(id)).findFirst().orElseThrow()
                    .members().stream().map(m -> m.itemId()).toList();
            assertEquals(original.stream().sorted().toList(), optimized.stream().sorted().toList(), id);
            assertEquals(object.getAsJsonArray("optimized_members").asList().stream().map(v -> v.getAsString()).toList(), optimized);
            for (var missing : object.getAsJsonArray("missing")) {
                assertEquals(original.indexOf(missing.getAsString()), optimized.indexOf(missing.getAsString()), id);
            }
            assertTrue(object.get("after").getAsDouble() <= object.get("before").getAsDouble(), id);
        }
    }
}
