package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import java.util.Comparator;
import java.util.List;

/** Local picker selection; inspecting an unavailable face never silently substitutes another texture. */
public final class HueFaceSelection {
    private final HueGradient.Candidate original;
    private final List<HueGradient.Candidate> allowed;
    private final List<HueBlockFaces.Surface> all;
    private List<HueBlockFaces.Surface> surfaces;
    private String face;
    private int variant;

    public HueFaceSelection(HueGradient.Candidate original, List<HueGradient.Candidate> allowed,
                            List<HueBlockFaces.Surface> surfaces) {
        this.original = original;
        this.allowed = List.copyOf(allowed);
        this.all = List.copyOf(surfaces);
        face = HueBlockFaces.DIRECTIONS.stream().filter(original.faces()::contains).findFirst().orElse("top");
        update();
        for (int i = 0; i < this.surfaces.size(); i++)
            if (this.surfaces.get(i).texture().equals(original.block().texture())) variant = i;
    }
    private void update() {
        surfaces = all.stream().filter(s -> s.faces().contains(face))
                .sorted(Comparator.comparing(s -> !s.defaultFaces().contains(face))).toList();
        variant = Math.clamp(variant, 0, Math.max(0, surfaces.size() - 1));
    }
    public HueGradient.Candidate original() { return original; }
    public String face() { return face; }
    public int variant() { return variant; }
    public int count() { return surfaces.size(); }
    public boolean hasFace(String direction) { return all.stream().anyMatch(s -> s.faces().contains(direction)); }
    public void face(String direction) {
        if (!HueBlockFaces.DIRECTIONS.contains(direction)) throw new IllegalArgumentException("Unknown face: " + direction);
        face = direction; variant = 0; update();
    }
    public void advance() { if (!surfaces.isEmpty()) variant = (variant + 1) % surfaces.size(); }
    public HueBlockFaces.Surface surface() { return surfaces.isEmpty() ? null : surfaces.get(variant); }
    public HueGradient.Candidate selected() {
        var surface = surface();
        return surface == null ? null : allowed.stream().filter(c -> c.itemId().equals(original.itemId())
                && c.block().texture().equals(surface.texture())).findFirst().orElse(null);
    }
}
