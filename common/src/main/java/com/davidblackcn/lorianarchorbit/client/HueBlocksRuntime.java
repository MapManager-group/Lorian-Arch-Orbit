package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksData;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksRepository;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlockFaces;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.world.level.block.state.properties.Property;

/** Client-only registry mapping. Upstream texture colors are never recomputed from resource packs. */
final class HueBlocksRuntime {
    private static HueBlocksRepository repository;
    private static HueBlocksData mappedData;
    private static List<HueGradient.Candidate> candidates = List.of();
    private static final Map<String, List<HueBlockFaces.Surface>> surfaces = new TreeMap<>();
    private HueBlocksRuntime() {}

    static void initialize() {
        if (repository == null) {
            repository = new HueBlocksRepository(ClientConfigRuntime.configManager().directory(),
                    SharedConstants.getCurrentVersion().name());
            repository.refresh();
        }
    }

    static HueBlocksRepository repository() {
        initialize();
        return repository;
    }

    static List<HueGradient.Candidate> candidates() {
        if (net.minecraft.client.Minecraft.getInstance().level == null) return List.of();
        HueBlocksData data = repository().data();
        if (data == null) return List.of();
        if (data == mappedData) return candidates;
        List<HueGradient.Candidate> result = new ArrayList<>();
        if (surfaces.isEmpty()) loadFaces();
        var colors = new java.util.HashMap<String, HueBlocksData.Block>();
        data.blocks().forEach(block -> colors.put(block.texture(), block));
        surfaces.forEach((id, faces) -> faces.forEach(face -> {
            var color = colors.get(face.texture());
            if (color != null) result.add(new HueGradient.Candidate(id, color, face.faces(), face.placements()));
        }));
        // Prefer the whole block over stairs/fences when several items share the exact color.
        result.sort(java.util.Comparator.comparing((HueGradient.Candidate c) -> c.block().texture())
                .thenComparing(c -> !c.itemId().equals("minecraft:" + c.block().texture().replace(".png", "")))
                .thenComparingInt(c -> c.itemId().length()).thenComparing(HueGradient.Candidate::itemId));
        candidates = List.copyOf(result);
        mappedData = data;
        return candidates;
    }

    private static void loadFaces() {
        try (var input = HueBlocksRuntime.class.getResourceAsStream(
                "/assets/lorian_arch_orbit/hueblocks/block_faces.json")) {
            if (input == null) throw new IllegalStateException("Missing HueBlocks texture mapping");
            var mapping = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : mapping.entrySet()) {
                Identifier key = Identifier.parse(entry.getKey());
                if (!(BuiltInRegistries.ITEM.getValue(key) instanceof BlockItem item)) continue;
                var defaults = new TreeMap<String, String>();
                var rotations = new TreeMap<String, List<String>>();
                var state = item.getBlock().defaultBlockState();
                state.getValues().forEach(value -> defaults.put(value.property().getName(), value.valueName()));
                for (var property : state.getProperties()) if (java.util.Set.of("facing", "horizontal_facing", "axis").contains(property.getName()))
                    rotations.put(property.getName(), propertyValues(property));
                surfaces.put(entry.getKey(), HueBlockFaces.resolve(entry.getValue(), defaults, rotations));
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not load HueBlocks texture mapping", exception);
        }
    }

    private static <T extends Comparable<T>> List<String> propertyValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }
    static List<HueBlockFaces.Surface> surfaces(String itemId) {
        if (surfaces.isEmpty()) loadFaces();
        return surfaces.getOrDefault(itemId, List.of());
    }

    static ItemStack stack(HueGradient.Candidate candidate) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(candidate.itemId())).getDefaultInstance();
    }

    static void close() {
        if (repository != null) repository.close();
        repository = null;
        mappedData = null;
        candidates = List.of();
        surfaces.clear();
    }
}
