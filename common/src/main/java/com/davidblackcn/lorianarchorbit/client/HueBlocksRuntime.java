package com.davidblackcn.lorianarchorbit.client;

import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksData;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueBlocksRepository;
import com.davidblackcn.lorianarchorbit.palette.hueblocks.HueGradient;
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

/** Client-only registry mapping. Upstream texture colors are never recomputed from resource packs. */
final class HueBlocksRuntime {
    private static HueBlocksRepository repository;
    private static HueBlocksData mappedData;
    private static List<HueGradient.Candidate> candidates = List.of();
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
        try (var input = HueBlocksRuntime.class.getResourceAsStream(
                "/assets/lorian_arch_orbit/hueblocks/texture_blocks.json")) {
            if (input == null) throw new IllegalStateException("Missing HueBlocks texture mapping");
            var mapping = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (HueBlocksData.Block block : data.blocks()) {
                if (!mapping.has(block.texture())) continue;
                for (var member : mapping.getAsJsonArray(block.texture())) {
                    String id = member.getAsString();
                    Identifier key = Identifier.parse(id);
                    if (BuiltInRegistries.ITEM.containsKey(key) && BuiltInRegistries.ITEM.getValue(key) instanceof BlockItem) {
                        result.add(new HueGradient.Candidate(id, block));
                    }
                }
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not load HueBlocks texture mapping", exception);
        }
        candidates = List.copyOf(result);
        mappedData = data;
        return candidates;
    }

    static ItemStack stack(HueGradient.Candidate candidate) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(candidate.itemId())).getDefaultInstance();
    }

    static void close() {
        if (repository != null) repository.close();
        repository = null;
        mappedData = null;
        candidates = List.of();
    }
}
