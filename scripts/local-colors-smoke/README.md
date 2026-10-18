# Active-resource color integration smoke test

Run from the version directory with JDK 25:

```powershell
.\gradlew.bat -I scripts/local-colors-smoke/smoke.init.gradle :fabric:runClient
.\gradlew.bat -I scripts/local-colors-smoke/smoke.init.gradle :neoforge:runClient
```

This opt-in development fixture is not included in release JARs or normal builds.
It opens a real client, waits for initial resources, creates the disposable
`run/resourcepacks/lorian-local-smoke` pack, then checks texture overrides,
a new texture namespace/path, changed blockstate/model rotation, changed bytes
under the same pack name, and restoration after removing the pack. It restores
the originally selected packs and requests client shutdown. Look for
`LOCAL_COLOR_SMOKE PASS`; a launch task exit code alone is not the test verdict.
Architectury's development transformer can keep non-daemon threads alive after
Minecraft stops (Fabric may subsequently emit a shutdown-watchdog report).

The fixture uses reflection only to inspect package-private runtime data; the
production implementation does not use reflection. Snapshot/generation failure
paths, color math, animation weighting and versioned exports are covered by
`LocalHueTest` in the normal shared test suite.

The init script places `lorian-local-smoke.jar` in the selected platform's
`run/mods` directory. Remove that exact JAR after testing, otherwise the fixture
will run again on the next development client launch. The generated resource
pack is left unselected and can also be removed. Build outputs stay under
`integration/local-colors`, which is ignored by Git.
