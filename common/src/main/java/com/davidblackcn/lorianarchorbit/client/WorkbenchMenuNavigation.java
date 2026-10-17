package com.davidblackcn.lorianarchorbit.client;

import java.util.function.IntPredicate;

/** One active row, owned by either pointer movement or keyboard navigation, never by the current value. */
final class WorkbenchMenuNavigation {
    private int index = -1;
    private boolean keyboard;
    int index() { return index; }
    boolean keyboard() { return keyboard; }
    void reset() { index = -1; keyboard = false; }
    void pointer(int hoveredIndex, boolean moved) {
        if (moved) keyboard = false;
        if (!keyboard) index = hoveredIndex;
    }
    void focus(int index) { this.index = index; keyboard = true; }
    void move(int direction, int count, IntPredicate enabled) {
        int next = index < 0 ? (direction > 0 ? -1 : 0) : index;
        for (int i = 0; i < count; i++) {
            next = Math.floorMod(next + direction, count);
            if (enabled.test(next)) { focus(next); return; }
        }
        reset();
    }
}
