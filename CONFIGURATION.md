# Configuration Schema v1

Runtime files are stored under `<Minecraft config>/lorian_arch_orbit/`. Every document has an integer `config_version`; version `1` is current. A version `0` object is migrated before use, with the original copied to `<filename>.bak` before replacement. A newer unsupported version is never overwritten.

## `client.json`

```json
{
  "config_version": 1,
  "features": {
    "reach_extension": { "enabled": false, "distance": 8 },
    "palette_wheel": { "enabled": true, "animation": "clockwise" },
    "smart_pick": {
      "enabled": true,
      "mode": "context",
      "scan_radius": 3,
      "candidate_limit": 12,
      "hold_threshold_ms": 180,
      "history_weight": true
    },
    "wall_visual_fix": { "enabled": true },
    "invisible_blocks": {
      "enabled": true,
      "currently_visible": false,
      "show_barriers": true,
      "show_light_blocks": true
    }
  },
  "ui": { "hud_enabled": true }
}
```

Known numeric ranges are `distance` 8–128, `scan_radius` 1–3, `candidate_limit` 8–24, and `hold_threshold_ms` 50–1000. Values outside these ranges are clamped in the immutable runtime snapshot and produce a path-specific warning.

## `server.json`

```json
{
  "config_version": 1,
  "features": {
    "reach_extension": {
      "enabled": false,
      "maximum_distance": 128,
      "creative_only": true,
      "required_permission_level": 0,
      "requests_per_second": 10
    }
  }
}
```

`maximum_distance` is limited to 8–128, permission level to 0–4, and request rate to 1–40. The server remains authoritative when the reach protocol is implemented in stage 7.

## Wheel documents

`lorian_arch_orbit-wheel-primary.json` and `lorian_arch_orbit-wheel-secondary.json` currently share this extensible shell:

```json
{
  "config_version": 1,
  "groups": []
}
```

Stage 5 will define typed group/member fields without changing file ownership or persistence rules.

## Persistence and compatibility

- Writes use a temporary file in the target directory followed by atomic replacement. If the file system explicitly lacks atomic moves, the manager logs the fallback and performs a same-directory replacement.
- A parse, migration, or write error preserves the user file and the last valid in-memory snapshot. On first startup with an unreadable file, safe defaults are used in memory without overwriting it.
- Unknown fields are preserved on reload, save, and restore-default operations. Unknown-only edits do not notify a Feature.
- Client file events are debounced for 250 ms and reloaded on the client main thread. `/lorian_arch_orbit reload` remains the manual recovery path.
