package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;
import com.davidblackcn.lorianarchorbit.config.PalettePreset;
import com.davidblackcn.lorianarchorbit.config.PaletteRotationDirection;
import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import com.davidblackcn.lorianarchorbit.palette.BuiltinPalettePresets;
import com.davidblackcn.lorianarchorbit.palette.PaletteMember;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RadialMenuWindowTest {
    @Test
    void largeRangeKeepsAllCandidatesInStateButShowsASelectedTwelveItemWindow() {
        List<Integer> all = IntStream.range(0, 30).boxed().toList();

        RadialMenuSnapshot<Integer> window = RadialMenuWindow.from(all, 27, 12);

        assertEquals(12, window.entries().size());
        assertEquals(27, window.selected().orElseThrow());
        assertEquals(List.of(21, 22, 23, 24, 25, 26, 27, 28, 29, 0, 1, 2), window.entries());
        assertEquals(6, window.selectedIndex());
    }

    @Test
    void brownPaletteShowsMangroveWoodBesideJungleLogBeforeScrollingBack() {
        var entries = BuiltinPalettePresets.groups(PalettePreset.COLOR_CATEGORIES).stream()
                .filter(group -> group.id().equals("builtin_visual_color_brown"))
                .findFirst().orElseThrow().members().stream().map(PaletteMember::itemId).toList();
        int selected = entries.indexOf("minecraft:jungle_log");
        assertTrue(selected >= 0);
        var window = RadialMenuWindow.from(entries, selected, 37);
        // The old forward-only window showed dark oak here, while selection used mangrove.
        assertEquals("minecraft:mangrove_wood", window.orderedEntries().getLast().value());
        var switched = new RadialMenuSnapshot<>(entries, selected).rotate(-1);
        assertEquals(switched.selected().orElseThrow(), window.orderedEntries().getLast().value());
    }

    @Test
    void visibleNeighborsMatchBothScrollDirectionsAcrossWrapsAndDuplicateItems() {
        // Identity includes the original position, even when item names repeat.
        record Entry(int position, String item) {}
        var entries = IntStream.range(0, 46).mapToObj(i -> new Entry(i, "item" + i % 3)).toList();
        for (int capacity : new int[]{3, 4, 12, 37, 45, 46, 47}) {
            for (int selected = 0; selected < entries.size(); selected++) {
                var state = new RadialMenuSnapshot<>(entries, selected);
                var window = RadialMenuWindow.from(entries, selected, capacity);
                var ordered = window.orderedEntries();
                for (var direction : PaletteRotationDirection.values()) {
                    for (int scroll : new int[]{-1, 1}) {
                        int step = direction.selectionSteps(scroll);
                        var shown = ordered.get(step > 0 ? 1 : ordered.size() - 1).value();
                        var next = state.rotate(step);
                        assertEquals(next.selected().orElseThrow(), shown);
                        assertEquals(next.selected(), RadialMenuWindow.from(entries, next.selectedIndex(), capacity).selected());
                        assertEquals(entries, next.entries());
                    }
                }
            }
        }
    }

    @Test
    void overflowingNeighborKeepsItsPositionWhenRotationStartsAndReachesEveryTarget() {
        var entries = IntStream.range(0, 46).boxed().toList();
        var center = new HudPoint(200, 200);
        var open = new RadialAnimationState(RadialAnimationMode.OFF, 0, 1);
        for (int capacity : new int[]{3, 4, 12, 37}) {
            for (int selected : new int[]{0, 14, 45}) {
                for (var target : PaletteTargetPosition.values()) {
                    for (var direction : PaletteRotationDirection.values()) {
                        for (int scroll : new int[]{-1, 1}) {
                            int step = direction.selectionSteps(scroll);
                            var before = RadialMenuWindow.from(entries, selected, capacity);
                            var after = RadialMenuWindow.from(entries, selected + step, capacity);
                            var rotation = RadialRotationState.idle(0, 140).retarget(step, capacity, 100, 140);
                            var oldSlots = RadialGeometry.slots(before, center, 100, open, 100, target.angleOffset());
                            var firstFrame = RadialGeometry.slots(after, center, 100, open, 100,
                                    target.angleOffset() + rotation.offsetRadians(100));
                            var selectedSlot = firstFrame.stream().filter(RadialSlot::selected).findFirst().orElseThrow();
                            var shown = oldSlots.stream().filter(s -> s.value().equals(selectedSlot.value())).findFirst().orElseThrow();
                            assertEquals(shown.x(), selectedSlot.x());
                            assertEquals(shown.y(), selectedSlot.y());
                            var finalSlot = RadialGeometry.slots(after, center, 100, open, 240,
                                    target.angleOffset() + rotation.offsetRadians(240)).getFirst();
                            assertTrue(finalSlot.selected());
                            assertEquals(oldSlots.getFirst().x(), finalSlot.x());
                            assertEquals(oldSlots.getFirst().y(), finalSlot.y());
                        }
                    }
                }
            }
        }
    }

    @Test
    void tinyWindowsStillKeepTheSelectedItemAndNormalizeIndices() {
        var entries = List.of("a", "b", "c");
        for (int capacity : new int[]{1, 2}) {
            for (int selected : new int[]{-1, 0, 2, 3}) {
                var window = RadialMenuWindow.from(entries, selected, capacity);
                assertEquals(capacity, window.entries().size());
                assertEquals(entries.get(Math.floorMod(selected, entries.size())), window.selected().orElseThrow());
            }
        }
    }
}
