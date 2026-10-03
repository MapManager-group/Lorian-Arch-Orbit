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

    public static List<Candidate> generate(List<Stop> stops, List<Candidate> candidates, boolean oklab) {
        if (stops.size() < 2 || stops.size() > MAX_STOPS) throw new IllegalArgumentException("Use 2–16 stops");
        if (candidates.isEmpty()) return List.of();
        List<Candidate> result = new ArrayList<>();
        for (int segment = 0; segment < stops.size() - 1; segment++) {
            Stop from = stops.get(segment);
            Stop to = stops.get(segment + 1);
            HueColor fromLab = HueColor.fromRgb(from.rgb());
            HueColor toLab = HueColor.fromRgb(to.rgb());
            for (int step = segment == 0 ? 0 : 1; step < from.steps(); step++) {
                double t = step / (double) (from.steps() - 1);
                HueColor target = fromLab.mix(toLab, t);
                Candidate nearest = null;
                double score = Double.POSITIVE_INFINITY;
                for (Candidate candidate : candidates) {
                    double distance = oklab ? target.distanceSquared(candidate.block().lab())
                            : rgbDistance(from.rgb(), to.rgb(), candidate.block().rgb(), t);
                    if (distance < score) {
                        score = distance;
                        nearest = candidate;
                    }
                }
                // Keep selected block endpoints when they satisfy the same filters as candidates.
                Candidate pin = step == 0 ? from.pinned() : step == from.steps() - 1 ? to.pinned() : null;
                if (pin != null && candidates.contains(pin)) nearest = pin;
                result.add(nearest);
            }
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

    private static double rgbDistance(int from, int to, int candidate, double t) {
        double score = 0;
        for (int shift : new int[]{16, 8, 0}) {
            double start = (from >> shift) & 255;
            double end = (to >> shift) & 255;
            score += Math.abs(start + (end - start) * t - ((candidate >> shift) & 255));
        }
        return score;
    }
}
