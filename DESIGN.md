---
version: alpha
colors:
  primary: "#7fd4ff"
  text: "#ffffff"
  muted: "#bbbbbb"
  warning: "#ffc14d"
  panel: "#202020"
typography:
  sans:
    fontFamily: "Minecraft default font"
omitted:
  - section: rounded
    reason: "Native Minecraft widgets own their shape."
  - section: spacing
    reason: "Logical GUI geometry is owned by the Java layout classes."
  - section: components
    reason: "Minecraft Button, CycleButton and EditBox own control appearance."
---

## Overview

A building workbench for creative-mode Minecraft builders. Actual block icons and a horizontal gradient are the signature; controls remain familiar Minecraft widgets. Avoid web-style cards, decorative animation and font substitutions.

## Colors

Existing editor colors remain the runtime source of truth. The documented colors map to editor text and feedback; the new gradient screen uses the same values. Color chips show target colors alongside actual block results, never substitute for labels.

## Typography

Use the game font and translatable Components in Chinese and English. Upstream palette names are localized for known presets. User group names remain user content. Long names are truncated with full tooltips.

## Layout

Palette screens share a centered 768×408 reference canvas. PaletteViewport derives its scale from framebuffer dimensions, independently of the vanilla GUI scale, and keeps at least 1.5 pixels per logical unit when space permits. Smaller windows reduce the canvas to a minimum layout of 320×180; below that physical size the entire minimum layout scales to fit. Extra width or height remains background. AdaptivePaletteScreen owns the rendering/input boundary, including drag deltas, clipping, tooltips and IME overlays. Only the palette screens and opted-in palette HUD use this transform.

Below 600 logical units wide or 270 high, the palette editor exposes Groups, Items, Members and Preview as tabs. Every pane retains a single footer row: File, Undo, Back and Save. The layer selector is in the header; New and group actions sit above the group list. Import/share/defaults live in File, and restoring both layers requires confirmation. Resizing preserves draft values, selections and list anchors, and cancels active dragging. Palette rings keep readable icons and window the visible entries when crowded; scrolling still traverses the complete ordered list, including duplicate entries.

Editor geometry belongs to PaletteEditorLayout. Gradient geometry belongs to HueGradientLayout. Actions use 20-unit native controls and 4-unit gaps. The gradient has a 240-unit settings column and a result area; below the same compact breakpoint, Settings and Results become tabs. Settings scroll independently, including Page Up/Down and visible arrow buttons. Valid counts from 2 to 128 apply on input without a separate commit button. Invalid or incomplete text preserves the last valid sequence and blocks generation/application. Existing positional locks still require confirmation before a count change clears them. GradientNodeButton draws a translucent background, the pinned matching texture or node number, and a final unobscured color bar. Eight 26-unit cells fit each row; nodes 9-16 wrap to a second row instead of using horizontal arrows. Controls and scroll extents include the extra row height. Data status and application actions stay outside scroll regions. Hide repeats affects browsing only; refining exposes the full sequence and original sample indices.

## Elevation & Depth

Use restrained flat panels with visible hover/focus states. The home page uses compact centered translucent dark cards; no extra shadows or animated backdrops.

## Shapes

Keep square block/color cells and native action buttons inside the editors. Dropdowns use flat dark headers, thin light borders and alternating gray list rows inspired by MaLiLib, without adding a MaLiLib dependency. Home entries are rectangular translucent panels, not native textured buttons.

## Components

Overflow windows in RadialMenuWindow include predecessors and successors around the selected member. The omitted arc sits opposite the target so both adjacent visible slots match a one-step selection in the full list (when at least three slots fit). Window-local indices are retained separately from full-list selection, including repeated items. HUD palettes, editor previews and range smart-pick share this windowing rule. Rotation uses the visible slot count; retained neighbors keep their coordinates at the first animation frame and settle at the configured target.

Palette HUD rings reserve 48 logical units above and 64 below, multiplied by the larger of the native GUI-to-canvas ratio and one. The lower budget includes the 26.2 selected-item label at window height minus 59; the upper budget leaves room for a conventional multi-line top HUD without a Jade dependency. Radius and capacity shrink together when needed. PaletteTargetPosition rotates only the rendered geometry (Bottom by default, Top/Left/Right optional). PaletteArrowStyle offers Vanilla (default), Arrow, Pointer and None; both locales retain these literal labels. PaletteArrowSprite redraws Vanilla's map-marker silhouette, a triangular Arrow with a straight stem, and a pointing hand with a distinct thumb and folded fingers on a shared 11x14 logical-pixel grid. Every cell is rendered at 1x; all three visible bounds are 11x14, with one-pixel outlines and the map frame marker's black/green ramp (00BC38, 00E043, 00FF4C). Missing/invalid arrow_style falls back to vanilla; legacy shift maps to arrow without changing the schema version or overriding an existing pointer selection. None skips arrow drawing only. All visible sprites anchor their tip, including the off-center fingertip, independently of selection and rotation. The 900 ms, three-unit radial bob uses the supplied frame timestamp directly, so scrolling cannot reset its phase or direction. Opening/closing may change radial distance, and small rings suppress the arrow to protect the center label.

PaletteRotationDirection maps accumulated scroll steps to selection steps in both ClientPaletteRuntime (all three wheel layers) and the editor preview. Clockwise is the legacy default: upward scrolling selects the previous member and animates clockwise in screen coordinates; Counterclockwise reverses those steps. Downward scrolling reverses either choice. The same signed step drives selection and RadialRotationState, including overflow windows and duplicate positions. Group ordering, target position, opening/closing animation and smart-pick input remain independent. The additive rotation_direction setting round-trips through the existing config schema, falls back to clockwise when missing/invalid, participates in palette hot reload, and resets with other behavior settings.

All three styles retain the enlarged Vanilla marker's 14-unit height without per-style pixel scaling. They start drawing at radius 55, share the same cardinal transform and keep their tips anchored, preserving the inward clearance from the central name during opening and closing.

Palette items receive up to 10% emphasis using a cosine falloff over one visible slot's angular distance from the configured target. Scroll retargeting preserves visual continuity; source indices keep duplicate items independent. The palette layout reserves an 11-unit half-size for enlarged items while maintaining 21.2-unit slot spacing. HUD and wheel-editor previews share the calculation; non-palette HUDs retain their original item size and 10-unit bounds. Permanent and temporary palette HUDs share a name line and a second position/total plus scroll-hint line, including non-overflowing wheels. Temporary mode no longer embeds a prefix and count in the name.

Canonical control owners: Minecraft Button (actions), WorkbenchFlatButton (dropdown headers and rows, retaining Button input/focus/narration), EditBox (hex colors and counts). Dropdown outer bounds, including border and scrollbar, match the triggering control's width exactly; long labels retain full tooltips. Only standalone confirmation lists size to their content. Lists attach to the triggering control, opening upward when the space below is insufficient. Overflow adds a draggable scrollbar; wheel and keyboard navigation still work. Clicking outside closes the list and consumes the click. A mouse-opened list initially has no highlighted row; pointer hover and keyboard navigation own one highlight at a time. Moving the pointer returns control to hover, while a stationary pointer does not override keyboard navigation. The dark page-selector header shows the current page's registered name; its list contains only page names, without checkmarks or a duplicate home action. Footer Back returns to home; Escape exits the editing session.

EditorSession keeps a registry of page IDs, translated names, descriptions, illustrations and factories; the home cards and top-right page list use that registry. EditorHomeLayout centers two 176×163 cards at the reference canvas (half the previous width and height), shrinking them further only when required by the available space. Three or four pages use a two-column grid. There is no exit button below the cards; Escape retains the existing session-exit confirmation. EditorPageCard makes the whole panel a native focusable click target. The left card illustrates a sample wheel using game item models; the right card illustrates a gradient using block-atlas textures. These previews are independent of user drafts and downloaded color data, and obtain current resource-pack sprites when rendered. Compact cards retain their title and illustration; descriptions remain in tooltips.

One Open editor key retains the existing palette_editor ID and default P; the separate gradient key is no longer registered. Page switching keeps screen view state and the session-owned WheelEditorState / GradientWorkbench models. Escape closes a popup first; otherwise it exits directly from either main editor page, with any discard confirmation shown on that same page. Footer Back retains the visit and returns to home. Exiting disposes the visit only after unsaved wheel or unapplied gradient changes have been acknowledged. No session is written to disk.

Both editors align Back and their primary action at the lower right, using equal 64-unit buttons: Back/Save for wheels and Back/Apply for gradients. Restore items stays at the lower left of the gradient page, with the target selector before Back on the right; Generate belongs to the settings column. Targets remain temporary wheel, inventory, new group and replace group. Permanent group changes return to an undoable editor draft and require Save. Successful inventory/temporary applications mark the current gradient revision applied; failed applications do not. Source/target groups use stable layer-plus-ID references and never silently follow selection changes. Temporary palette and rollback data retain their separate memory-only lifetime.

Gradient samples carry original index, interpolated target, matched texture/item and lock state. The inspector offers the eight closest alternatives, searchable candidates, lock/unlock and an explicit Use as node action. Replacements automatically lock their position. Regeneration retains locks if they satisfy the filters; conflicts block generation and application. Structural changes clear locks only after confirmation. Matching distances are unchanged; inverse OkLAB-to-sRGB conversion is used solely to display target swatches.

HueBlockPickerScreen shares the wheel browser's 20-unit cells, translucent background and blue-gray hover feedback. It defaults to matching textures with an item-icon toggle. Counts distinguish unique item IDs from item/texture candidates. Search matches translated name, item ID or texture filename. Click/Enter selects a block in place; the right panel owns six direction controls, texture-variant cycling and placement feedback. HueFaceSelection retains the clicked texture and respects the allowed item/texture pairs; uncolored or excluded surfaces remain inspectable but cannot be applied. Only Use face updates the node or replaces/locks the inspected result. Selection survives search, scrolling, icon toggles and resize, and overrides hover preview until another block is clicked. Compact pickers retain a narrow face panel with a small texture preview; candidate-exclusion mode still uses the full compact grid. HueFaceScreen remains available from held-item and result-inspection actions. The picker uses a draggable right scrollbar, row-based wheel scrolling, Page Up/Down and cross-row keyboard focus. Resizing preserves its row anchor; query edits keep the EditBox and IME state. Candidate-edit mode toggles per-pair exclusions in place and retains keyboard focus.

Texture tiles use the matching candidate's block-atlas sprite from the active resource pack, repeated four times across the gradient. Direction is horizontal/vertical, size is 16/24/32 (default 24), and TextureTilingLayout submits only visible cells. Sprites are obtained each frame to survive resource reloads; missing sprites retain the vanilla placeholder and an explicit tooltip. This preview does not simulate biome tint, world lighting or full block models. Download states still come from HueBlocksRepository; local textures do not change the upstream color matching data.

HueBlockFaces resolves packaged blockstate predicates against actual registered default values, expanding only facing/horizontal_facing/axis properties. The extractor normalizes resource IDs, resolves model inheritance and texture aliases, reads actual element faces, and applies blockstate quarter-turn rotations. It excludes sloped elements without cardinal faces and does not infer entity-rendered models. Candidate metadata is item-specific: sharing sandstone_top does not give ordinary sandstone the side faces of smooth sandstone. Non-default lit/honey/charge states stay excluded. Resource-pack sprites can change, but geometry/UV/tint are not analyzed at runtime. The old texture_blocks mapping remains only for historical preset reproducibility.

GradientWorkbench exclusions use stable item/texture keys; invalidated locks remain explicit conflicts. Consecutive preview deduplication compares both item and texture; application still uses existing item semantics. A manually retained Comparison contains immutable samples and the original generation metadata. GradientComparisonScreen scrolls both full sequences at a shared index without mutating either result.

GradientRecipe has a separate versioned, bounded JSON format for explicit parameter or candidate-palette exports. Recipes contain node colors, lengths, pinned texture references, color space and source filters; palette-only files contain filters/exclusions. Neither stores output samples, locks, application targets or editor sessions. Decoding completes before mutation. Imports require confirmation; missing pins are reported and retain explicit colors, while missing source groups/presets reject import. GradientRecipeFiles confines reads to gradient-plans and writes unique files through a temporary file and atomic move where supported. Existing configuration/wheel schemas and dependencies are unchanged.

## Do's and Don'ts

Keep native keyboard focus and narration. Use visible previous/next controls for bounded content; scrolling is an additional shortcut. Preserve values through resize, background refresh and returning to the editor. Do not regenerate results until the user requests it. Do not overwrite custom wheel overrides when upstream data changes.
