package com.davidblackcn.lorianarchorbit.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkbenchMenuNavigationTest {
    @Test void openingAndLeavingWithPointerDoesNotHighlightAnUnrelatedRow() {
        var navigation = new WorkbenchMenuNavigation();
        assertEquals(-1, navigation.index());
        assertFalse(navigation.keyboard());
        navigation.pointer(2, true);
        assertEquals(2, navigation.index());
        navigation.pointer(-1, true);
        assertEquals(-1, navigation.index());
    }
    @Test void keyboardOwnsHighlightUntilThePointerActuallyMoves() {
        var navigation = new WorkbenchMenuNavigation();
        navigation.pointer(0, true);
        navigation.move(1, 4, index -> index != 1);
        assertEquals(2, navigation.index());
        assertTrue(navigation.keyboard());
        navigation.pointer(0, false);
        assertEquals(2, navigation.index());
        navigation.pointer(0, true);
        assertEquals(0, navigation.index());
        assertFalse(navigation.keyboard());
    }
    @Test void keyboardEntryAndWrapSkipDisabledRowsAndResetOnClose() {
        var navigation = new WorkbenchMenuNavigation();
        navigation.move(1, 3, index -> index != 0);
        assertEquals(1, navigation.index());
        navigation.move(-1, 3, index -> index != 0);
        assertEquals(2, navigation.index());
        navigation.reset();
        navigation.move(-1, 3, index -> true);
        assertEquals(2, navigation.index());
        navigation.move(1, 3, index -> false);
        assertEquals(-1, navigation.index());
        assertFalse(navigation.keyboard());
    }
}
