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

Below 600 logical units wide or 270 high, the palette editor exposes Groups, Items, Members and Preview as tabs. Preview uses a single Back to editing action so its content remains usable at minimum height. Import and share actions wrap above reserved status lines. Resizing preserves draft values, selections and list anchors, and cancels active dragging. Palette rings keep readable icons and window the visible entries when crowded; scrolling still traverses the complete ordered list, including duplicate entries.

Editor geometry belongs to PaletteEditorLayout. Gradient geometry belongs to HueGradientLayout. Actions use 20-unit native controls and 4-unit gaps. Keep status and actions outside scroll regions. The preview header contains a default-on Hide repeats toggle alongside pagination. It affects rendering and hit testing only; generated samples remain intact for application. At 320×180 logical GUI units, use a compact node editor and independently paged block preview. Wider screens show a full strip of result blocks.

## Elevation & Depth

Use restrained flat panels and native hover/focus states. No extra shadows or animated backdrops.

## Shapes

Preserve native button shapes and square block/color cells.

## Components

Palette HUD rings reserve 48 logical units above and 64 below, multiplied by the larger of the native GUI-to-canvas ratio and one. The lower budget includes the 26.2 selected-item label at window height minus 59; the upper budget leaves room for a conventional multi-line top HUD without a Jade dependency. Radius and capacity shrink together when needed. PaletteTargetPosition rotates only the rendered geometry (Bottom by default, Top/Left/Right optional). PaletteArrowStyle selects one of two shared dark-green pixel sprites: a Pointer hand or the default Shift arrow. The optional features.palette_wheel.arrow_style field falls back to shift in old or invalid configurations, without a schema-version bump. Both sprites anchor their tip (including the off-center fingertip) to the configured target direction independently of the selected slot and scroll rotation. Its own 900 ms, three-unit radial bob uses the supplied frame timestamp directly, so scrolling cannot reset its phase or turn its direction. Opening/closing may change its radial distance with the ring, and very small radii suppress it to protect the center label. Non-palette radial HUDs keep their existing geometry and visuals.

Canonical control owners: Minecraft Button (actions), CycleButton (bounded choices), EditBox (hex colors and counts). Screen owns navigation, focus and Escape. PaletteEditorScreen owns draft mutations, undo and save. The four-button footer contains Generate, target selector, Apply and Back. Targets are temporary wheel, inventory, new group and replace group. Permanent group changes return to an undoable editor draft. Inventory and temporary wheel applications stay in the workbench with feedback; Restore items and Refresh share the header status row. The standalone key entry returns directly to the game on Escape. Temporary palette and rollback data live only in memory, and never write files directly. Download states come from HueBlocksRepository and occupy a stable status line. Missing upstream data never triggers local texture color estimation.

## Do's and Don'ts

Keep native keyboard focus and narration. Use visible previous/next controls for bounded content; scrolling is an additional shortcut. Preserve values through resize, background refresh and returning to the editor. Do not regenerate results until the user requests it. Do not overwrite custom wheel overrides when upstream data changes.
