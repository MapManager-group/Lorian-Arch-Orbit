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

## Radial editor regression (2026-10-18)

- Real-client fixture now opens the mode dropdown, selects the registered pick-item mode through mouse events, opens its target picker, closes it and asserts that the populated editor returns.
- Runtime assertions: PASS with Radial 3.2.2+26.3. This includes the existing independent-target and item-component checks.
- `./gradlew.bat build --offline`: PASS with JDK 25.0.3. `git diff --check`: PASS.
- These are automated client event checks, not manual mouse QA. The development shutdown-watchdog limitation above still applies; launch task exit status is not reported as success.
- The existing 26.3 implementation already uses the 3.2 editor API; no production-code fix was needed. Only the regression fixture and this report changed. Release update notes are not applicable to this test-only change.
- Code Review: PASS WITH RISKS (development shutdown limitation only); no BLOCKER/MAJOR findings.
