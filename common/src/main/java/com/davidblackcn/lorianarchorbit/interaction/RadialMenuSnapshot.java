package com.davidblackcn.lorianarchorbit.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RadialMenuSnapshot<T> {
    private final List<T> entries;
    private final int selectedIndex;

    public RadialMenuSnapshot(List<T> entries, int selectedIndex) {
        this.entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (this.entries.isEmpty()) {
            if (selectedIndex != 0) {
                throw new IllegalArgumentException("selectedIndex must be zero for an empty menu");
            }
            this.selectedIndex = 0;
        } else {
            this.selectedIndex = Math.floorMod(selectedIndex, this.entries.size());
        }
    }

    public static <T> RadialMenuSnapshot<T> empty() {
        return new RadialMenuSnapshot<>(List.of(), 0);
    }

    public List<T> entries() {
        return entries;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public Optional<T> selected() {
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.get(selectedIndex));
    }

    public RadialMenuSnapshot<T> rotate(int steps) {
        if (entries.isEmpty() || steps == 0) {
            return this;
        }
        return new RadialMenuSnapshot<>(entries, Math.floorMod(selectedIndex + steps, entries.size()));
    }

    public List<RadialOrderedEntry<T>> orderedEntries() {
        if (entries.isEmpty()) {
            return List.of();
        }
        List<RadialOrderedEntry<T>> ordered = new ArrayList<>(entries.size());
        for (int offset = 0; offset < entries.size(); offset++) {
            int index = Math.floorMod(selectedIndex + offset, entries.size());
            ordered.add(new RadialOrderedEntry<>(entries.get(index), index, offset, offset == 0));
        }
        return List.copyOf(ordered);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RadialMenuSnapshot<?> that
                && selectedIndex == that.selectedIndex
                && entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, selectedIndex);
    }
}
