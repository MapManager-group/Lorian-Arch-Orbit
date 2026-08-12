package com.davidblackcn.lorianarchorbit.interaction;

import java.util.ArrayList;
import java.util.List;

public final class RadialMenuWindow {
    private RadialMenuWindow() {
    }

    public static <T> RadialMenuSnapshot<T> from(List<T> entries, int selectedIndex, int maximumVisible) {
        if (maximumVisible < 1) {
            throw new IllegalArgumentException("maximumVisible must be positive");
        }
        if (entries.isEmpty()) {
            return RadialMenuSnapshot.empty();
        }
        int selected = Math.floorMod(selectedIndex, entries.size());
        if (entries.size() <= maximumVisible) {
            return new RadialMenuSnapshot<>(entries, selected);
        }
        List<T> visible = new ArrayList<>(maximumVisible);
        for (int offset = 0; offset < maximumVisible; offset++) {
            visible.add(entries.get(Math.floorMod(selected + offset, entries.size())));
        }
        return new RadialMenuSnapshot<>(visible, 0);
    }
}
