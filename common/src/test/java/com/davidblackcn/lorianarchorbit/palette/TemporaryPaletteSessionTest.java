package com.davidblackcn.lorianarchorbit.palette;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class TemporaryPaletteSessionTest {
    @Test
    void temporaryWheelPreservesOrderAndRepeatedSamplesAcrossPlayerChangesButResetsOnShutdown() {
        var session = new TemporaryPaletteSession<String>(value -> value);
        List<String> samples = IntStream.range(0, 10).mapToObj(i -> "minecraft:" + (i < 5 ? "stone" : "dirt")).toList();
        session.setPalette(samples);
        session.bindInventory(new Object());
        session.bindInventory(new Object());
        assertEquals(samples, session.palette());
        assertEquals(10, session.palette().size());
        assertThrows(UnsupportedOperationException.class, () -> session.palette().clear());
        session.reset();
        assertTrue(session.palette().isEmpty());
    }

    @Test
    void backupKeepsFirstOriginalAndCopiesMutableValuesOnBothSides() {
        var session = new TemporaryPaletteSession<List<String>>(ArrayList::new);
        Object player = new Object();
        List<String> original = new ArrayList<>(List.of("named item", "count=32", "damage=17"));
        session.remember(player, 0, original);
        original.clear();
        session.remember(player, 0, List.of("second gradient result"));
        session.remember(player, 9, List.of());
        var backup = session.originals(player);
        assertEquals(List.of("named item", "count=32", "damage=17"), backup.get(0));
        assertEquals(List.of(), backup.get(9));
        backup.get(0).clear();
        assertEquals(3, session.originals(player).get(0).size());
        assertThrows(UnsupportedOperationException.class, backup::clear);
        session.clearBackup();
        assertFalse(session.hasBackup(player));
        session.remember(player, 0, List.of("new original after restore"));
        assertEquals(List.of("new original after restore"), session.originals(player).get(0));
    }

    @Test
    void newPlayerDisconnectAndShutdownInvalidateOriginalsWithoutCrossingSessions() {
        var session = new TemporaryPaletteSession<String>(value -> value);
        Object first = new Object();
        Object second = new Object();
        session.remember(first, 3, "first player's stack");
        assertFalse(session.hasBackup(second));
        assertTrue(session.originals(first).isEmpty());
        session.remember(first, 3, "new stack");
        session.bindInventory(null);
        assertFalse(session.hasBackup(first));
        session.remember(first, 3, "again");
        session.reset();
        assertTrue(session.originals(first).isEmpty());
    }

    @Test
    void bulkApplicationKeepsEverySampleAndRollsBackTheFirstOriginalAfterMultipleApplications() {
        var session = new TemporaryPaletteSession<String>(value -> value);
        Object player = new Object();
        List<String> originals = IntStream.range(0, 36).mapToObj(i -> "original-" + i).toList();
        List<String> inventory = new ArrayList<>(originals);
        List<String> ten = IntStream.range(0, 10).mapToObj(i -> "repeated-stone").toList();
        assertTrue(session.applyInventory(player, ten, inventory::get, inventory::set));
        assertEquals(ten, inventory.subList(0, 10));
        assertEquals(originals.subList(10, 36), inventory.subList(10, 36));
        List<String> thirtySix = IntStream.range(0, 36).mapToObj(i -> "second-" + i).toList();
        assertTrue(session.applyInventory(player, thirtySix, inventory::get, inventory::set));
        assertTrue(session.restoreInventory(player, inventory::set));
        assertEquals(originals, inventory);
        assertFalse(session.hasBackup(player));
        assertFalse(session.restoreInventory(player, inventory::set));
    }

    @Test
    void oversizedEmptyOrPlayerlessApplicationsDoNotReadWriteOrCreateBackups() {
        var session = new TemporaryPaletteSession<String>(value -> value);
        Object player = new Object();
        for (List<String> items : List.of(List.<String>of(), java.util.Collections.nCopies(37, "stone"))) {
            assertFalse(session.applyInventory(player, items, slot -> fail("Must not read a slot"),
                    (slot, item) -> fail("Must not write a slot")));
            assertFalse(session.hasBackup(player));
        }
        assertFalse(session.applyInventory(null, List.of("stone"), slot -> fail("Must not read a slot"),
                (slot, item) -> fail("Must not write a slot")));
    }

    @Test
    void all36InventorySlotsMapUniquelyWithoutTouchingArmorOrOffhand() {
        List<Integer> slots = IntStream.range(0, 36).map(TemporaryPaletteSession::menuSlot).boxed().toList();
        assertEquals(IntStream.range(36, 45).boxed().toList(), slots.subList(0, 9));
        assertEquals(IntStream.range(9, 36).boxed().toList(), slots.subList(9, 36));
        assertEquals(36, slots.stream().distinct().count());
        assertFalse(slots.contains(45));
        assertThrows(IllegalArgumentException.class, () -> TemporaryPaletteSession.menuSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> TemporaryPaletteSession.menuSlot(36));
    }
}
