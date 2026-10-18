# Interaction fix verification (26.3)

Code Review: PASS WITH RISKS

- Scope: editor input constants, 26.3 color categories, shared-key arbitration, temporary shortcut, Radial target configuration, translations and guides.
- No dependency, Mixin, protocol or configuration schema changes. Radial uses its existing `value` field; old icon-based targets remain readable. Custom palette overrides keep their existing precedence.
- `./gradlew.bat build --offline` with JDK 25.0.3: PASS (212 JUnit tests, zero failures).
- `./gradlew.bat -I scripts/interaction-smoke/smoke.init.gradle :fabric:runClient --offline`: INTERACTION_SMOKE PASS in a disposable world with Radial 3.2.2+26.3.
- Verified empty-hand arbitration, temporary shortcut press/release, old target fallback, icon-only edits preserving the target, component-bearing explicit targets, invalid-target rejection, group selection, member reorder and left/right/middle clicks. The fixture sends events into the real client; it is not manual physical-mouse QA.
- `git diff --check`: PASS. Release JARs checked: no smoke fixture classes.
- BLOCKER: None. MAJOR: None.
- Update notes appended in this change.

## Limits

The Architectury development transformer can retain non-daemon threads after the client stops; Fabric then reports its shutdown watchdog. Test verdicts above are explicit assertions emitted before shutdown, not the launch task exit status. Not every resolution, third-party resource pack or Radial macro composition was manually exercised.

NeoForge 26.3.0.41-beta editor fixture: INTERACTION_SMOKE PASS (selection, reorder, left/right/middle behavior). All 87 new obtainable block-state item IDs are present; existing member order and non-color presets were checked against Git HEAD.
