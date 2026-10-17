package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RadialMenuSnapshotTest {
    @Test
    public void rotatesInBothDirectionsWithFloorModWrapping() {
        RadialMenuSnapshot<String> snapshot = new RadialMenuSnapshot<>(List.of("a", "b", "c"), 0);

        assertEquals("c", snapshot.rotate(-1).selected().orElseThrow());
        assertEquals("b", snapshot.rotate(4).selected().orElseThrow());
        assertEquals("a", snapshot.selected().orElseThrow());
    }

    @Test
    public void orderedEntriesAlwaysContainsEveryConfiguredCandidate() {
        RadialMenuSnapshot<Integer> snapshot = new RadialMenuSnapshot<>(
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), 7
        );
        List<RadialOrderedEntry<Integer>> visible = snapshot.orderedEntries();

        assertEquals(12, visible.size());
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                visible.stream().map(RadialOrderedEntry::relativeIndex).toList());
        assertEquals(1, visible.stream().filter(RadialOrderedEntry::selected).count());
        assertEquals(7, visible.stream().filter(RadialOrderedEntry::selected).findFirst().orElseThrow().value());
    }

    @Test
    public void emptySnapshotHasNoSelectionOrVisibleEntries() {
        RadialMenuSnapshot<String> empty = RadialMenuSnapshot.empty();

        assertTrue(empty.selected().isEmpty());
        assertTrue(empty.orderedEntries().isEmpty());
        assertEquals(empty, empty.rotate(3));
    }
}
