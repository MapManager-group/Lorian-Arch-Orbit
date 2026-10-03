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

Editor geometry belongs to PaletteEditorLayout. Gradient geometry belongs to HueGradientLayout. Actions use 20-unit native controls and 4-unit gaps. The gradient has a 224-unit settings column and a result area; below the same compact breakpoint, Settings and Results become tabs. Settings scroll independently, including Page Up/Down and visible arrow buttons. Valid counts from 2 to 128 apply on input without a separate commit button. Invalid or incomplete text preserves the last valid sequence and blocks generation/application. Existing positional locks still require confirmation before a count change clears them. Node-strip arrows appear only when nodes overflow the available row; unavailable directions are disabled. Data status and application actions stay outside scroll regions. Hide repeats affects browsing only; refining exposes the full sequence and original sample indices.

## Elevation & Depth

Use restrained flat panels with visible hover/focus states. The home page uses compact centered translucent dark cards; no extra shadows or animated backdrops.

## Shapes

Keep square block/color cells and native action buttons inside the editors. Dropdowns use flat dark headers, thin light borders and alternating gray list rows inspired by MaLiLib, without adding a MaLiLib dependency. Home entries are rectangular translucent panels, not native textured buttons.

## Components

Palette HUD rings reserve 48 logical units above and 64 below, multiplied by the larger of the native GUI-to-canvas ratio and one. The lower budget includes the 26.2 selected-item label at window height minus 59; the upper budget leaves room for a conventional multi-line top HUD without a Jade dependency. Radius and capacity shrink together when needed. PaletteTargetPosition rotates only the rendered geometry (Bottom by default, Top/Left/Right optional). PaletteArrowStyle selects one of two shared dark-green pixel sprites: a Pointer hand or the default Shift arrow. The optional features.palette_wheel.arrow_style field falls back to shift in old or invalid configurations, without a schema-version bump. Both sprites anchor their tip (including the off-center fingertip) to the configured target direction independently of the selected slot and scroll rotation. Its own 900 ms, three-unit radial bob uses the supplied frame timestamp directly, so scrolling cannot reset its phase or turn its direction. Opening/closing may change its radial distance with the ring, and very small radii suppress it to protect the center label. Non-palette radial HUDs keep their existing geometry and visuals.

Canonical control owners: Minecraft Button (actions), WorkbenchFlatButton (dropdown headers and rows, retaining Button input/focus/narration), EditBox (hex colors and counts). Dropdown outer bounds, including border and scrollbar, match the triggering control's width exactly; long labels retain full tooltips. Only standalone confirmation lists size to their content. Lists attach to the triggering control, opening upward when the space below is insufficient. Overflow adds a draggable scrollbar; wheel and keyboard navigation still work. Clicking outside closes the list and consumes the click. A mouse-opened list initially has no highlighted row; pointer hover and keyboard navigation own one highlight at a time. Moving the pointer returns control to hover, while a stationary pointer does not override keyboard navigation. The dark page-selector header shows the current page's registered name; its list contains only page names, without checkmarks or a duplicate home action. Footer Back returns to home; Escape exits the editing session.

EditorSession keeps a registry of page IDs, translated names, descriptions, illustrations and factories; the home cards and top-right page list use that registry. EditorHomeLayout centers two 176×163 cards at the reference canvas (half the previous width and height), shrinking them further only when required by the available space. Three or four pages use a two-column grid. There is no exit button below the cards; Escape retains the existing session-exit confirmation. EditorPageCard makes the whole panel a native focusable click target. The left card illustrates a sample wheel using game item models; the right card illustrates a gradient using block-atlas textures. These previews are independent of user drafts and downloaded color data, and obtain current resource-pack sprites when rendered. Compact cards retain their title and illustration; descriptions remain in tooltips.

One Open editor key retains the existing palette_editor ID and default P; the separate gradient key is no longer registered. Page switching keeps screen view state and the session-owned WheelEditorState / GradientWorkbench models. Escape closes a popup first; otherwise it exits directly from either main editor page, with any discard confirmation shown on that same page. Footer Back retains the visit and returns to home. Exiting disposes the visit only after unsaved wheel or unapplied gradient changes have been acknowledged. No session is written to disk.

Both editors align Back and their primary action at the lower right, using equal 64-unit buttons: Back/Save for wheels and Back/Apply for gradients. Restore items stays at the lower left of the gradient page, with the target selector before Back on the right; Generate belongs to the settings column. Targets remain temporary wheel, inventory, new group and replace group. Permanent group changes return to an undoable editor draft and require Save. Successful inventory/temporary applications mark the current gradient revision applied; failed applications do not. Source/target groups use stable layer-plus-ID references and never silently follow selection changes. Temporary palette and rollback data retain their separate memory-only lifetime.

Gradient samples carry original index, interpolated target, matched texture/item and lock state. The inspector offers the eight closest alternatives, searchable candidates, lock/unlock and an explicit Use as node action. Replacements automatically lock their position. Regeneration retains locks if they satisfy the filters; conflicts block generation and application. Structural changes clear locks only after confirmation. Matching distances are unchanged; inverse OkLAB-to-sRGB conversion is used solely to display target swatches.

HueBlockPickerScreen shares the wheel browser's 20-unit item cells, translucent grid background and blue-gray hover feedback. HueBlockPickerLayout places search and candidate count above the grid, with a block/model/texture preview to its right. Below 600 wide or 270 high, the grid uses the available width and tooltips provide texture details. Candidates retain their source order and distinct texture variants; no item-ID deduplication changes endpoint or replacement semantics. Search matches translated name, item ID or texture filename. Click/Enter chooses immediately and returns to the caller. The picker has a right-hand draggable scrollbar and row-based wheel scrolling, without page buttons or page numbers. Page Up/Down scrolls by the visible row count; arrow navigation scrolls when focus reaches a grid edge. Resize retains the first visible candidate as a row anchor and cancels dragging. Keyboard focus controls the preview until the pointer moves. Search edits replace only grid controls, preserving the text cursor and IME state.

Texture tiles use the matching candidate's block-atlas sprite from the active resource pack, repeated four times across the gradient. Direction is horizontal/vertical, size is 16/24/32 (default 24), and TextureTilingLayout submits only visible cells. Sprites are obtained each frame to survive resource reloads; missing sprites retain the vanilla placeholder and an explicit tooltip. This preview does not simulate biome tint, world lighting or full block models. Download states still come from HueBlocksRepository; local textures do not change the upstream color matching data.

## Do's and Don'ts

Keep native keyboard focus and narration. Use visible previous/next controls for bounded content; scrolling is an additional shortcut. Preserve values through resize, background refresh and returning to the editor. Do not regenerate results until the user requests it. Do not overwrite custom wheel overrides when upstream data changes.
