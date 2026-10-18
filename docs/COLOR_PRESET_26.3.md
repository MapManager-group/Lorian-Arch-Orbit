# 26.3 color-category additions

The 87 new obtainable block items were checked against the 26.3 and 26.2 vanilla JAR assets. Wall signs and the potted sapling have no separate item and are excluded. Cushions are not block-state entries. Existing members retain their relative order.

Wool and concrete stairs/slabs follow their full block. Other additions are inserted next to a reviewed nearest existing representative in OkLab using alpha-weighted linear-sRGB vanilla sprite means; named leaves/shrubs remain in their named color family. Model side/all/particle textures provide representatives (wood planks for signs; particle sprites for special models). This is a stable vanilla default, not a resource-pack-dependent automatic rewrite of user groups.

| Item | Color group | Placement |
|---|---|---|
| black_concrete_slab | black_white | adjacent to minecraft:black_concrete |
| black_concrete_stairs | black_white | adjacent to minecraft:black_concrete |
| black_wool_slab | black_white | adjacent to minecraft:black_wool |
| black_wool_stairs | black_white | adjacent to minecraft:black_wool |
| blue_concrete_slab | cyan_blue | adjacent to minecraft:blue_concrete |
| blue_concrete_stairs | cyan_blue | adjacent to minecraft:blue_concrete |
| blue_wool_slab | cyan_blue | adjacent to minecraft:blue_wool |
| blue_wool_stairs | cyan_blue | adjacent to minecraft:blue_wool |
| brown_concrete_slab | brown | adjacent to minecraft:brown_concrete |
| brown_concrete_stairs | brown | adjacent to minecraft:brown_concrete |
| brown_wool_slab | brown | adjacent to minecraft:brown_wool |
| brown_wool_stairs | brown | adjacent to minecraft:brown_wool |
| cyan_concrete_slab | cyan_blue | adjacent to minecraft:cyan_concrete |
| cyan_concrete_stairs | cyan_blue | adjacent to minecraft:cyan_concrete |
| cyan_wool_slab | cyan_blue | adjacent to minecraft:cyan_wool |
| cyan_wool_stairs | cyan_blue | adjacent to minecraft:cyan_wool |
| gray_concrete_slab | black_white | adjacent to minecraft:gray_concrete |
| gray_concrete_stairs | black_white | adjacent to minecraft:gray_concrete |
| gray_wool_slab | black_white | adjacent to minecraft:gray_wool |
| gray_wool_stairs | black_white | adjacent to minecraft:gray_wool |
| green_concrete_slab | green | adjacent to minecraft:green_concrete |
| green_concrete_stairs | green | adjacent to minecraft:green_concrete |
| green_wool_slab | green | adjacent to minecraft:green_wool |
| green_wool_stairs | green | adjacent to minecraft:green_wool |
| light_blue_concrete_slab | cyan_blue | adjacent to minecraft:light_blue_concrete |
| light_blue_concrete_stairs | cyan_blue | adjacent to minecraft:light_blue_concrete |
| light_blue_wool_slab | cyan_blue | adjacent to minecraft:light_blue_wool |
| light_blue_wool_stairs | cyan_blue | adjacent to minecraft:light_blue_wool |
| light_gray_concrete_slab | black_white | adjacent to minecraft:light_gray_concrete |
| light_gray_concrete_stairs | black_white | adjacent to minecraft:light_gray_concrete |
| light_gray_wool_slab | black_white | adjacent to minecraft:light_gray_wool |
| light_gray_wool_stairs | black_white | adjacent to minecraft:light_gray_wool |
| lime_concrete_slab | green | adjacent to minecraft:lime_concrete |
| lime_concrete_stairs | green | adjacent to minecraft:lime_concrete |
| lime_wool_slab | green | adjacent to minecraft:lime_wool |
| lime_wool_stairs | green | adjacent to minecraft:lime_wool |
| magenta_concrete_slab | purple_magenta_pink | adjacent to minecraft:magenta_concrete |
| magenta_concrete_stairs | purple_magenta_pink | adjacent to minecraft:magenta_concrete |
| magenta_wool_slab | purple_magenta_pink | adjacent to minecraft:magenta_wool |
| magenta_wool_stairs | purple_magenta_pink | adjacent to minecraft:magenta_wool |
| orange_concrete_slab | orange | adjacent to minecraft:orange_concrete |
| orange_concrete_stairs | orange | adjacent to minecraft:orange_concrete |
| orange_poplar_leaves | orange | near minecraft:red_sandstone |
| orange_wool_slab | orange | adjacent to minecraft:orange_wool |
| orange_wool_stairs | orange | adjacent to minecraft:orange_wool |
| pink_concrete_slab | purple_magenta_pink | adjacent to minecraft:pink_concrete |
| pink_concrete_stairs | purple_magenta_pink | adjacent to minecraft:pink_concrete |
| pink_wool_slab | purple_magenta_pink | adjacent to minecraft:pink_wool |
| pink_wool_stairs | purple_magenta_pink | adjacent to minecraft:pink_wool |
| poplar_button | black_white | near minecraft:light_gray_wool |
| poplar_door | black_white | near minecraft:light_gray_wool |
| poplar_fence | black_white | near minecraft:light_gray_wool |
| poplar_fence_gate | black_white | near minecraft:light_gray_wool |
| poplar_hanging_sign | black_white | near minecraft:light_gray_wool |
| poplar_log | brown | near minecraft:mangrove_roots |
| poplar_planks | black_white | near minecraft:light_gray_wool |
| poplar_pressure_plate | black_white | near minecraft:light_gray_wool |
| poplar_sapling | orange | near minecraft:orange_terracotta |
| poplar_shelf | black_white | near minecraft:light_gray_wool |
| poplar_sign | black_white | near minecraft:dead_horn_coral_block |
| poplar_slab | black_white | near minecraft:light_gray_wool |
| poplar_stairs | black_white | near minecraft:light_gray_wool |
| poplar_trapdoor | black_white | near minecraft:light_gray_wool |
| poplar_wood | brown | near minecraft:mangrove_roots |
| purple_concrete_slab | purple_magenta_pink | adjacent to minecraft:purple_concrete |
| purple_concrete_stairs | purple_magenta_pink | adjacent to minecraft:purple_concrete |
| purple_wool_slab | purple_magenta_pink | adjacent to minecraft:purple_wool |
| purple_wool_stairs | purple_magenta_pink | adjacent to minecraft:purple_wool |
| red_concrete_slab | red | adjacent to minecraft:red_concrete |
| red_concrete_stairs | red | adjacent to minecraft:red_concrete |
| red_poplar_leaves | red | near minecraft:red_wool |
| red_shrub | red | near minecraft:red_concrete |
| red_wool_slab | red | adjacent to minecraft:red_wool |
| red_wool_stairs | red | adjacent to minecraft:red_wool |
| shelf_mushroom | orange | near minecraft:honeycomb_block |
| straw_bed | yellow | near minecraft:hay_block |
| stripped_poplar_log | black_white | near minecraft:light_gray_wool (exclude biome-tinted grayscale leaves as anchors) |
| stripped_poplar_wood | black_white | near minecraft:light_gray_wool (exclude biome-tinted grayscale leaves as anchors) |
| white_concrete_slab | black_white | adjacent to minecraft:white_concrete |
| white_concrete_stairs | black_white | adjacent to minecraft:white_concrete |
| white_wool_slab | black_white | adjacent to minecraft:white_wool |
| white_wool_stairs | black_white | adjacent to minecraft:white_wool |
| yellow_concrete_slab | yellow | adjacent to minecraft:yellow_concrete |
| yellow_concrete_stairs | yellow | adjacent to minecraft:yellow_concrete |
| yellow_poplar_leaves | yellow | near minecraft:yellow_terracotta |
| yellow_wool_slab | yellow | adjacent to minecraft:yellow_wool |
| yellow_wool_stairs | yellow | adjacent to minecraft:yellow_wool |

Biome-tinted leaf textures are grayscale before rendering; they are not valid color anchors for un-tinted wood. Stripped poplar uses the neutral group after review.
