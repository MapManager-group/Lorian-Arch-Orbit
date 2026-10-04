package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Editable, memory-only gradient. Locks refer to sample positions, never item identity. */
public final class GradientWorkbench {
    public static final class Node {
        public String hex;
        public String steps;
        public String editedSteps;
        public HueGradient.Candidate pinned;
        public Node(String hex, String steps) { this.hex = hex; this.steps = steps; this.editedSteps = steps; }
    }
    public record Sample(int index, HueGradient.Target target, HueGradient.Candidate candidate, boolean locked) { }
    public final List<Node> nodes = new ArrayList<>(List.of(new Node("E0C9A2", "8"), new Node("4B5E73", "8")));
    public int selectedNode;
    public boolean oklab = true;
    public String face = "all";
    public String palette = "all";
    public GroupRef sourceGroup;
    public GroupRef targetGroup;
    public final java.util.Set<String> excluded = new java.util.HashSet<>();
    public record Comparison(List<Sample> samples, boolean oklab, String face, String palette) {
        public Comparison { samples = List.copyOf(samples); }
    }
    private Comparison comparison;
    private boolean generatedOklab = true;
    private String generatedFace = "all", generatedPalette = "all";
    public Comparison comparison() { return comparison; }
    public Comparison snapshot() { return new Comparison(samples, generatedOklab, generatedFace, generatedPalette); }
    public void keepComparison() { if (!samples.isEmpty()) comparison = snapshot(); }
    public void clearComparison() { comparison = null; }
    public boolean allows(HueGradient.Candidate candidate) { return !excluded.contains(candidate.key()); }
    public void exclude(HueGradient.Candidate candidate) { excluded.add(candidate.key()); invalidate(); }
    public void include(HueGradient.Candidate candidate) { if (excluded.remove(candidate.key())) invalidate(); }
    private final Map<Integer, HueGradient.Candidate> locks = new HashMap<>();
    private List<Sample> samples = List.of();
    private boolean stale = true;
    private long revision;
    private long appliedRevision;

    public List<Sample> samples() { return samples; }
    public boolean stale() { return stale; }
    public boolean unapplied() { return revision != appliedRevision || pendingSteps(); }
    public boolean pendingSteps() { return nodes.subList(0, nodes.size() - 1).stream().anyMatch(n -> !n.steps.equals(n.editedSteps)); }
    public boolean hasLocks() { return !locks.isEmpty(); }
    public void applied() { appliedRevision = revision; }
    public void invalidate() { stale = true; revision++; }
    public void setColor(String value) {
        Node node = nodes.get(selectedNode);
        if (!node.hex.equals(value)) { node.hex = value; node.pinned = null; invalidate(); }
    }
    public void setBlock(HueGradient.Candidate candidate) {
        Node node = nodes.get(selectedNode);
        node.hex = String.format(java.util.Locale.ROOT, "%06X", candidate.block().rgb()); node.pinned = candidate; invalidate();
    }
    public enum CountEdit { UNCHANGED, INVALID, CHANGED, CONFIRM_LOCK_LOSS }
    /** Valid text applies immediately. Incomplete text must not destroy samples or positional locks. */
    public CountEdit editSteps(Node node, String text, boolean discardLocks) {
        int value;
        try {
            value = Integer.parseInt(text);
            if (value < 2 || value > 128) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            node.editedSteps = text;
            return CountEdit.INVALID;
        }
        if (Integer.parseInt(node.steps) == value) {
            node.steps = node.editedSteps = text;
            return CountEdit.UNCHANGED;
        }
        if (hasLocks() && !discardLocks) return CountEdit.CONFIRM_LOCK_LOSS;
        structural(() -> node.steps = node.editedSteps = text);
        return CountEdit.CHANGED;
    }
    /** Called only after the UI has confirmed any lock loss. */
    public void structural(Runnable change) { change.run(); locks.clear(); samples = List.of(); invalidate(); }
    public void reverse() {
        var lengths = nodes.subList(0, nodes.size() - 1).stream().map(n -> n.steps).toList();
        structural(() -> {
            Collections.reverse(nodes);
            for (int i = 0; i < nodes.size() - 1; i++) {
                nodes.get(i).steps = lengths.get(lengths.size() - 1 - i);
                nodes.get(i).editedSteps = nodes.get(i).steps;
            }
            selectedNode = nodes.size() - 1 - selectedNode;
        });
    }
    public List<HueGradient.Stop> stops() {
        List<HueGradient.Stop> stops = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            String hex = node.hex.startsWith("#") ? node.hex.substring(1) : node.hex;
            if (!hex.matches("[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Invalid color");
            stops.add(new HueGradient.Stop(Integer.parseInt(hex, 16),
                    i == nodes.size() - 1 ? 2 : Integer.parseInt(node.steps), node.pinned));
        }
        return List.copyOf(stops);
    }
    public boolean valid() { try { stops(); return true; } catch (IllegalArgumentException ex) { return false; } }
    public int expectedCount() {
        var stops = stops(); return 1 + stops.subList(0, stops.size() - 1).stream().mapToInt(s -> s.steps() - 1).sum();
    }
    private static boolean same(HueGradient.Candidate a, HueGradient.Candidate b) {
        return a.itemId().equals(b.itemId()) && a.block().texture().equals(b.block().texture());
    }
    private static HueGradient.Candidate resolve(HueGradient.Candidate value, List<HueGradient.Candidate> available) {
        return available.stream().filter(c -> same(value, c)).findFirst().orElse(null);
    }
    public List<Integer> conflicts(List<HueGradient.Candidate> available) {
        return locks.entrySet().stream().filter(e -> resolve(e.getValue(), available) == null)
                .map(Map.Entry::getKey).sorted().toList();
    }
    public boolean generate(List<HueGradient.Candidate> available) {
        if (available.isEmpty() || !conflicts(available).isEmpty()) return false;
        List<HueGradient.Stop> stops = stops();
        var targets = HueGradient.targets(stops);
        var result = HueGradient.generate(stops, available, oklab);
        List<Sample> next = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) next.add(new Sample(i, targets.get(i),
                locks.containsKey(i) ? resolve(locks.get(i), available) : result.get(i), locks.containsKey(i)));
        samples = List.copyOf(next); generatedOklab = oklab; generatedFace = face; generatedPalette = palette;
        stale = false; revision++; return true;
    }
    public void replace(int index, HueGradient.Candidate value) {
        if (index < 0 || index >= samples.size()) return;
        locks.put(index, value); rebuildSample(index, value, true); revision++;
    }
    public void toggleLock(int index) {
        if (index < 0 || index >= samples.size()) return;
        Sample sample = samples.get(index);
        if (locks.remove(index) == null) locks.put(index, sample.candidate());
        rebuildSample(index, sample.candidate(), locks.containsKey(index)); revision++;
    }
    public void unlockAll() {
        locks.clear(); samples = samples.stream().map(s -> new Sample(s.index(), s.target(), s.candidate(), false)).toList(); revision++;
    }
    private void rebuildSample(int index, HueGradient.Candidate value, boolean locked) {
        var next = new ArrayList<>(samples); var sample = next.get(index);
        next.set(index, new Sample(index, sample.target(), value, locked)); samples = List.copyOf(next);
    }
    public List<HueGradient.Candidate> results() { return samples.stream().map(Sample::candidate).toList(); }
    public List<Sample> preview(boolean hideRepeats) {
        if (!hideRepeats) return samples;
        List<Sample> result = new ArrayList<>();
        for (Sample sample : samples) if (result.isEmpty() || !result.getLast().candidate().key().equals(sample.candidate().key())) result.add(sample);
        return List.copyOf(result);
    }
}
