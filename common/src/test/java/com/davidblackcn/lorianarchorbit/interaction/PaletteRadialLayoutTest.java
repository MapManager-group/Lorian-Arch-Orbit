package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.client.PaletteViewport;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class PaletteRadialLayoutTest {
    @Test
    void ringsFitAndRetainReadableSpacingAcrossResolutionsAndGuiScales() {
        for (int[] size : new int[][]{{854, 480}, {1280, 720}, {1920, 1080}, {2560, 1440},
                {3840, 2036}, {3840, 2160}, {3440, 1440}, {1024, 768}, {320, 180}}) {
            for (int guiScale : new int[]{1, 2, 3, 5}) {
                var viewport = PaletteViewport.calculate(size[0], size[1], guiScale);
                for (int count : new int[]{0, 1, 2, 12, 34, 48, 100, 1000}) {
                    var ring = PaletteRadialLayout.hud(count, viewport.width(), viewport.height(), 1 / viewport.scale());
                    assertTrue(ring.radius() >= 0);
                    assertTrue(ring.visibleCount() <= count);
                    assertEquals(count == 0, ring.visibleCount() == 0);
                    assertTrue(ring.radius() + PaletteRadialLayout.ITEM_HALF_SIZE <= viewport.width() / 2);
                    if (ring.visibleCount() > 1) {
                        assertTrue(2 * ring.radius() * Math.sin(Math.PI / ring.visibleCount()) >= PaletteRadialLayout.SLOT_SPACING - 1e-8);
                    }
                    double hudScale = Math.max(1, 1 / viewport.scale());
                    if (PaletteRadialLayout.BOTTOM_HUD_SAFE_AREA * hudScale + 10 <= viewport.height() / 2) {
                        assertTrue(viewport.height() / 2 + ring.radius() + 10
                                <= viewport.height() - PaletteRadialLayout.BOTTOM_HUD_SAFE_AREA * hudScale + 1e-8);
                        assertTrue(viewport.height() / 2 - ring.radius() - 10
                                >= PaletteRadialLayout.TOP_HUD_SAFE_AREA * hudScale - 1e-8);
                    }
                }
            }
        }
    }

    @Test
    void capacityBoundaryAndEverySelectionRemainReachableIncludingRepeats() {
        List<Integer> entries = IntStream.range(0, 100).map(i -> i / 2).boxed().toList();
        int capacity = PaletteRadialLayout.calculate(100, 80).visibleCount();
        for (int selected = -1; selected <= entries.size(); selected++) {
            var window = RadialMenuWindow.from(entries, selected, capacity);
            assertEquals(entries.get(Math.floorMod(selected, entries.size())), window.selected().orElseThrow());
            assertEquals(capacity, window.entries().size());
            for (int offset = 0; offset < capacity; offset++) {
                assertEquals(entries.get(Math.floorMod(selected + offset, entries.size())), window.entries().get(offset));
            }
            var bottom = RadialGeometry.slots(window, new HudPoint(100, 100), 80,
                    new RadialAnimationState(RadialAnimationMode.OFF, 0, 1), 0).getFirst();
            assertTrue(bottom.selected());
            assertEquals(100, bottom.x());
            assertEquals(180, bottom.y());
        }
        assertEquals(capacity, RadialMenuWindow.from(entries.subList(0, capacity), 0, capacity).entries().size());
        assertEquals(capacity, RadialMenuWindow.from(entries.subList(0, capacity + 1), 0, capacity).entries().size());
        assertTrue(RadialMenuWindow.from(List.of(), 0, capacity).entries().isEmpty());
    }
}
