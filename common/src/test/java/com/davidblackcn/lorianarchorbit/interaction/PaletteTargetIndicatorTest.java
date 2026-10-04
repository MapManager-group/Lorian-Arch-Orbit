package com.davidblackcn.lorianarchorbit.interaction;

import com.davidblackcn.lorianarchorbit.config.PaletteTargetPosition;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PaletteTargetIndicatorTest {
    @Test
    void targetOrientationChangesCoordinatesWithoutChangingSelectionOrOrder() {
        List<String> entries = List.of("oak", "stone", "stone", "birch", "brick");
        int[][] directions = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};
        for (var position : PaletteTargetPosition.values()) {
            for (int selected = -1; selected <= entries.size(); selected++) {
                var snapshot = RadialMenuWindow.from(entries, selected, 4);
                var slots = RadialGeometry.slots(snapshot, new HudPoint(200, 150), 80,
                        new RadialAnimationState(RadialAnimationMode.OFF, 0, 1), 0, position.angleOffset());
                var target = slots.stream().filter(RadialSlot::selected).findFirst().orElseThrow();
                assertEquals(entries.get(Math.floorMod(selected, entries.size())), target.value());
                assertEquals(200 + directions[position.ordinal()][0] * 80, target.x());
                assertEquals(150 + directions[position.ordinal()][1] * 80, target.y());
                assertEquals(snapshot.entries(), slots.stream().map(RadialSlot::value).toList());
            }
        }
    }

    @Test
    void triangleKeepsItsGapInEveryDirectionAndHidesInTinyRings() {
        for (var position : PaletteTargetPosition.values()) {
            assertTrue(PaletteTargetIndicator.at(100, 100, 0, position, 0).isEmpty());
            assertTrue(PaletteTargetIndicator.at(100, 100, 47.9, position, 0).isEmpty());
            for (int radius : new int[]{48, 57, 80, 130}) {
                var arrow = PaletteTargetIndicator.at(100, 100, radius, position, 0).orElseThrow();
                assertEquals(radius - 17, Math.hypot(arrow.tipX() - 100, arrow.tipY() - 100), 1e-8);
                var tip = arrow.pixel(0, 0);
                var base = arrow.pixel(0, -4);
                assertTrue(Math.hypot(tip.x() - 100, tip.y() - 100) > Math.hypot(base.x() - 100, base.y() - 100));
            }
        }
    }

    @Test
    void repeatedScrollChangesSlotsButNeverAffectsTheIndependentMarker() {
        var center = new HudPoint(100, 100);
        for (var position : PaletteTargetPosition.values()) {
            var marker = PaletteTargetIndicator.at(100, 100, 80, position, 120).orElseThrow();
            var snapshot = new RadialMenuSnapshot<>(List.of("oak", "stone", "stone", "birch"), 0);
            var rotation = RadialRotationState.idle(0, 140);
            for (int step : new int[]{1, 1, -1, 3, -5}) {
                snapshot = snapshot.rotate(step);
                rotation = rotation.retarget(step, snapshot.entries().size(), 100, 140);
                var moving = RadialGeometry.slots(snapshot, center, 80,
                        new RadialAnimationState(RadialAnimationMode.OFF, 0, 1), 120,
                        position.angleOffset() + rotation.offsetRadians(120)).getFirst();
                var settled = RadialGeometry.slots(snapshot, center, 80,
                        new RadialAnimationState(RadialAnimationMode.OFF, 0, 1), 300, position.angleOffset()).getFirst();
                assertTrue(moving.x() != settled.x() || moving.y() != settled.y());
                assertEquals(marker, PaletteTargetIndicator.at(100, 100, 80, position, 120).orElseThrow());
                double gap = Math.hypot(settled.x() - marker.tipX(), settled.y() - marker.tipY());
                assertTrue(gap >= 17 && gap <= 20);
            }
        }
    }

    @Test
    void bobStaysOnTheTargetAxisAndRepeatsWithoutResetting() {
        for (var position : PaletteTargetPosition.values()) {
            var start = PaletteTargetIndicator.at(100, 100, 80, position, 0).orElseThrow();
            var inward = PaletteTargetIndicator.at(100, 100, 80, position, 450).orElseThrow();
            assertEquals(3, Math.hypot(start.tipX() - inward.tipX(), start.tipY() - inward.tipY()), 1e-8);
            for (int time = 0; time < 900; time += 15) {
                var marker = PaletteTargetIndicator.at(100, 100, 80, position, time).orElseThrow();
                assertEquals(marker, PaletteTargetIndicator.at(100, 100, 80, position, time + 900).orElseThrow());
                assertEquals(0, (marker.tipX() - 100) * start.forwardY()
                        - (marker.tipY() - 100) * start.forwardX(), 1e-8);
                double distance = Math.hypot(marker.tipX() - 100, marker.tipY() - 100);
                assertTrue(distance >= 60 && distance <= 63);
            }
        }
    }
}
