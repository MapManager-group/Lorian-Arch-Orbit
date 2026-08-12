package com.davidblackcn.lorianarchorbit.config;

import com.davidblackcn.lorianarchorbit.palette.PaletteGroup;
import com.davidblackcn.lorianarchorbit.palette.PaletteMatchMode;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WheelConfigCodecTest {
    private final WheelConfigCodec codec = new WheelConfigCodec();

    @Test
    void emptyDefaultsRoundTrip() {
        DecodedConfig<WheelConfigSnapshot> decoded = codec.decode(codec.encode(codec.defaults()));
        assertTrue(decoded.snapshot().typedGroups().isEmpty());
    }

    @Test
    void preservesArbitraryMemberCountsWithoutCapacityWarnings() {
        List<PaletteMember> members = new ArrayList<>();
        for (int index = 0; index < 96; index++) {
            members.add(new PaletteMember("minecraft:stone"));
        }
        WheelConfigSnapshot snapshot = codec.fromGroups(List.of(
                new PaletteGroup("stone", "Stone", "minecraft:stone", members)
        ));
        DecodedConfig<WheelConfigSnapshot> decoded = codec.decode(codec.encode(snapshot));
        assertEquals(96, decoded.snapshot().typedGroups().getFirst().members().size());
        assertTrue(decoded.warnings().isEmpty());
    }

    @Test
    void rejectsDamagedGroupShape() {
        JsonObject damaged = codec.encode(codec.defaults());
        damaged.getAsJsonArray("groups").add("not-an-object");
        assertThrows(ConfigException.class, () -> codec.decode(damaged));
    }

    @Test
    void exactRuleRequiresComponents() {
        JsonObject damaged = codec.encode(codec.fromGroups(List.of(
                new PaletteGroup("a", "A", "minecraft:stone", List.of(new PaletteMember("minecraft:stone")))
        )));
        damaged.getAsJsonArray("groups").get(0).getAsJsonObject()
                .getAsJsonArray("members").get(0).getAsJsonObject()
                .addProperty("match", PaletteMatchMode.EXACT_COMPONENTS.serializedName());
        assertThrows(ConfigException.class, () -> codec.decode(damaged));
    }

    @Test
    void editorRewritePreservesUnknownRootAndGroupFields() {
        JsonObject source = codec.encode(codec.fromGroups(List.of(
                new PaletteGroup("a", "A", "minecraft:stone", List.of())
        )));
        source.addProperty("future_root", 7);
        source.getAsJsonArray("groups").get(0).getAsJsonObject().addProperty("future_group", true);
        WheelConfigSnapshot loaded = codec.decode(source).snapshot();
        JsonObject rewritten = codec.encode(codec.fromGroups(loaded, loaded.typedGroups()));
        assertEquals(7, rewritten.get("future_root").getAsInt());
        assertTrue(rewritten.getAsJsonArray("groups").get(0).getAsJsonObject()
                .get("future_group").getAsBoolean());
    }

    @Test
    void customGroupsTakePriorityAndDeletionFallsBackToBuiltin() {
        PaletteGroup builtin = new PaletteGroup(
                "builtin_oak", "Built-in Oak", "minecraft:oak_log",
                List.of(new PaletteMember("minecraft:oak_log"), new PaletteMember("minecraft:oak_planks"))
        );
        PaletteGroup custom = new PaletteGroup(
                "group1", "Custom Oak", "minecraft:oak_log",
                List.of(new PaletteMember("minecraft:oak_log"), new PaletteMember("minecraft:oak_fence"))
        );
        WheelConfigCodec layered = new WheelConfigCodec(() -> List.of(builtin));

        WheelConfigSnapshot withOverride = layered.fromGroups(List.of(custom, builtin));
        assertEquals(List.of(custom), withOverride.overrideGroups());
        assertEquals(List.of(custom, builtin), withOverride.typedGroups());
        assertSame(custom, withOverride.typedGroups().getFirst());

        WheelConfigSnapshot afterDeletion = layered.fromGroups(withOverride, List.of(builtin));
        assertTrue(afterDeletion.overrideGroups().isEmpty());
        assertEquals(List.of(builtin), layered.decode(layered.encode(afterDeletion)).snapshot().typedGroups());
    }
}
