package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public final class PaletteWheelDraft {
    private final Deque<List<PaletteGroup>> undo = new ArrayDeque<>();
    private List<PaletteGroup> groups;

    public PaletteWheelDraft(WheelConfigSnapshot snapshot) {
        groups = new ArrayList<>(snapshot.typedGroups());
    }

    public List<PaletteGroup> groups() {
        return List.copyOf(groups);
    }

    public void replace(List<PaletteGroup> replacement) {
        remember();
        groups = new ArrayList<>(replacement);
    }

    public void addGroup(PaletteGroup group) {
        remember();
        groups.add(Objects.requireNonNull(group, "group"));
    }

    public void replaceGroup(int index, PaletteGroup group) {
        remember();
        groups.set(index, Objects.requireNonNull(group, "group"));
    }

    public void removeGroup(int index) {
        remember();
        groups.remove(index);
    }

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        groups = new ArrayList<>(undo.pop());
        return true;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public void restoreWithoutUndo(List<PaletteGroup> snapshot) {
        groups = new ArrayList<>(Objects.requireNonNull(snapshot, "snapshot"));
    }

    private void remember() {
        undo.push(List.copyOf(groups));
    }
}
