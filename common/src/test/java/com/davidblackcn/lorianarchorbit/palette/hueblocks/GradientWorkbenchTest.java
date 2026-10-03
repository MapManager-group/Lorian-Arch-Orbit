package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class GradientWorkbenchTest {
    private static HueGradient.Candidate block(String id, int color) {
        return new HueGradient.Candidate(id, new HueBlocksData.Block(id + ".png", color, HueColor.fromRgb(color), Set.of("top")));
    }
    private final HueGradient.Candidate black = block("black", 0), gray = block("gray", 0x888888), white = block("white", 0xFFFFFF);
    private final List<HueGradient.Candidate> candidates = List.of(black, gray, white);
    @Test void repeatedItemsHaveIndependentPositionLocksAndSurviveRegeneration() {
        var state = new GradientWorkbench();
        assertFalse(state.unapplied()); assertTrue(state.generate(List.of(black)));
        assertEquals(8, state.samples().size()); assertEquals(1, state.preview(true).size());
        state.replace(3, white); assertTrue(state.samples().get(3).locked()); assertFalse(state.samples().get(2).locked());
        state.setColor("AABBCC"); assertTrue(state.stale()); assertTrue(state.generate(candidates));
        assertEquals(white, state.samples().get(3).candidate());
        assertEquals(3, state.samples().get(3).index());
        state.applied(); assertFalse(state.unapplied());
        state.toggleLock(3); assertTrue(state.unapplied()); assertFalse(state.hasLocks());
    }
    @Test void filtersRejectConflictingLocksWithoutMutatingResults() {
        var state = new GradientWorkbench(); state.generate(candidates); state.replace(1, white);
        var before = state.samples();
        assertEquals(List.of(1), state.conflicts(List.of(black)));
        assertFalse(state.generate(List.of(black))); assertEquals(before, state.samples());
        state.replace(1, black); assertTrue(state.generate(List.of(black)));
        state.unlockAll(); assertFalse(state.hasLocks());
    }
    @Test void refreshedColorDataResolvesLocksByItemAndTextureIdentity() {
        var state = new GradientWorkbench(); state.generate(candidates); state.replace(2, white);
        var refreshed = block("white", 0xEEEEEE);
        assertTrue(state.conflicts(List.of(refreshed)).isEmpty()); assertTrue(state.generate(List.of(refreshed)));
        assertEquals(refreshed, state.samples().get(2).candidate());
    }
    @Test void structuralChangesClearPositionLocksAndReverseSharedSegments() {
        var state = new GradientWorkbench();
        state.structural(() -> state.nodes.add(new GradientWorkbench.Node("000000", "4")));
        state.nodes.get(0).steps = state.nodes.get(0).editedSteps = "8";
        state.nodes.get(1).steps = state.nodes.get(1).editedSteps = "6";
        assertEquals(13, state.expectedCount()); state.generate(candidates); state.replace(1, white);
        state.reverse(); assertFalse(state.hasLocks()); assertTrue(state.samples().isEmpty());
        assertEquals("6", state.nodes.get(0).steps); assertEquals("8", state.nodes.get(1).steps);
        assertEquals("000000", state.nodes.get(0).hex); assertEquals(13, state.expectedCount());
        assertEquals(2, state.selectedNode);
    }
    @Test void changingCountsProtectsLockedPositionsUntilLockLossIsAccepted() {
        var state = new GradientWorkbench(); state.generate(candidates); state.replace(6, white); state.applied();
        assertEquals(GradientWorkbench.CountEdit.CONFIRM_LOCK_LOSS,state.editSteps(state.nodes.getFirst(),"3",false));
        assertFalse(state.pendingSteps()); assertFalse(state.unapplied()); assertEquals(8, state.expectedCount());
        assertTrue(state.samples().get(6).locked());
        assertEquals(GradientWorkbench.CountEdit.CHANGED,state.editSteps(state.nodes.getFirst(),"3",true));
        assertEquals(3,state.expectedCount()); assertFalse(state.hasLocks()); assertTrue(state.samples().isEmpty());
    }
    @Test void validCountInputAppliesImmediatelyAndInvalidIntermediateTextPreservesResults() {
        var state=new GradientWorkbench(); state.generate(candidates); state.applied();
        var before=state.samples();
        for(String invalid:new String[]{"","1","129","abc"}) {
            assertEquals(GradientWorkbench.CountEdit.INVALID,state.editSteps(state.nodes.getFirst(),invalid,false));
            assertTrue(state.pendingSteps()); assertEquals(before,state.samples()); assertEquals(8,state.expectedCount());
        }
        assertEquals(GradientWorkbench.CountEdit.CHANGED,state.editSteps(state.nodes.getFirst(),"12",false));
        assertEquals(12,state.expectedCount()); assertFalse(state.pendingSteps()); assertTrue(state.stale());
        assertTrue(state.generate(candidates)); assertEquals(12,state.samples().size());
        state.applied();
        assertEquals(GradientWorkbench.CountEdit.UNCHANGED,state.editSteps(state.nodes.getFirst(),"012",false));
        assertFalse(state.unapplied()); assertEquals(12,state.samples().size());
    }
    @Test void alternativesUseTheSameMetricAsGenerationAndTiesAreStable() {
        var stops = List.of(new HueGradient.Stop(0, 3, null), new HueGradient.Stop(0xFFFFFF, 2, null));
        var targets = HueGradient.targets(stops);
        for (boolean oklab : new boolean[]{true, false}) {
            var result = HueGradient.generate(stops, candidates, oklab);
            for (int i = 0; i < result.size(); i++) assertEquals(result.get(i), HueGradient.alternatives(targets.get(i), candidates, oklab).getFirst());
        }
        var twin = block("aaa", 0);
        assertEquals(twin, HueGradient.alternatives(targets.getFirst(), List.of(black, twin), true).getFirst());
    }
    @Test void previewCompressionDoesNotChangeOriginalIndicesOrApplications() {
        var state = new GradientWorkbench(); state.generate(List.of(black)); state.replace(3, white);
        assertEquals(List.of(0, 3, 4), state.preview(true).stream().map(GradientWorkbench.Sample::index).toList());
        assertEquals(8, state.results().size()); assertEquals(8, state.preview(false).size());
        state.applied(); state.setColor("BAD"); assertTrue(state.unapplied()); assertFalse(state.valid());
    }
    @Test void displayedTargetsRoundTripRgbAndFollowTheChosenColorSpace() {
        for (int r = 0; r <= 255; r += 17) for (int g = 0; g <= 255; g += 17) for (int b = 0; b <= 255; b += 17) {
            int rgb = r << 16 | g << 8 | b;
            assertEquals(rgb, HueColor.fromRgb(rgb).toRgb());
        }
        var target = HueGradient.targets(List.of(new HueGradient.Stop(0, 3, null), new HueGradient.Stop(0xFFFFFF, 2, null))).get(1);
        assertEquals(0x808080, target.displayRgb(false));
        assertNotEquals(target.displayRgb(false), target.displayRgb(true));
    }
}
