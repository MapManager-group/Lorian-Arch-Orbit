package com.davidblackcn.lorianarchorbit.palette;

import com.davidblackcn.lorianarchorbit.config.PalettePreset;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BuiltinPalettePresetsTest {
    @Test
    void itemTagAOrganizesTheSameBlockFormAcrossWoodTypes() {
        List<PaletteGroup> groups = BuiltinPalettePresets.groups(PalettePreset.ITEM_TAG_A);
        PaletteGroup planks = group(groups, "builtin_wood_form_planks");

        assertEquals(50, groups.size());
        assertContains(planks, "minecraft:oak_planks");
        assertContains(planks, "minecraft:birch_planks");
        assertContains(planks, "minecraft:acacia_planks");
        assertContains(planks, "minecraft:bamboo_planks");
        assertContains(planks, "minecraft:crimson_planks");
        assertContains(planks, "minecraft:warped_planks");
        assertContains(group(groups, "builtin_color_material_concrete"), "minecraft:red_concrete");
        assertContains(group(groups, "builtin_color_material_stained_glass"), "minecraft:glass");
        assertContains(group(groups, "builtin_color_material_terracotta"), "minecraft:terracotta");
        assertContains(group(groups, "builtin_color_material_candles"), "minecraft:candle");
        assertContains(group(groups, "builtin_color_material_beds"), "minecraft:red_bed");
        assertContains(group(groups, "builtin_color_material_banners"), "minecraft:blue_banner");
        assertContains(group(groups, "builtin_building_stone"), "minecraft:polished_diorite");
    }

    @Test
    void itemTagBOrganizesEveryBlockInTheSameWoodFamily() {
        List<PaletteGroup> groups = BuiltinPalettePresets.groups(PalettePreset.ITEM_TAG_B);
        PaletteGroup oak = group(groups, "builtin_wood_family_oak");

        assertEquals(47, groups.size());
        assertContains(oak, "minecraft:oak_log");
        assertContains(oak, "minecraft:oak_planks");
        assertContains(oak, "minecraft:oak_fence");
        assertContains(oak, "minecraft:oak_shelf");
        assertContains(oak, "minecraft:oak_hanging_sign");
        assertContains(group(groups, "builtin_color_family_white"), "minecraft:white_concrete");
        assertContains(group(groups, "builtin_copper_state_oxidized"), "minecraft:oxidized_cut_copper_stairs");
        assertContains(group(groups, "builtin_stone_family_stone"), "minecraft:smooth_stone");
        assertContains(group(groups, "builtin_stone_family_granite"), "minecraft:polished_granite_stairs");
        assertContains(group(groups, "builtin_stone_family_granite"), "minecraft:granite_wall");
    }

    @Test
    void everyBuiltInMemberExistsAndColorPresetContainsColorRows() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        for (PalettePreset preset : PalettePreset.values()) {
            for (PaletteGroup group : BuiltinPalettePresets.groups(preset)) {
                assertTrue(group.members().size() >= 2, group.id());
                HashSet<String> unique = new HashSet<>();
                for (PaletteMember member : group.members()) {
                    assertTrue(unique.add(member.itemId()), group.id() + ": " + member.itemId());
                    assertTrue(BuiltInRegistries.ITEM.containsKey(Identifier.parse(member.itemId())), member.itemId());
                }
            }
        }
        assertEquals(12, BuiltinPalettePresets.groups(PalettePreset.COLOR_CATEGORIES).size());
    }

    private static PaletteGroup group(List<PaletteGroup> groups, String id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst().orElseThrow();
    }

    private static void assertContains(PaletteGroup group, String itemId) {
        assertTrue(group.members().stream().anyMatch(member -> member.itemId().equals(itemId)), itemId);
    }
}
