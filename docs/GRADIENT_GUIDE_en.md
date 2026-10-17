# Gradient Guide

[Back to README](../README_en.md) · [Color Wheel Guide](PALETTE_GUIDE_en.md) · [中文](GRADIENT_GUIDE.md)

Want a light-to-dark transition for a wall without searching the inventory block by block? Generate a set of blocks in the gradient editor, then fine-tune it to suit your build.

Press `P` to open the editor home and choose **Gradient Editor**. Settings appear on the left and results on the right; smaller windows use **Settings / Results** tabs.

## Generate Your First Gradient

1. Wait for the data-ready message at the top. The first use needs a download; click **Refresh data** to retry if it fails
2. Select the first color stop, use **Choose block** to pick a light block such as sandstone, and select the face you want on the right
3. Select the second stop and choose a dark block such as deepslate in the same way
4. Return to the first stop, set **Count** to `8`, and keep the default `OkLAB` setting
5. For a wall, select the **Sides** filter, then click **Generate gradient**
6. Choose **Temporary wheel** as the application target and click **Apply**. Exit the editor, quickly triple-tap `R`, and hold the third press to switch between these blocks

You can choose different blocks or enter colors directly. Opening the temporary wheel and applying results to inventory require Creative mode.

![Generated gradient](image/渐变生成示例.png)

## Set Up Color Stops

Stops determine which colors the gradient passes through. You need at least 2 and can use up to 16. Click a stop to edit it; the line at its bottom shows its color.

- **Color**: enter a six-digit hexadecimal color such as `E0C9A2`, with or without `#`
- **Use held / Choose block**: take the color from a specific block's matching texture
- **Count**: the number of positions from this stop to the next, including both endpoints. Valid values are 2–128 and take effect as you type
- The last stop has no following segment, so it does not need a count

Two stops with a count of `8` produce 8 positions. Three stops with both segment counts set to `8` produce `8 + 8 - 1 = 15` positions because the middle endpoint is shared.

This is a **position count**, not the number of distinct blocks. Similar target colors may match the same block, so repeated blocks are normal.

### Reorder Stops

- **Add stop / Remove stop**: add or remove a stop
- **Move stop earlier / later**: move the selected stop along with its outgoing segment count
- **Reverse**: reverse the entire gradient and its segment lengths

If results are locked, adding, removing, moving, or reversing stops, or changing segment counts, asks you to clear the locks first. Canceling preserves the original parameters and locks.

## Choose a Block and Face

The **Choose block** page has a scrollable grid searchable by name, registry ID, or texture filename. It shows matching textures by default; you can switch to item icons.

1. Click a block to preview it on the right
2. Select its top, bottom, east, west, south, or north face. If a direction has multiple available textures, use the arrow buttons to cycle through them
3. Check the texture, color, and placement properties, then click **Use this face**

Face selection happens directly in the right panel without opening another page. Textures without color data, or excluded by the current filters, can be previewed but cannot be used for the current match.

![Block and face selection](image/选择方块示例.png)

Selecting a face chooses the texture used for color matching. It **does not change the orientation of blocks in the world**; place them in the required orientation yourself.

## Limit the Candidates

- **Color space**: `OkLAB` matches perceptual colors, while `RGB` matches color channels. Generate with each and compare the results
- **Facing**: try sides for walls or top faces for floors, or choose a specific direction
- **Candidate source**: use all indexed blocks, a HueBlocks palette, or a wheel group you select
- **Edit candidate palette**: exclude unwanted candidates; a red line marks excluded entries. Search and batch actions are available

“Current group” remembers the layer and group you explicitly selected. Choosing another group in the wheel editor does not silently change it. If that group is deleted, select a new one.

Changing parameters keeps the old result visible but marks it as needing regeneration. Click **Generate gradient** before applying it again.

## Fine-Tune Individual Results

Click a position in the generated sequence to open its inspector, where you can see the current block, target color, and lock state.

- Choose from the 8 closest alternatives by color distance, or use **Search more**
- Replacing a result automatically locks its position. Regeneration updates unlocked positions and preserves valid locks
- Unlock a position to let it be matched again
- **Set as color stop** uses a result as a stop; **Inspect faces** lets you examine its other face textures

Locks belong to positions in the complete sequence. Two positions containing the same block can be changed independently. Editing shows all repeated positions so you can identify exactly which one you are changing.

Color and color-space changes preserve locks. If a new filter excludes a locked block, a conflict is shown; replace or unlock it before generating or applying.

## Tiling and Comparison

### Tiled Textures

Switch the results view to **Tiled textures** to see the actual textures laid out without gaps.

- Use the **View** menu to choose horizontal or vertical direction and a tile size of `16`, `24`, or `32`
- The gradient axis keeps the complete order and repeated positions; the other axis repeats four tiles. Scroll to see results beyond the visible area
- The normal block sequence can hide consecutive repeats for browsing; this does not remove generated positions

![Tiled texture preview](image/纹理平铺示例.png)

Tiling displays textures from the current resource pack, but **color matching still uses HueBlocks data**. It does not simulate scene lighting, biome tinting, or complete block models. Try the blocks in your build before settling on a result.

### Keep a Result for Comparison

1. Generate a result and choose **Keep current result for comparison** from the **View** menu
2. Change stops or filters and generate again
3. Choose **Compare results** to view both versions

The retained result does not change when you edit parameters. Keeping another result replaces the snapshot, which is not retained after exiting the editor.

## Apply Results

Choose an application target at the bottom, then click **Apply** at the bottom right.

| Target | Behavior and limits |
| --- | --- |
| Temporary wheel | Keeps the complete order and repeated positions; clears on game restart. Open it in Creative mode with a triple-tap of `R`, holding the third press |
| Inventory | Creative mode only, up to 36 positions. Fills the hotbar first, then the main inventory; results exceeding capacity are not truncated and applied |
| New permanent group | Deduplicates by item ID and creates a wheel draft. Requires at least two distinct items, followed by a save in the wheel editor |
| Replace group | Replaces the draft of an explicitly selected group. Also requires at least two distinct items and a manual save |

Permanent groups deduplicate items: different faces of the same block do not become separate members, and preview orientations are not saved as world block states.

### Restore Your Original Items

After applying to inventory or replacing held items through the temporary wheel, use **Restore items** at the bottom left.

- Only slots overwritten by these operations are restored, with original counts and components
- Repeated applications keep the backup from before the first overwrite, rather than treating the previous gradient as your original items
- Backups last only for the current player session and clear after restoration. They cannot be used after disconnecting or changing player instances

Normal primary and secondary wheel switches are not included in this backup.

## Save Parameters for Later

Under **Plans & palettes**, copy JSON, save a new file, or import from a file or the clipboard.

- **Gradient parameters**: saves stop colors, block texture references, segment counts, color space, filters, and related settings, so you can reuse a gradient setup
- **Candidate palette**: saves only filters and exclusions, for reusing a limited selection of materials
- Files live in `config/lorian_arch_orbit/gradient-plans/`

Generated results, positional locks, application targets, and comparison snapshots are not included. Importing gradient parameters asks to replace the current parameters and clear old results and locks. Importing a candidate palette keeps stops and locks but requires regeneration.

## Leaving the Editor and Understanding the Data

- The top-right menu switches to the wheel editor; **Back** at the bottom right returns to the editor home. Switching pages within a session preserves parameters
- `Esc` exits the editor directly. An open menu closes first; unapplied gradients or unsaved wheel drafts trigger a confirmation
- Applying the current gradient clears its unapplied-change warning, but a permanent wheel draft created by the application still needs **Save**
- The editor does not automatically save the whole session. Export parameters explicitly if you want to reuse them later

The candidate count is not Minecraft's total block count. Only textures with upstream color data that can be mapped to blocks in the current game participate in matching. Different faces of one block may contribute multiple candidates, and facing, palette, and custom exclusions further affect the count.

The client checks for data updates in the background at startup. A successful download is required initially; valid cached data remains available if the network later fails. See [HueBlocks Data and Preset Optimization](HUEBLOCKS.md) and [Configuration and Troubleshooting](../CONFIGURATION.md), both in Chinese, for more details.
