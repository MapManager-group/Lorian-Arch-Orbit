package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.WheelConfigCodec;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteWheelDraftTest {
    @Test
    void undoRestoresLastUnsavedMutation() {
        PaletteWheelDraft draft = new PaletteWheelDraft(new WheelConfigCodec().defaults());
        draft.addGroup(new PaletteGroup("a", "A", "minecraft:stone", List.of()));
        assertTrue(draft.canUndo());
        assertEquals(1, draft.groups().size());
        assertTrue(draft.undo());
        assertTrue(draft.groups().isEmpty());
        assertFalse(draft.canUndo());
    }
}
