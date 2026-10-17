package com.davidblackcn.lorianarchorbit.interaction;

import java.util.ArrayList;
import java.util.List;

public final class RadialGeometry {
    private RadialGeometry() {
    }

    public static <T> List<RadialSlot<T>> slots(
            RadialMenuSnapshot<T> snapshot,
            HudPoint center,
            int radius,
            RadialAnimationState animation,
            long nowMillis
    ) {
        return slots(snapshot, center, radius, animation, nowMillis, 0.0);
    }

    public static <T> List<RadialSlot<T>> slots(
            RadialMenuSnapshot<T> snapshot,
            HudPoint center,
            int radius,
            RadialAnimationState animation,
            long nowMillis,
            double angleOffsetRadians
    ) {
        if (!Double.isFinite(angleOffsetRadians)) {
            throw new IllegalArgumentException("angleOffsetRadians must be finite");
        }
        List<RadialOrderedEntry<T>> ordered = snapshot.orderedEntries();
        if (ordered.isEmpty()) {
            return List.of();
        }
        List<RadialSlot<T>> result = new ArrayList<>(ordered.size());
        for (int index = 0; index < ordered.size(); index++) {
            RadialOrderedEntry<T> entry = ordered.get(index);
            double progress = animation.entryProgress(index, ordered.size(), nowMillis);
            double angle = Math.PI / 2.0
                    + (Math.PI * 2.0 * entry.relativeIndex() / ordered.size())
                    + angleOffsetRadians;
            int x = center.x() + (int) Math.round(Math.cos(angle) * radius * progress);
            int y = center.y() + (int) Math.round(Math.sin(angle) * radius * progress);
            result.add(new RadialSlot<>(entry.value(), entry.sourceIndex(), entry.selected(), x, y, progress));
        }
        return List.copyOf(result);
    }
}
