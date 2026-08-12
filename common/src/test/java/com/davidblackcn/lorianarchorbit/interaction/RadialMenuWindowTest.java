package com.davidblackcn.lorianarchorbit.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RadialMenuWindowTest {
    @Test
    void largeRangeKeepsAllCandidatesInStateButShowsASelectedTwelveItemWindow() {
        List<Integer> all = IntStream.range(0, 30).boxed().toList();

        RadialMenuSnapshot<Integer> window = RadialMenuWindow.from(all, 27, 12);

        assertEquals(12, window.entries().size());
        assertEquals(27, window.selected().orElseThrow());
        assertEquals(List.of(27, 28, 29, 0, 1, 2, 3, 4, 5, 6, 7, 8), window.entries());
    }
}
