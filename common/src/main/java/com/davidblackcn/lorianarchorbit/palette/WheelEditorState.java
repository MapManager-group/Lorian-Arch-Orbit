package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.WheelConfigSnapshot;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Draft and save baselines are independent of widget/screen lifetime. */
public final class WheelEditorState {
    public final WheelConfigSnapshot primaryBase;
    public final WheelConfigSnapshot secondaryBase;
    public final PaletteWheelDraft primary;
    public final PaletteWheelDraft secondary;
    public final Deque<Snapshot> undo = new ArrayDeque<>();
    private List<PaletteGroup> savedPrimary;
    private List<PaletteGroup> savedSecondary;
    public record Snapshot(List<PaletteGroup> primary, List<PaletteGroup> secondary) {
        public Snapshot { primary = List.copyOf(primary); secondary = List.copyOf(secondary); }
    }
    public record GroupRef(boolean primary, String id) { }
    public WheelEditorState(WheelConfigSnapshot primary, WheelConfigSnapshot secondary) {
        primaryBase = primary; secondaryBase = secondary;
        this.primary = new PaletteWheelDraft(primary); this.secondary = new PaletteWheelDraft(secondary);
        savedPrimary = this.primary.groups(); savedSecondary = this.secondary.groups();
    }
    public boolean dirty() { return !savedPrimary.equals(primary.groups()) || !savedSecondary.equals(secondary.groups()); }
    public void saved(boolean primaryOk, boolean secondaryOk) {
        if (primaryOk) savedPrimary = primary.groups();
        if (secondaryOk) savedSecondary = secondary.groups();
    }
    public PaletteGroup resolve(GroupRef ref) {
        if (ref == null) return null;
        return (ref.primary() ? primary : secondary).groups().stream().filter(g -> g.id().equals(ref.id())).findFirst().orElse(null);
    }
}
