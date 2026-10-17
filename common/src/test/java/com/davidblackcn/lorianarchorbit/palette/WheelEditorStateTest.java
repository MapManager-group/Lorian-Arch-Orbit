package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.WheelConfigCodec;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WheelEditorStateTest {
    private static PaletteGroup group(String id, String name) { return new PaletteGroup(id, name, "minecraft:stone", List.of(new PaletteMember("minecraft:stone"))); }
    @Test void saveBaselinesTrackBothLayersAndPartialFailures() {
        var state = new WheelEditorState(new WheelConfigCodec().defaults(), new WheelConfigCodec().defaults());
        assertFalse(state.dirty());
        state.primary.addGroup(group("a", "A")); state.secondary.addGroup(group("b", "B"));
        state.saved(true, false); assertTrue(state.dirty());
        state.saved(false, true); assertFalse(state.dirty());
        state.primary.replace(List.of()); assertTrue(state.dirty());
        state.primary.restoreWithoutUndo(List.of(group("a", "A"))); assertFalse(state.dirty());
    }
    @Test void groupReferencesSurviveRenameAndReorderButDoNotRetargetAfterDeletion() {
        var state = new WheelEditorState(new WheelConfigCodec().defaults(), new WheelConfigCodec().defaults());
        var ref = new WheelEditorState.GroupRef(true, "a");
        state.primary.replace(List.of(group("a", "A"), group("b", "B")));
        state.secondary.addGroup(group("a", "Other layer"));
        state.primary.replace(List.of(group("b", "B"), group("a", "Renamed")));
        assertEquals("Renamed", state.resolve(ref).displayName());
        state.primary.removeGroup(1); assertNull(state.resolve(ref));
    }
}
