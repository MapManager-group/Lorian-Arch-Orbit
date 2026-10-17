package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class HueFaceSelectionTest {
    private static HueGradient.Candidate candidate(String id, String texture) {
        return new HueGradient.Candidate(id, new HueBlocksData.Block(texture, 0xAABBCC,
                HueColor.fromRgb(0xAABBCC), Set.of("top", "north")));
    }
    private static final HueGradient.Candidate SIDE = candidate("minecraft:oak_log", "oak_log.png");
    private static final HueGradient.Candidate END = candidate("minecraft:oak_log", "oak_log_top.png");
    private static final List<HueBlockFaces.Surface> SURFACES = List.of(
            new HueBlockFaces.Surface("oak_log.png", Set.of("north"), Map.of("top", "axis=x", "north", "axis=y")),
            new HueBlockFaces.Surface("oak_log_top.png", Set.of("top"), Map.of("top", "axis=y", "north", "axis=z")));

    @Test void clickedTextureIsPreservedAndVariantsFollowTheChosenFace() {
        var state = new HueFaceSelection(SIDE, List.of(SIDE, END), SURFACES);
        assertEquals("top", state.face());
        assertSame(SIDE, state.selected());
        assertEquals("axis=x", state.surface().placements().get("top"));
        state.advance();
        assertSame(END, state.selected());
        state.face("north");
        assertSame(SIDE, state.selected());
        assertEquals(0, state.variant());
        state.advance();
        assertSame(END, state.selected());
        state.advance();
        assertSame(SIDE, state.selected());
        assertSame(SIDE, state.original());
    }
    @Test void excludedTexturesAndOtherItemsCannotBeConfirmed() {
        var other = candidate("minecraft:birch_log", "oak_log_top.png");
        var state = new HueFaceSelection(SIDE, List.of(SIDE, other), SURFACES);
        state.advance();
        assertNotNull(state.surface());
        assertNull(state.selected());
        state.advance();
        assertSame(SIDE, state.selected());
    }
    @Test void missingFacesNeverFallBackToTheOriginalCandidate() {
        var state = new HueFaceSelection(SIDE, List.of(SIDE), SURFACES);
        assertFalse(state.hasFace("bottom"));
        state.face("bottom");
        state.advance();
        assertNull(state.surface());
        assertNull(state.selected());
        assertEquals(0, state.count());
        assertThrows(IllegalArgumentException.class, () -> state.face("all"));
        var empty = new HueFaceSelection(SIDE, List.of(SIDE), List.of());
        assertNull(empty.selected());
    }
}
