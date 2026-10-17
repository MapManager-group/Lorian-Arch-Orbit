package com.davidblackcn.lorianarchorbit.palette;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteLookupTest {
    @Test
    void exactRulesWinBeforeEarlierItemOnlyRules() {
        JsonObject components = new JsonObject();
        components.addProperty("custom", "named");
        PaletteGroup broad = group("broad", new PaletteMember("minecraft:stone"));
        PaletteGroup exact = group("exact",
                new PaletteMember("minecraft:stone", PaletteMatchMode.EXACT_COMPONENTS, components));

        var match = PaletteLookup.find(
                List.of(broad, exact), "minecraft:stone#named",
                (member, candidate) -> candidate.startsWith(member.itemId()),
                (member, candidate) -> candidate.endsWith("#named")
        );
        assertEquals("exact", match.orElseThrow().group().id());
    }

    @Test
    void fileOrderBreaksDuplicateTiesDeterministically() {
        var match = PaletteLookup.find(
                List.of(group("first", new PaletteMember("minecraft:stone")),
                        group("second", new PaletteMember("minecraft:stone"))),
                "minecraft:stone",
                (member, candidate) -> member.itemId().equals(candidate),
                (member, candidate) -> false
        );
        assertEquals("first", match.orElseThrow().group().id());
    }

    @Test
    void unmatchedCandidateDoesNotClaimAGroup() {
        assertTrue(PaletteLookup.find(
                List.of(group("stone", new PaletteMember("minecraft:stone"))), "minecraft:dirt",
                (member, candidate) -> member.itemId().equals(candidate),
                (member, candidate) -> false
        ).isEmpty());
    }

    private static PaletteGroup group(String id, PaletteMember... members) {
        return new PaletteGroup(id, id, members[0].itemId(), List.of(members));
    }
}
