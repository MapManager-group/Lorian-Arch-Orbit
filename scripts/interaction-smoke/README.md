# Interaction regression fixture

Opt-in Fabric client test with the version-matched Radial JAR in `fabric/run/mods` and a disposable same-version world at `fabric/run/saves/lorian-interaction-smoke`. Do not point this fixture at a user world. Run with JDK 25:

```powershell
.\gradlew.bat -I scripts/interaction-smoke/smoke.init.gradle :fabric:runClient
```

Checks empty-hand shared-key arbitration, legacy and component-bearing explicit item targets, invalid-target rejection, group selection, member drag ordering, left/right/middle buttons. Editor changes remain in memory and are discarded. The fixture shuts down the game. Look for `INTERACTION_SMOKE PASS`; the development transformer may keep a background thread alive after shutdown. Remove the generated `fabric/run/mods/lorian-interaction-smoke.jar` after testing. The fixture is never packaged into release JARs.
