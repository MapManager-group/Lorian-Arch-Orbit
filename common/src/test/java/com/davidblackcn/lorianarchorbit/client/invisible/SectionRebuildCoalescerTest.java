package com.davidblackcn.lorianarchorbit.client.invisible;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class SectionRebuildCoalescerTest {
    @Test
    void repeatedRequestsCollapseIntoOneRebuild() {
        SectionRebuildCoalescer coalescer = new SectionRebuildCoalescer();

        coalescer.request();
        coalescer.request();
        coalescer.request();

        assertTrue(coalescer.consume());
        assertFalse(coalescer.consume());
        assertEquals(1, coalescer.completedRebuilds());
    }

    @Test
    void laterRequestSchedulesASecondRebuild() {
        SectionRebuildCoalescer coalescer = new SectionRebuildCoalescer();
        coalescer.request();
        assertTrue(coalescer.consume());

        coalescer.request();

        assertTrue(coalescer.consume());
        assertEquals(2, coalescer.completedRebuilds());
    }
}
