# Input and HUD Foundation

Stage 4 provides shared client interaction primitives. Feature-specific item selection and reach behavior remain owned by stages 5–7.

## Gesture lifecycle

`PressGestureStateMachine` uses monotonic real time and reports press, double press, long-press transition, held updates, short press, release, and cancellation. `ClientInputCoordinator` supplies the current binding token, physical key state, Feature-enabled state, focus state, and world identity on every client tick.

A held gesture is cancelled and suppressed until physical release when any of these conditions changes:

- the game window loses focus or a GUI screen opens;
- the active world changes or is unloaded;
- the key mapping changes while held;
- the owning Feature becomes disabled.

This prevents a long frame, key rebind, disconnect, or mid-gesture disable from producing a delayed click after control returns.

## Wheel ownership

`WheelInputArbiter` has one active lease. Priorities are deterministic:

1. palette wheel (`300`);
2. smart pick (`200`);
3. reach adjustment (`100`).

A higher-priority owner may revoke a lower-priority lease. Equal or lower priorities cannot steal a lease from another owner, and closing an obsolete lease cannot release the replacement. Raw scroll input cancels vanilla scrolling only when the current handler explicitly returns `true`. Focus loss, GUI opening, world transition, shutdown, or HUD disable clears ownership.

`ScrollAccumulator` combines fractional touchpad deltas into whole steps and discards a partial remainder when direction reverses.

## Radial snapshots and animation

`RadialMenuSnapshot` is immutable. Rotation returns a new snapshot, wraps in both directions with floor-mod arithmetic, and exposes a capacity-limited visible window while keeping the selected entry at the bottom of the circle. Candidate data is independent from animation state.

Animation modes are:

- `CLOCKWISE`: entries reveal sequentially clockwise from the selected entry;
- `EXPAND`: all entries move outward together;
- `OFF`: final positions are immediate.

The HUD renderer accepts text now and already supports item icons for later palette and smart-pick stages.

## Layout and development preview

Numeric text is placed below the crosshair. The radial center is placed above the hotbar. Both layouts use scaled GUI dimensions, safe margins, and an adaptive radius for small or unusual aspect ratios. Large candidate lists render only their visible window.

In a development client, `/lorian_arch_orbit preview_hud` toggles a twelve-entry, nine-visible-entry test radial and numeric HUD. While it is visible, vertical or horizontal scroll rotates the selection and consumes the vanilla hotbar scroll. Running the command again, opening a GUI, changing world, or losing focus closes the preview and restores vanilla scrolling.
