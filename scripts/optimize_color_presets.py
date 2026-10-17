#!/usr/bin/env python3
"""Reproduce texture mapping and color-only preset ordering from exact vanilla models + HueBlocks.
Usage: python3 scripts/optimize_color_presets.py --jar <26.2 client/merged.jar> --blockdata <_blockdata.json>
Only known-color members move; absent colors stay in their original slots. No texture colors are estimated.
"""
import argparse
import json
import math
import re
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'common/src/main/resources/assets/lorian_arch_orbit'


def distance(a, b):
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def cost(order, colors):
    return sum(distance(colors[a], colors[b]) for a, b in zip(order, order[1:]))


def order_members(members, colors):
    known = [m for m in members if m in colors]
    if len(known) < 3:
        return members
    best = known[:]
    best_cost = cost(best, colors)
    # ponytail: small built-in groups only; nearest-neighbor + 2-opt avoids a solver dependency.
    for start in known:
        remaining = [m for m in known if m != start]
        order = [start]
        while remaining:
            next_member = min(remaining, key=lambda m: distance(colors[order[-1]], colors[m]))
            order.append(next_member)
            remaining.remove(next_member)
        improved = True
        while improved:
            improved = False
            for i in range(1, len(order) - 1):
                for j in range(i + 1, len(order)):
                    before = distance(colors[order[i - 1]], colors[order[i]])
                    after = distance(colors[order[i - 1]], colors[order[j]])
                    if j + 1 < len(order):
                        before += distance(colors[order[j]], colors[order[j + 1]])
                        after += distance(colors[order[i]], colors[order[j + 1]])
                    if after + 1e-10 < before:
                        order[i:j + 1] = reversed(order[i:j + 1])
                        improved = True
        current_cost = cost(order, colors)
        if current_cost + 1e-10 < best_cost:
            best, best_cost = order, current_cost
    # Preserve the original group's direction where its endpoint colors make that meaningful.
    if (distance(colors[best[-1]], colors[known[0]]) + distance(colors[best[0]], colors[known[-1]])
            < distance(colors[best[0]], colors[known[0]]) + distance(colors[best[-1]], colors[known[-1]])):
        best.reverse()
    iterator = iter(best)
    return [next(iterator) if m in colors else m for m in members]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', required=True, type=Path)
    parser.add_argument('--blockdata', required=True, type=Path)
    args = parser.parse_args()
    raw = json.loads(args.blockdata.read_text())
    blocks = raw[1:]
    upstream = {b['texture'] for b in blocks}
    mapping = {texture: [] for texture in sorted(upstream)}
    with zipfile.ZipFile(args.jar) as archive:
        names = set(archive.namelist())
        block_ids = sorted(n.removeprefix('assets/minecraft/blockstates/').removesuffix('.json')
                           for n in names if n.startswith('assets/minecraft/blockstates/') and n.endswith('.json'))
        models = {}

        def model_textures(model, ancestors=()):
            if model in ancestors:
                raise ValueError('Cyclic model inheritance')
            if model in models:
                return models[model]
            path = 'assets/' + model.replace(':', '/models/', 1) + '.json'
            if path not in names:
                return {}
            node = json.loads(archive.read(path))
            textures = dict(model_textures(node['parent'], ancestors + (model,))) if 'parent' in node else {}
            textures.update({k: v.get('sprite', '') if isinstance(v, dict) else v
                             for k, v in node.get('textures', {}).items()})
            models[model] = textures
            return textures

        for block_id in block_ids:
            if 'assets/minecraft/items/' + block_id + '.json' not in names:
                continue
            # Only same-named base models: excludes lit/powered/transformed textures that
            # an ordinary inventory item cannot represent. No suffix guessing at runtime.
            textures = model_textures('minecraft:block/' + block_id)
            for key, value in textures.items():
                if key == 'particle':
                    continue
                seen = set()
                while value.startswith('#') and value[1:] in textures and value not in seen:
                    seen.add(value)
                    value = textures[value[1:]]
                if value.startswith('minecraft:block/'):
                    texture = value.removeprefix('minecraft:block/') + '.png'
                    if texture in mapping and 'minecraft:' + block_id not in mapping[texture]:
                        mapping[texture].append('minecraft:' + block_id)
    mapping = {k: sorted(v, key=lambda item: (item != 'minecraft:' + k[:-4], len(item), item))
               for k, v in mapping.items() if v}
    out = ASSETS / 'hueblocks/texture_blocks.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(mapping, indent=2) + '\n')
    per_item = {}
    for block in blocks:
        for item in mapping.get(block['texture'], []):
            per_item.setdefault(item, []).append(block)
    colors = {}
    for item, candidates in per_item.items():
        sides = [b for b in candidates if any(s not in ('top', 'bottom') for s in b['sides'])]
        chosen = sides or candidates
        colors[item] = tuple(sum(float(b['lab'][i]) for b in chosen) / len(chosen) for i in range(3))
    preset_path = ASSETS / 'palette_presets/default.json'
    preset_text = preset_path.read_text()
    presets = json.loads(preset_text)
    report_path = ROOT / 'docs/HUEBLOCKS_PRESET_REPORT.json'
    previous = json.loads(report_path.read_text()) if report_path.exists() else {'groups': []}
    baselines = {g['id']: g['original_members'] for g in previous['groups']}
    report = {'source': 'https://1280px.github.io/hueblocks/blocksets/Minecraft%2026.2/_blockdata.json',
              'generated_at': raw[0], 'metric': 'Sum of consecutive known-color OkLAB Euclidean distances',
              'mapped_textures': len(mapping), 'groups': []}
    for group in presets['color_explicit']:
        original = baselines.get(group['id'], group['members'])[:]
        assert sorted(original) == sorted(group['members'])
        replacement = order_members(original, colors)
        assert sorted(original) == sorted(replacement)
        assert all(replacement[i] == m for i, m in enumerate(original) if m not in colors)
        known_before = [m for m in original if m in colors]
        known_after = [m for m in replacement if m in colors]
        before, after = cost(known_before, colors), cost(known_after, colors)
        assert after <= before + 1e-10
        report['groups'].append({'id': group['id'], 'before': round(before, 6), 'after': round(after, 6),
                                 'missing': [m for m in original if m not in colors],
                                 'original_members': original, 'optimized_members': replacement})
        group['members'] = replacement
    for group in presets['color_explicit']:
        pattern = r'("id"\s*:\s*"' + re.escape(group['id']) + r'"[^\n]*?"members"\s*:\s*)\[[^\]]*\]'
        preset_text, count = re.subn(pattern, lambda m: m[1] + json.dumps(group['members']), preset_text)
        if count != 1:
            raise ValueError('Expected one inline member array for ' + group['id'])
    preset_path.write_text(preset_text)
    report_path.write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps({g['id']: {'before': g['before'], 'after': g['after'], 'missing': len(g['missing'])}
                      for g in report['groups']}, indent=2))


if __name__ == '__main__':
    main()
