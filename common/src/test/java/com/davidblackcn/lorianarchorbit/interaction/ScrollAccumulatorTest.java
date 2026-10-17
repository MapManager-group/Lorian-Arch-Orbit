package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class ScrollAccumulatorTest {
    @Test
    public void combinesTouchpadFractionsIntoWholeSteps() {
        ScrollAccumulator accumulator = new ScrollAccumulator();

        assertEquals(0, accumulator.add(0.25));
        assertEquals(0, accumulator.add(0.25));
        assertEquals(1, accumulator.add(0.5));
        assertEquals(-2, accumulator.add(-2.4));
        assertEquals(-0.4, accumulator.remainder(), 0.000001);
    }

    @Test
    public void directionChangeDropsThePreviousFraction() {
        ScrollAccumulator accumulator = new ScrollAccumulator();
        accumulator.add(0.75);

        assertEquals(0, accumulator.add(-0.75));
        assertEquals(-1, accumulator.add(-0.25));
        accumulator.reset();
        assertEquals(0.0, accumulator.remainder());
    }
}
