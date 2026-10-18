package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** Client-only active-resource database; never downloads or reads the legacy HueBlocks cache. */
public final class HueBlocksRuntime {
    private static final System.Logger LOGGER = System.getLogger("lorian_arch_orbit.local_colors");
    private static final HueBlocksRuntime INSTANCE = new HueBlocksRuntime();
    private static final LocalHueState<Snapshot> STATE = new LocalHueState<>();
    private static final ExecutorService MANUAL = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "lorian-local-colors"); thread.setDaemon(true); return thread;
    });
    private static volatile ResourceManager resources;
    private record Snapshot(HueBlocksData data, Map<String, List<HueBlockFaces.Surface>> surfaces,
                            List<HueGradient.Candidate> candidates, Map<String, Integer> skipped) { }
    private record Prepared(Snapshot snapshot, LocalColorCache cache, Path path) { }
    private HueBlocksRuntime() { }
    static void initialize() { /* The platform resource listener owns initial loading. */ }
    static HueBlocksRuntime repository() { return INSTANCE; }
    public HueBlocksData data() { var snapshot = STATE.value(); return snapshot == null ? null : snapshot.data(); }
    public boolean checking() { return STATE.checking(); }
    public boolean failed() { return STATE.failed(); }
    public Map<String, Integer> skipped() { var snapshot = STATE.value(); return snapshot == null ? Map.of() : snapshot.skipped(); }

    public static PreparableReloadListener reloadListener() {
        return (shared, prepareExecutor, barrier, applyExecutor) -> {
            long token = STATE.begin(true);
            ResourceManager manager = shared.resourceManager(); resources = manager;
            return CompletableFuture.supplyAsync(() -> prepare(manager), prepareExecutor)
                    .handle((prepared, error) -> {
                        if (error != null) { STATE.fail(token); LOGGER.log(System.Logger.Level.WARNING, "Local color resource scan failed", error); }
                        return prepared;
                    }).thenCompose(barrier::wait).thenAcceptAsync(prepared -> publish(token, prepared), applyExecutor);
        };
    }

    public void refresh() {
        ResourceManager manager = resources;
        if (manager == null || checking()) return;
        long token = STATE.begin(false);
        CompletableFuture.supplyAsync(() -> prepare(manager), MANUAL).whenCompleteAsync((prepared, error) -> {
            if (error != null) { STATE.fail(token); LOGGER.log(System.Logger.Level.WARNING, "Local color recalculation failed", error); }
            else publish(token, prepared);
        }, Minecraft.getInstance());
    }

    private static void publish(long token, Prepared prepared) {
        if (prepared == null || !STATE.publish(token, prepared.snapshot())) return;
        var snapshot = prepared.snapshot();
        LOGGER.log(System.Logger.Level.INFO, "Local colors ready: {0} textures, {1} items, {2} candidates; skipped {3}",
                snapshot.data().blocks().size(), snapshot.candidates().stream().map(HueGradient.Candidate::itemId).distinct().count(),
                snapshot.candidates().size(), snapshot.skipped());
        // Only content-addressed colors are cached; an older save cannot return stale colors.
        MANUAL.execute(() -> {
            try { prepared.cache().save(prepared.path()); }
            catch (IOException ex) { LOGGER.log(System.Logger.Level.WARNING, "Could not save local color cache; in-memory data remains usable", ex); }
        });
    }

    private static Prepared prepare(ResourceManager manager) {
        Path path = ClientConfigRuntime.configManager().directory().resolve("local-colors")
                .resolve(SharedConstants.getCurrentVersion().name() + ".json");
        LocalColorCache cache = new LocalColorCache(path);
        Map<String, List<HueBlockFaces.Surface>> surfaces = new TreeMap<>();
        Map<String, Integer> skipped = new TreeMap<>();
        LocalModelFaces parser = new LocalModelFaces(id -> {
            try (var input = manager.open(Identifier.parse(id))) {
                return JsonParser.parseString(new String(readBounded(input, 4 * 1024 * 1024), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        });
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            if (!(entry.getValue() instanceof BlockItem item)) continue;
            String itemId = entry.getKey().identifier().toString();
            var blockId = BuiltInRegistries.BLOCK.getKey(item.getBlock());
            try (var input = manager.open(Identifier.fromNamespaceAndPath(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json"))) {
                var json = JsonParser.parseString(new String(readBounded(input, 4 * 1024 * 1024), StandardCharsets.UTF_8)).getAsJsonObject();
                var defaults = new TreeMap<String, String>(); var rotations = new TreeMap<String, List<String>>();
                var state = item.getBlock().defaultBlockState();
                state.getValues().forEach(value -> defaults.put(value.property().getName(), value.valueName()));
                for (var property : state.getProperties()) if (Set.of("facing", "horizontal_facing", "axis").contains(property.getName()))
                    rotations.put(property.getName(), propertyValues(property));
                var resolved = HueBlockFaces.resolve(parser.extract(json), defaults, rotations);
                if (resolved.isEmpty()) skipped.merge("no_standard_faces", 1, Integer::sum);
                else surfaces.put(itemId, resolved);
            } catch (IOException | RuntimeException ex) {
                skipped.merge("model", 1, Integer::sum);
                LOGGER.log(System.Logger.Level.DEBUG, "Skipping model " + itemId + ": " + ex.getMessage());
            }
        }
        parser.skipped().forEach((key, count) -> skipped.merge(key, count, Integer::sum));
        Map<String, Set<String>> textureFaces = new TreeMap<>();
        surfaces.values().forEach(list -> list.forEach(surface -> textureFaces.computeIfAbsent(surface.texture(), key -> new TreeSet<>()).addAll(surface.faces())));
        Map<String, HueBlocksData.Block> colors = new TreeMap<>(); Set<String> opaque = new HashSet<>();
        textureFaces.forEach((texture, faces) -> {
            Identifier sprite = Identifier.parse(texture);
            try {
                Resource resource = manager.getResourceOrThrow(Identifier.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"));
                var section = resource.metadata().getSection(AnimationMetadataSection.TYPE);
                JsonObject animation = section.isEmpty() ? null : AnimationMetadataSection.CODEC.encodeStart(JsonOps.INSTANCE, section.get()).getOrThrow().getAsJsonObject();
                LocalTextureColor.Color color;
                try (var input = resource.open()) { color = cache.color(readBounded(input, 32 * 1024 * 1024), animation); }
                colors.put(texture, new HueBlocksData.Block(texture, color.rgb(), HueColor.fromRgb(color.rgb()), faces));
                if (color.opaque()) opaque.add(texture);
            } catch (IOException | RuntimeException ex) {
                skipped.merge("texture", 1, Integer::sum);
                LOGGER.log(System.Logger.Level.DEBUG, "Skipping texture " + texture + ": " + ex.getMessage());
            }
        });
        List<HueGradient.Candidate> candidates = new ArrayList<>();
        surfaces.forEach((id, faces) -> faces.forEach(face -> {
            var color = colors.get(face.texture());
            if (color != null) candidates.add(new HueGradient.Candidate(id, color, face.faces(), face.placements()));
        }));
        candidates.sort(Comparator.comparing((HueGradient.Candidate c) -> c.block().texture())
                .thenComparing(c -> !c.itemId().equals(c.block().texture().replace(":block/", ":")))
                .thenComparingInt(c -> c.itemId().length()).thenComparing(HueGradient.Candidate::itemId));
        var data = new HueBlocksData(SharedConstants.getCurrentVersion().name(), "local", List.copyOf(colors.values()), palettes(colors.keySet(), opaque));
        return new Prepared(new Snapshot(data, Map.copyOf(surfaces), List.copyOf(candidates), Map.copyOf(skipped)), cache, path);
    }

    private static List<HueBlocksData.Palette> palettes(Set<String> available, Set<String> opaque) {
        try (var input = HueBlocksRuntime.class.getResourceAsStream("/assets/lorian_arch_orbit/hueblocks/local_palettes.json")) {
            if (input == null) throw new IOException("Missing local palette definitions");
            var array = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray();
            List<HueBlocksData.Palette> result = new ArrayList<>();
            for (var entry : array) {
                if (!entry.isJsonObject() || !entry.getAsJsonObject().has("textures")) continue;
                var object = entry.getAsJsonObject(); String name = object.get("name").getAsString(); Set<String> textures = new TreeSet<>();
                for (var texture : object.getAsJsonArray("textures")) {
                    String id = HueResourceId.texture(texture.getAsString());
                    if (available.contains(id) && (!name.equals("Default (opaque only)") || opaque.contains(id))) textures.add(id);
                }
                result.add(new HueBlocksData.Palette(name, textures));
            }
            return List.copyOf(result);
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
    }
    private static byte[] readBounded(InputStream input, int limit) throws IOException {
        byte[] bytes = input.readNBytes(limit + 1);
        if (bytes.length > limit) throw new IOException("Resource too large");
        return bytes;
    }
    private static <T extends Comparable<T>> List<String> propertyValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }
    static List<HueGradient.Candidate> candidates() {
        var snapshot = STATE.value();
        return snapshot == null || Minecraft.getInstance().level == null ? List.of() : snapshot.candidates();
    }
    static List<HueBlockFaces.Surface> surfaces(String itemId) {
        var snapshot = STATE.value(); return snapshot == null ? List.of() : snapshot.surfaces().getOrDefault(itemId, List.of());
    }
    static ItemStack stack(HueGradient.Candidate candidate) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(candidate.itemId())).getDefaultInstance();
    }
    static void close() { STATE.begin(true); resources = null; MANUAL.shutdownNow(); }
}
