# Source Boundaries

The project keeps loader-specific code at the edges and all reusable logic in `common`.

## Runtime boundaries

- `common/src/main`: loader-neutral state, feature logic, protocols, and data models. Client-only shared code is isolated under `com.davidblackcn.lorianarchorbit.client`; only loader client entry points may reference that package.
- `fabric/src/main`: Fabric common and dedicated-server entry points, registrations, and adapters.
- `fabric/src/main/.../client`: Fabric client entry points and client-only adapters.
- `neoforge/src/main`: NeoForge common and dedicated-server entry points, registrations, and adapters.
- NeoForge client-only adapters live under `com.davidblackcn.lorianarchorbit.neoforge.client` and are reached only from the client-only Mod entry point.

## Planned narrow platform bridges

Platform interfaces will be introduced only when their first consumer is implemented. Their scope is limited to:

- resolving the platform configuration directory;
- opening the loader-specific configuration page;
- wrapping or replacing baked client models;
- requesting rebuilds for already-visible chunks;
- forwarding the minimal loader lifecycle events required by a feature.

YACL remains compile-only for production artifacts and is supplied by the client installation. The shared YACL page lives in the isolated common client package, while Config Manager and common initialization do not reference it. Mod Menu is referenced only by the Fabric client adapter. The development `runClient` tasks receive both libraries through a client-only classpath so `runServer` does not discover them.

## Feature framework

- `FeatureRegistry` owns immutable definitions and validates required dependencies before startup.
- `FeatureManager` owns mutable runtime state. It enables dependencies in topological order and releases dependents in reverse order.
- User preferences and current availability are independent; a temporary missing server capability never overwrites the saved preference.
- `FeatureContext` exposes typed service boundaries and a state gate. One-time loader listeners must check this gate before doing work.
- Disabled features keep only their validated configuration snapshot. They do not enter enable or world lifecycle callbacks.
- Common initialization loads only `server.json` and the server-side Feature manager. Fabric and NeoForge client entry points separately create the client configuration/Feature runtime.

## Configuration runtime

- `ConfigFile` owns one immutable snapshot and only replaces it after a complete parse, migration, validation, and successful save.
- `ClientConfigManager` owns `client.json` and both wheel documents; `ServerConfigManager` owns `server.json`.
- Unknown JSON fields are retained across normal saves and default restoration. Only known typed values participate in Feature change notifications.
- The watcher observes only the three client files, merges rapid events for 250 ms, and submits reload work through the Minecraft client executor.
- YACL edits a detached `ClientConfigDraft`. Its binding setters enter the draft when YACL applies options; only the Save callback invokes Config Manager.

## Input and HUD runtime

- Pure interaction logic lives under `com.davidblackcn.lorianarchorbit.interaction`; it has no loader-specific imports and is covered by unit tests.
- `ClientInteractionRuntime` is initialized only by the shared client configuration runtime and registers Architectury client tick, raw scroll, and HUD callbacks once.
- Gesture tracking uses real time and receives binding, focus, world, and Feature-enabled state on every tick. Invalidating any of them cancels the gesture and suppresses it until release.
- `WheelInputArbiter` owns the sole scroll lease. Vanilla scrolling is cancelled only when the active owner handles the event; focus, screens, world transitions, and shutdown release all transient ownership.
- `ClientHudOverlayManager` is the Minecraft 26.2 `GuiGraphicsExtractor` adapter. Layout, circular selection, visible-window, and animation calculations remain immutable pure logic outside the renderer.

## Generated and fixture data

- Generated resources are owned by `common/src/generated/resources` and are produced once by Fabric's data-generation API for both loader artifacts.
- Hand-authored regression fixtures belong in `test-data`; they are test inputs and must not be packaged as runtime resources.
