package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class RadialAnimationAndLayoutTest {
    @Test
    public void expandMovesAllEntriesTogetherAndOffIsImmediate() {
        RadialAnimationState expand = new RadialAnimationState(RadialAnimationMode.EXPAND, 100, 200);
        RadialAnimationState off = new RadialAnimationState(RadialAnimationMode.OFF, 100, 200);

        assertEquals(expand.entryProgress(0, 5, 200), expand.entryProgress(4, 5, 200));
        assertEquals(1.0, off.entryProgress(3, 5, 100));
        assertTrue(off.complete(100));
    }

    @Test
    public void clockwiseRevealStaggersEntriesAndEventuallyCompletes() {
        RadialAnimationState state = new RadialAnimationState(RadialAnimationMode.CLOCKWISE, 100, 200);

        assertTrue(state.entryProgress(0, 8, 180) > state.entryProgress(6, 8, 180));
        assertEquals(1.0, state.entryProgress(7, 8, 300));
        assertTrue(state.complete(300));
    }

    @Test
    public void layoutsRemainInsideSmallNormalAndUltrawideScreens() {
        assertSafeLayout(160, 90, 48);
        assertSafeLayout(854, 480, 48);
        assertSafeLayout(3440, 900, 48);
    }

    @Test
    public void radiusGrowsWithEveryMemberUntilScreenBoundsCapIt() {
        int one = HudLayout.adaptiveRadialRadius(854, 480, 1, 57, 15, HudLayout.DEFAULT_MARGIN);
        int twelve = HudLayout.adaptiveRadialRadius(854, 480, 12, 57, 15, HudLayout.DEFAULT_MARGIN);
        int twenty = HudLayout.adaptiveRadialRadius(854, 480, 20, 57, 15, HudLayout.DEFAULT_MARGIN);
        int hundred = HudLayout.adaptiveRadialRadius(854, 480, 100, 57, 15, HudLayout.DEFAULT_MARGIN);

        assertEquals(57, one);
        assertTrue(twelve > one);
        assertTrue(twenty > twelve);
        assertTrue(hundred >= twenty);
        assertEquals(195, hundred);
    }

    @Test
    public void wheelRotationUsesScrollDirectionAndSettlesAtBottom() {
        RadialRotationState clockwise = RadialRotationState.idle(0, 140)
                .retarget(1, 8, 100, 140);
        RadialRotationState counterClockwise = RadialRotationState.idle(0, 140)
                .retarget(-1, 8, 100, 140);

        assertTrue(clockwise.offsetRadians(100) > 0.0);
        assertTrue(counterClockwise.offsetRadians(100) < 0.0);
        assertEquals(0.0, clockwise.offsetRadians(240), 0.000_001);
        assertEquals(0.0, counterClockwise.offsetRadians(240), 0.000_001);
    }

    @Test
    public void repeatedWheelRotationContinuesFromCurrentAnimatedOffset() {
        RadialRotationState first = RadialRotationState.idle(0, 140)
                .retarget(1, 8, 100, 140);
        double inFlight = first.offsetRadians(150);
        RadialRotationState continued = first.retarget(1, 8, 150, 140);

        assertEquals(inFlight + Math.PI / 4.0, continued.offsetRadians(150), 0.000_001);
    }

    @Test
    public void selectedOverflowEntryRemainsAtBottomOfRadial() {
        RadialMenuSnapshot<Integer> snapshot = new RadialMenuSnapshot<>(
                List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), 0
        );
        HudPoint center = new HudPoint(100, 100);
        RadialAnimationState animation = new RadialAnimationState(RadialAnimationMode.OFF, 0, 1);
        RadialSlot<Integer> selected = RadialGeometry.slots(snapshot, center, 40, animation, 0).stream()
                .filter(RadialSlot::selected)
                .findFirst()
                .orElseThrow();

        assertEquals(center.x(), selected.x());
        assertEquals(center.y() + 40, selected.y());
    }

    private static void assertSafeLayout(int width, int height, int itemCount) {
        int margin = HudLayout.DEFAULT_MARGIN;
        int itemHalf = 15;
        int radius = HudLayout.adaptiveRadialRadius(width, height, itemCount, 57, itemHalf, margin);
        HudPoint center = HudLayout.crosshairRadialCenter(width, height, margin);
        assertTrue(center.x() - radius - itemHalf >= margin);
        assertTrue(center.x() + radius + itemHalf <= width - margin);
        assertTrue(center.y() - radius - itemHalf >= HudLayout.BOSS_BAR_BOTTOM_OFFSET);
        assertTrue(center.y() + radius + itemHalf <= height - HudLayout.HOTBAR_TOP_OFFSET - HudLayout.HOTBAR_GAP);

        HudPoint text = HudLayout.crosshairText(width, height, 80, 9, margin);
        HudPoint above = HudLayout.crosshairTextAbove(width, height, 80, 9, margin);
        assertTrue(text.x() >= margin);
        assertTrue(text.x() + 80 <= width - margin);
        assertTrue(text.y() >= margin);
        assertTrue(above.y() < height / 2);
        assertTrue(text.y() + 9 <= height - margin);
    }

}
