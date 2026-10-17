package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteRotationDirection;
import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaletteRotationDirectionTest {
    private static final HudPoint CENTER = new HudPoint(200, 200);
    private static final RadialAnimationState OPEN = new RadialAnimationState(RadialAnimationMode.OFF, 0, 1);

    @Test
    void upwardScrollMovesInNamedScreenDirectionAndKeepsEveryTargetPosition() {
        var before = new RadialMenuSnapshot<>(List.of(0, 1, 2, 3, 4, 5, 6, 7), 0);
        for (var direction : PaletteRotationDirection.values()) {
            for (var target : PaletteTargetPosition.values()) {
                int steps = direction.selectionSteps(1);
                var after = before.rotate(steps);
                var rotation = RadialRotationState.idle(0, 140).retarget(steps, 8, 100, 140);
                var oldSlots = RadialGeometry.slots(before, CENTER, 100, OPEN, 100, target.angleOffset());
                var start = RadialGeometry.slots(after, CENTER, 100, OPEN, 100,
                        target.angleOffset() + rotation.offsetRadians(100));
                // Retargeting keeps all items at their previous coordinates at the first frame.
                for (var slot : start) {
                    var old = oldSlots.stream().filter(s -> s.sourceIndex() == slot.sourceIndex()).findFirst().orElseThrow();
                    assertEquals(old.x(), slot.x());
                    assertEquals(old.y(), slot.y());
                }
                var end = RadialGeometry.slots(after, CENTER, 100, OPEN, 240,
                        target.angleOffset() + rotation.offsetRadians(240));
                var from = start.stream().filter(RadialSlot::selected).findFirst().orElseThrow();
                var to = end.stream().filter(RadialSlot::selected).findFirst().orElseThrow();
                int cross = (from.x() - CENTER.x()) * (to.y() - CENTER.y())
                        - (from.y() - CENTER.y()) * (to.x() - CENTER.x());
                // Screen Y increases downwards, so a positive cross product is clockwise.
                assertEquals(direction == PaletteRotationDirection.CLOCKWISE, cross > 0);
                var originalTarget = oldSlots.stream().filter(RadialSlot::selected).findFirst().orElseThrow();
                assertEquals(originalTarget.x(), to.x());
                assertEquals(originalTarget.y(), to.y());
            }
        }
    }

    @Test
    void bothDirectionsVisitEveryDuplicatePositionWithAnOverflowWindow() {
        var entries = List.of("a", "b", "a", "c", "d", "c", "e", "f", "a");
        for (var direction : PaletteRotationDirection.values()) {
            var snapshot = new RadialMenuSnapshot<>(entries, 0);
            var visited = new HashSet<Integer>();
            for (int i = 0; i < entries.size(); i++) {
                visited.add(snapshot.selectedIndex());
                var visible = RadialMenuWindow.from(snapshot.entries(), snapshot.selectedIndex(), 4);
                assertEquals(snapshot.selected(), visible.selected());
                assertEquals(4, visible.entries().size());
                snapshot = snapshot.rotate(direction.selectionSteps(1));
                assertEquals(entries, snapshot.entries());
            }
            assertEquals(entries.size(), visited.size());
            assertEquals(0, snapshot.selectedIndex());
            snapshot = snapshot.rotate(direction.selectionSteps(3)).rotate(direction.selectionSteps(-3));
            assertEquals(0, snapshot.selectedIndex());
            assertSame(snapshot, snapshot.rotate(direction.selectionSteps(0)));
        }
    }

    @Test
    void fractionalScrollingRetainsItsThresholdBeforeApplyingDirection() {
        for (var direction : PaletteRotationDirection.values()) {
            var scroll = new ScrollAccumulator();
            assertEquals(0, direction.selectionSteps(scroll.add(0.4)));
            assertEquals(direction.selectionSteps(1), direction.selectionSteps(scroll.add(0.6)));
            assertEquals(direction.selectionSteps(-1), direction.selectionSteps(scroll.add(-1)));
        }
    }
}
