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

        assertEquals(68, groups.size());
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
        assertContains(group(groups, "builtin_masonry_form_walls"), "minecraft:cinnabar_brick_wall");
        assertContains(group(groups, "builtin_masonry_form_slabs"), "minecraft:sulfur_brick_slab");
        assertContains(group(groups, "builtin_copper_form_lanterns"), "minecraft:waxed_oxidized_copper_lantern");
        assertContains(group(groups, "builtin_building_ores"), "minecraft:nether_quartz_ore");
    }

    @Test
    void itemTagBOrganizesEveryBlockInTheSameWoodFamily() {
        List<PaletteGroup> groups = BuiltinPalettePresets.groups(PalettePreset.ITEM_TAG_B);
        PaletteGroup oak = group(groups, "builtin_wood_family_oak");

        assertEquals(56, groups.size());
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
        assertContains(group(groups, "builtin_stone_family_stone"), "minecraft:mossy_stone_brick_stairs");
        assertContains(group(groups, "builtin_stone_family_cinnabar"), "minecraft:chiseled_cinnabar");
        assertContains(group(groups, "builtin_stone_family_sulfur"), "minecraft:sulfur_spike");
        assertContains(group(groups, "builtin_copper_state_waxed_oxidized"),
                "minecraft:waxed_oxidized_copper_golem_statue");
    }

    @Test
    void everyBuiltInMemberExistsAndVisualColorPresetIsComplete() {
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
        List<PaletteGroup> colors = BuiltinPalettePresets.groups(PalettePreset.COLOR_CATEGORIES);
        assertEquals(8, colors.size());
        assertEquals(34, group(colors, "builtin_visual_color_red").members().size());
        assertEquals(25, group(colors, "builtin_visual_color_yellow").members().size());
        assertContains(group(colors, "builtin_visual_color_red"), "minecraft:cinnabar_bricks");
        assertContains(group(colors, "builtin_visual_color_yellow"), "minecraft:potent_sulfur");
        assertContains(group(colors, "builtin_visual_color_cyan_blue"), "minecraft:prismarine_bricks");
    }

    private static PaletteGroup group(List<PaletteGroup> groups, String id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst().orElseThrow();
    }

    private static void assertContains(PaletteGroup group, String itemId) {
        assertTrue(group.members().stream().anyMatch(member -> member.itemId().equals(itemId)), itemId);
    }
}
