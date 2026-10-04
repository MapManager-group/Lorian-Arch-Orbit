package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.davidblackcn.lorianarchorbit.palette.WheelEditorState.GroupRef;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GradientRecipeTest {
    private final HueGradient.Candidate candidate = new HueGradient.Candidate("minecraft:stone",
            new HueBlocksData.Block("stone.png", 0x808080, HueColor.fromRgb(0x808080), Set.of("north")));
    @Test void parametersAndExclusionsRoundTripWithoutResultsOrLocks() {
        var source = new GradientWorkbench(); source.setBlock(candidate); source.oklab = false; source.face = "north";
        source.palette = "group"; source.sourceGroup = new GroupRef(false, "a-stable-id");
        source.excluded.add("minecraft:granite|granite.png"); source.generate(List.of(candidate)); source.replace(2, candidate);
        var recipe = GradientRecipe.decode(GradientRecipe.encode(source, false));
        var target = new GradientWorkbench(); recipe.apply(target, List.of(candidate));
        assertEquals(candidate, target.nodes.getFirst().pinned); assertEquals("808080", target.nodes.getFirst().hex);
        assertEquals(source.sourceGroup, target.sourceGroup); assertEquals(source.excluded, target.excluded);
        assertFalse(target.oklab); assertEquals("north", target.face); assertTrue(target.samples().isEmpty());
        assertFalse(target.hasLocks()); assertTrue(target.unapplied());
    }
    @Test void paletteImportPreservesNodesAndLocksAndExposesConflicts() {
        var source = new GradientWorkbench(); source.exclude(candidate);
        var target = new GradientWorkbench(); target.generate(List.of(candidate)); target.replace(2, candidate);
        var nodes = List.copyOf(target.nodes); var samples = target.samples();
        GradientRecipe.decode(GradientRecipe.encode(source, true)).apply(target, List.of(candidate));
        assertEquals(nodes, target.nodes); assertEquals(samples, target.samples()); assertTrue(target.stale());
        assertEquals(List.of(2), target.conflicts(List.of())); assertFalse(target.allows(candidate));
    }
    @Test void missingPinnedTextureIsReportedAndRetainsExplicitColorOnAcceptedImport() {
        var state = new GradientWorkbench(); state.setBlock(candidate);
        var recipe = GradientRecipe.decode(GradientRecipe.encode(state, false));
        assertEquals(List.of(candidate.key()), recipe.missing(List.of()));
        var target = new GradientWorkbench(); recipe.apply(target, List.of());
        assertEquals("808080", target.nodes.getFirst().hex); assertNull(target.nodes.getFirst().pinned);
    }
    @Test void invalidFilesNeverMutateCurrentState() {
        var state = new GradientWorkbench(); state.generate(List.of(candidate)); state.replace(1, candidate);
        var before = state.samples(); String text = GradientRecipe.encode(state, false);
        for (String field : List.of("version", "face", "nodes", "excluded", "oklab")) {
            var root = JsonParser.parseString(text).getAsJsonObject(); root.addProperty(field, "invalid");
            assertThrows(IllegalArgumentException.class, () -> GradientRecipe.decode(root.toString()));
        }
        assertEquals(before, state.samples()); assertTrue(state.hasLocks());
        assertThrows(IllegalArgumentException.class, () -> GradientRecipe.decode("x".repeat(GradientRecipe.MAX_LENGTH + 1)));
    }
    @Test void invalidCountsAndTooManyNodesAreRejected() {
        String text = GradientRecipe.encode(new GradientWorkbench(), false);
        for (String steps : List.of("1", "129", "2.5", "NaN")) {
            var root = JsonParser.parseString(text).getAsJsonObject(); root.getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("steps", steps);
            assertThrows(IllegalArgumentException.class, () -> GradientRecipe.decode(root.toString()));
        }
        var root = JsonParser.parseString(text).getAsJsonObject();
        for (int i = 0; i < 15; i++) root.getAsJsonArray("nodes").add(root.getAsJsonArray("nodes").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> GradientRecipe.decode(root.toString()));
    }
    @Test void filesAreSeparateAndNeverOverwriteExistingPlans(@TempDir Path directory) throws Exception {
        String text = GradientRecipe.encode(new GradientWorkbench(), false);
        Path a = GradientRecipeFiles.save(directory, text), b = GradientRecipeFiles.save(directory, text);
        assertNotEquals(a, b); assertEquals(2, GradientRecipeFiles.list(directory).size());
        assertEquals(2, GradientRecipeFiles.read(directory, a).nodes().size());
        Path outside = directory.resolve("outside.json"); Files.writeString(outside, text);
        assertThrows(java.io.IOException.class, () -> GradientRecipeFiles.read(directory, outside));
        Path oversized = GradientRecipeFiles.directory(directory).resolve("oversized.json"); Files.writeString(oversized, "x".repeat(GradientRecipe.MAX_LENGTH + 1));
        assertThrows(java.io.IOException.class, () -> GradientRecipeFiles.read(directory, oversized));
    }
    @Test void comparisonSnapshotsKeepOriginalMetadataAndCannotFollowFurtherEdits() {
        var state = new GradientWorkbench(); state.generate(List.of(candidate)); state.keepComparison();
        var snapshot = state.comparison(); state.oklab = false; state.face = "top"; state.invalidate();
        state.keepComparison(); assertTrue(state.comparison().oklab()); assertEquals("all", state.comparison().face());
        state.replace(0, candidate); state.generate(List.of(candidate));
        assertEquals(snapshot, state.comparison()); assertFalse(state.snapshot().oklab());
        assertFalse(snapshot.samples().getFirst().locked()); assertTrue(state.samples().getFirst().locked());
    }
    @Test void differentFaceTexturesOfOneItemRemainSeparateInPreview() {
        var other = new HueGradient.Candidate(candidate.itemId(), new HueBlocksData.Block("other.png", 0, HueColor.fromRgb(0), Set.of("top")));
        var state = new GradientWorkbench(); state.generate(List.of(candidate)); state.replace(2, other);
        assertEquals(3, state.preview(true).size()); assertEquals(other, state.preview(true).get(1).candidate());
        assertEquals(8, state.results().size());
    }
}
