package com.davidblackcn.lorianarchorbit.palette.hueblocks;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class LocalHueTest {
    private static JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private static BufferedImage image(int... pixels) {
        var image = new BufferedImage(1, pixels.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < pixels.length; y++) image.setRGB(0, y, pixels[y]); return image;
    }
    private static byte[] png(int... pixels) throws IOException {
        var out = new ByteArrayOutputStream(); ImageIO.write(image(pixels), "png", out); return out.toByteArray();
    }
    @Test void meanIsLinearAndTransparentPixelsDoNotPolluteColor() {
        assertEquals(0xBCBCBC, LocalTextureColor.calculate(image(0xFF000000, 0xFFFFFFFF), null).rgb());
        var color = LocalTextureColor.calculate(image(0x00FF0000, 0x800000FF), null);
        assertEquals(0x0000FF, color.rgb()); assertFalse(color.opaque());
        assertThrows(IllegalArgumentException.class, () -> LocalTextureColor.calculate(image(0x00FFFFFF), null));
    }
    @Test void animationUsesFrameSelectionRepeatedFramesAndDuration() {
        var pixels = image(0xFF000000, 0xFFFFFFFF, 0xFFFF0000);
        var animation = json("{\"frames\":[0,{\"index\":1,\"time\":3}]}");
        assertEquals(0xE1E1E1, LocalTextureColor.calculate(pixels, animation).rgb());
        assertEquals(0xFFFFFF, LocalTextureColor.calculate(pixels, json("{\"frames\":[1,1]}")).rgb());
        assertThrows(IllegalArgumentException.class, () -> LocalTextureColor.calculate(pixels, json("{\"frames\":[3]}")));
        assertThrows(IllegalArgumentException.class, () -> LocalTextureColor.calculate(pixels, json("{\"frametime\":0}")));
        assertThrows(IllegalArgumentException.class, () -> LocalTextureColor.calculate(pixels, json("{\"height\":2}")));
    }
    @Test void cacheFollowsBytesAndAnimationNotPackName(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("colors.json"); byte[] pixels = png(0xFF000000, 0xFFFFFFFF);
        var cache = new LocalColorCache(file);
        assertEquals(0xBCBCBC, cache.color(pixels, null).rgb()); cache.save(file);
        var reopened = new LocalColorCache(file);
        assertEquals(0xBCBCBC, reopened.color(pixels, null).rgb());
        assertEquals(0x000000, reopened.color(pixels, json("{\"frames\":[0]}")).rgb());
        assertEquals(0xFF0000, reopened.color(png(0xFFFF0000), null).rgb());
        assertNotEquals(LocalColorCache.fingerprint(pixels, null), LocalColorCache.fingerprint(pixels, json("{}")));
        Files.writeString(file, "corrupt");
        assertEquals(0xFFFFFF, new LocalColorCache(file).color(png(0xFFFFFFFF), null).rgb());
        assertThrows(IOException.class, () -> cache.color(new byte[]{1,2,3}, null));
    }
    @Test void oldWorkCannotPublishOverNewResourcesAndManualFailuresKeepValidData() {
        var state = new LocalHueState<String>(); long a = state.begin(true); state.publish(a, "old");
        long manual = state.begin(false); state.fail(manual); assertEquals("old", state.value()); assertTrue(state.failed());
        long next = state.begin(true); assertNull(state.value());
        assertFalse(state.publish(manual, "stale")); state.fail(manual); assertTrue(state.checking());
        assertTrue(state.publish(next, "new")); assertFalse(state.failed()); assertEquals("new", state.value());
    }
    @Test void modelsResolveParentTextureOverridesNamespacesMultipartAndRotation() throws Exception {
        Map<String, JsonObject> assets = new HashMap<>();
        assets.put("test:models/block/base.json", json("{\"textures\":{\"all\":\"test:block/old\"},\"elements\":[{\"faces\":{\"north\":{\"texture\":\"#all\"}}}]}"));
        assets.put("test:models/block/new.json", json("{\"parent\":\"test:block/base\",\"textures\":{\"all\":\"test:block/folder/new\"}}"));
        var parser = new LocalModelFaces(id -> { if (!assets.containsKey(id)) throw new IOException(id); return assets.get(id); });
        var entries = parser.extract(json("{\"multipart\":[{\"when\":{\"axis\":\"x\"},\"apply\":{\"model\":\"test:block/new\",\"y\":90}}]}"));
        var faces = HueBlockFaces.resolve(entries, Map.of("axis", "y"), Map.of("axis", List.of("x","y","z")));
        assertEquals(1, faces.size()); assertEquals("test:block/folder/new", faces.getFirst().texture());
        assertEquals(Set.of("east"), faces.getFirst().faces()); assertTrue(faces.getFirst().defaultFaces().isEmpty());
        assertEquals("axis=x", faces.getFirst().placements().get("east"));
    }
    @Test void tintedOverlayRemovesUnderlyingFaceAndCyclesAreRejected() throws Exception {
        var assets = new HashMap<String, JsonObject>();
        assets.put("minecraft:models/block/grass.json", json("{\"elements\":[{\"faces\":{\"north\":{\"texture\":\"block/dirt\"},\"down\":{\"texture\":\"block/dirt\"}}},{\"faces\":{\"north\":{\"texture\":\"block/overlay\",\"tintindex\":0}}}]}"));
        assets.put("minecraft:models/block/cycle.json", json("{\"parent\":\"block/cycle\"}"));
        var parser = new LocalModelFaces(id -> { if (!assets.containsKey(id)) throw new IOException(id); return assets.get(id); });
        var result = parser.extract(json("{\"variants\":{\"\":{\"model\":\"block/grass\"}}}"));
        var faces = HueBlockFaces.resolve(result, Map.of(), Map.of());
        assertEquals(Set.of("bottom"), faces.getFirst().faces()); assertEquals(1, parser.skipped().get("tinted"));
        assertThrows(IOException.class, () -> parser.extract(json("{\"variants\":{\"\":{\"model\":\"block/cycle\"}}}")));
    }
    @Test void differentNamespacesNeverCollideAndLegacyRecipeReferencesUpgrade() {
        assertNotEquals(HueResourceId.texture("a:block/stone"), HueResourceId.texture("b:block/stone"));
        assertEquals("minecraft:stone|minecraft:block/stone", HueResourceId.candidate("minecraft:stone|stone.png"));
        assertThrows(IllegalArgumentException.class, () -> HueResourceId.texture("a:../secret"));
        var model = new GradientWorkbench();
        var root = JsonParser.parseString(GradientRecipe.encode(model, false)).getAsJsonObject();
        assertEquals(2, root.get("version").getAsInt()); root.addProperty("version", 1);
        root.getAsJsonArray("nodes").get(0).getAsJsonObject().addProperty("candidate", "minecraft:stone|stone.png");
        root.getAsJsonArray("excluded").add("minecraft:dirt|dirt.png");
        var recipe = GradientRecipe.decode(root.toString());
        assertEquals("minecraft:stone|minecraft:block/stone", recipe.nodes().getFirst().candidate());
        assertEquals(Set.of("minecraft:dirt|minecraft:block/dirt"), recipe.excluded());
    }
    @Test void resourceChangesRebindNodesInvalidateResultsAndPreserveComparisonAndMissingLocks() {
        var old = candidate(0x112233); var replacement = candidate(0xAABBCC);
        var model = new GradientWorkbench(); model.bindResources(new Object(), List.of(old)); model.setBlock(old);
        model.generate(List.of(old)); model.toggleLock(1); model.keepComparison(); var comparison = model.comparison();
        model.bindResources(new Object(), List.of(replacement));
        assertEquals("AABBCC", model.nodes.getFirst().hex); assertTrue(model.stale());
        assertEquals(comparison, model.comparison()); assertEquals(old, model.comparison().samples().getFirst().candidate());
        assertTrue(model.conflicts(List.of(replacement)).isEmpty()); assertEquals(List.of(1), model.conflicts(List.of()));
        assertTrue(model.generate(List.of(replacement))); assertEquals(replacement, model.samples().get(1).candidate());
    }
    private static HueGradient.Candidate candidate(int rgb) {
        return new HueGradient.Candidate("test:new_block", new HueBlocksData.Block("test:block/new", rgb, HueColor.fromRgb(rgb), Set.of("north")));
    }

    @Test void selectingFromAnOldPopupRebindsEvenWithinTheSameResourceSnapshot() {
        var model = new GradientWorkbench(); var snapshot = new Object(); var current = candidate(0xFFFFFF);
        model.bindResources(snapshot, List.of(current)); model.setBlock(candidate(0));
        model.bindResources(snapshot, List.of(current));
        assertEquals(current, model.nodes.getFirst().pinned); assertEquals("FFFFFF", model.nodes.getFirst().hex);
        model.bindResources(new Object(), List.of());
        assertEquals("FFFFFF", model.nodes.getFirst().hex); assertEquals(current, model.nodes.getFirst().pinned);
    }
}
