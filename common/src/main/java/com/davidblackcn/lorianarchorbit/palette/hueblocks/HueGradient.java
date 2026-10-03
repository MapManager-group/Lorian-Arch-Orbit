package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import java.util.ArrayList;
import java.util.List;

/** Multi-stop interpolation and nearest-texture matching, independent of Minecraft/UI state. */
public final class HueGradient {
    public static final int MAX_STOPS = 16;
    public static final int MAX_STEPS = 128;
    private HueGradient() {}

    public record Candidate(String itemId, HueBlocksData.Block block) {}
    /** steps is the number of samples including both endpoints of the outgoing segment. */
    public record Stop(int rgb, int steps, Candidate pinned) {
        public Stop {
            if (rgb < 0 || rgb > 0xFFFFFF || steps < 2 || steps > MAX_STEPS) {
                throw new IllegalArgumentException("Invalid gradient stop");
            }
        }
    }

    public record Target(double red, double green, double blue, HueColor lab, Candidate pinned) {
        public int rgb() { return (int) Math.round(red) << 16 | (int) Math.round(green) << 8 | (int) Math.round(blue); }
        public int displayRgb(boolean oklab) { return oklab ? lab.toRgb() : rgb(); }
    }

    public static List<Target> targets(List<Stop> stops) {
        if (stops.size() < 2 || stops.size() > MAX_STOPS) throw new IllegalArgumentException("Use 2-16 stops");
        List<Target> targets = new ArrayList<>();
        for (int segment = 0; segment < stops.size() - 1; segment++) {
            Stop from = stops.get(segment), to = stops.get(segment + 1);
            HueColor fromLab = HueColor.fromRgb(from.rgb()), toLab = HueColor.fromRgb(to.rgb());
            for (int step = segment == 0 ? 0 : 1; step < from.steps(); step++) {
                double t = step / (double) (from.steps() - 1);
                targets.add(new Target(channel(from.rgb(), to.rgb(), 16, t), channel(from.rgb(), to.rgb(), 8, t),
                        channel(from.rgb(), to.rgb(), 0, t), fromLab.mix(toLab, t),
                        step == 0 ? from.pinned() : step == from.steps() - 1 ? to.pinned() : null));
            }
        }
        return List.copyOf(targets);
    }
    private static double channel(int from, int to, int shift, double t) {
        return ((from >> shift) & 255) + (((to >> shift) & 255) - ((from >> shift) & 255)) * t;
    }
    public static double distance(Target target, Candidate candidate, boolean oklab) {
        int rgb = candidate.block().rgb();
        return oklab ? target.lab().distanceSquared(candidate.block().lab())
                : Math.abs(target.red() - ((rgb >> 16) & 255)) + Math.abs(target.green() - ((rgb >> 8) & 255))
                + Math.abs(target.blue() - (rgb & 255));
    }
    public static List<Candidate> alternatives(Target target, List<Candidate> candidates, boolean oklab) {
        return candidates.stream().sorted(java.util.Comparator.comparingDouble((Candidate c) -> distance(target, c, oklab))
                .thenComparing(Candidate::itemId).thenComparing(c -> c.block().texture())).toList();
    }
    public static List<Candidate> generate(List<Stop> stops, List<Candidate> candidates, boolean oklab) {
        List<Target> targets = targets(stops);
        if (candidates.isEmpty()) return List.of();
        List<Candidate> result = new ArrayList<>();
        for (Target target : targets) {
            Candidate nearest = null;
            double score = Double.POSITIVE_INFINITY;
            for (Candidate candidate : candidates) {
                double distance = distance(target, candidate, oklab);
                if (distance < score) { score = distance; nearest = candidate; }
            }
            if (target.pinned() != null && candidates.contains(target.pinned())) nearest = target.pinned();
            result.add(nearest);
        }
        return List.copyOf(result);
    }

    /** Display-only filtering by block item; nonadjacent repeats and original generated samples are preserved. */
    public static List<Candidate> preview(List<Candidate> samples, boolean hideConsecutive) {
        if (!hideConsecutive) return List.copyOf(samples);
        List<Candidate> visible = new ArrayList<>();
        for (Candidate sample : samples) {
            if (visible.isEmpty() || !visible.getLast().itemId().equals(sample.itemId())) visible.add(sample);
        }
        return List.copyOf(visible);
    }

}
