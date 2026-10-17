"""Extract face references from the exact target jar, without guessing model names.

No colors are computed. State predicates are preserved for runtime evaluation against
the registered default block state. Rotations use Minecraft blockstate x/y quarters.
"""
import argparse
import json
from pathlib import Path
import zipfile

VECTORS = {'down': (0, -1, 0), 'up': (0, 1, 0), 'north': (0, 0, -1),
           'south': (0, 0, 1), 'west': (-1, 0, 0), 'east': (1, 0, 0)}
FACE_NAMES = {'down': 'bottom', 'up': 'top'}


def resource(value):
    return value if ':' in value else 'minecraft:' + value


def rotated(face, x, y):
    a, b, c = VECTORS[face]
    for _ in range((x % 360) // 90):
        b, c = c, -b
    for _ in range((y % 360) // 90):
        a, c = -c, a
    name = next(k for k, v in VECTORS.items() if v == (a, b, c))
    return FACE_NAMES.get(name, name)


def extract(archive):
    names = set(archive.namelist())
    cache = {}

    def model(name, ancestors=()):
        name = resource(name)
        if name in ancestors:
            raise ValueError('Cyclic model: ' + name)
        if name in cache:
            return cache[name]
        path = 'assets/' + name.replace(':', '/models/', 1) + '.json'
        if path not in names:
            return {}, []  # Entity/special renderers have no ordinary model faces.
        data = json.loads(archive.read(path))
        inherited, elements = model(data['parent'], ancestors + (name,)) if 'parent' in data else ({}, [])
        textures = dict(inherited)
        textures.update({k: v.get('sprite', '') if isinstance(v, dict) else v
                         for k, v in data.get('textures', {}).items()})
        result = textures, data.get('elements', elements)
        cache[name] = result
        return result

    result = {}
    for path in sorted(names):
        if not path.startswith('assets/minecraft/blockstates/') or not path.endswith('.json'):
            continue
        item = Path(path).stem
        if 'assets/minecraft/items/' + item + '.json' not in names:
            continue
        data = json.loads(archive.read(path))
        entries = []
        for selector, apply in data.get('variants', {}).items():
            when = dict(pair.split('=', 1) for pair in selector.split(',') if pair)
            entries.append((when, apply))
        entries += [(part.get('when', {}), part['apply']) for part in data.get('multipart', [])]
        records = []
        for when, apply in entries:
            for variant in apply if isinstance(apply, list) else [apply]:
                textures, elements = model(variant['model'])
                faces = {}
                for element in elements:
                    # Sloped elements have no exact cardinal viewing face.
                    if element.get('rotation', {}).get('angle', 0) != 0:
                        continue
                    for face, info in element.get('faces', {}).items():
                        value, visited = info['texture'], set()
                        while value.startswith('#') and value not in visited:
                            visited.add(value)
                            value = textures.get(value[1:], '')
                        value = resource(value)
                        if not value.startswith('minecraft:block/'):
                            continue
                        texture = value.removeprefix('minecraft:block/')
                        if '/' in texture or not texture:
                            continue
                        direction = rotated(face, variant.get('x', 0), variant.get('y', 0))
                        faces.setdefault(texture + '.png', set()).add(direction)
                if faces:
                    record = {'when': when, 'textures': {k: sorted(v) for k, v in sorted(faces.items())}}
                    if record not in records:
                        records.append(record)
        if records:
            result['minecraft:' + item] = records
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jar', type=Path, required=True)
    parser.add_argument('--output', type=Path, default=Path(__file__).resolve().parents[1] /
                        'common/src/main/resources/assets/lorian_arch_orbit/hueblocks/block_faces.json')
    args = parser.parse_args()
    with zipfile.ZipFile(args.jar) as archive:
        result = extract(archive)
    args.output.write_text(json.dumps(result, separators=(',', ':')) + '\n', encoding='utf-8')
    print(f'Extracted {len(result)} block models, {sum(map(len, result.values()))} state/face records')


if __name__ == '__main__':
    main()
