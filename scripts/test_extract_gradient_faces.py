import io
import json
import unittest
import zipfile
from extract_gradient_faces import extract, rotated


class ExtractFacesTest(unittest.TestCase):
    def test_rotation_matches_blockstate_quarter_turns(self):
        self.assertEqual('east', rotated('north', 0, 90))
        self.assertEqual('top', rotated('north', 270, 0))
        self.assertEqual('bottom', rotated('north', 90, 0))
        for face in ['north', 'south', 'east', 'west']:
            self.assertEqual(face, rotated(face, 360, 360))

    def test_indirect_model_inherits_faces_and_resolves_unqualified_references(self):
        files = {
            'assets/minecraft/items/test.json': {},
            'assets/minecraft/blockstates/test.json': {'variants': {'facing=east,powered=false': {'model': 'block/custom', 'y': 90}}},
            'assets/minecraft/models/block/custom.json': {'parent': 'block/base', 'textures': {'paint': 'block/test'}},
            'assets/minecraft/models/block/base.json': {'elements': [{'faces': {'north': {'texture': '#paint'}}}]},
        }
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, 'w') as output:
            for path, data in files.items():
                output.writestr(path, json.dumps(data))
        with zipfile.ZipFile(stream) as archive:
            result = extract(archive)['minecraft:test']
        self.assertEqual([{'when': {'facing': 'east', 'powered': 'false'}, 'textures': {'test.png': ['east']}}], result)

    def test_multipart_predicates_survive_extraction(self):
        stream = io.BytesIO()
        predicate = {'OR': [{'north': 'true'}, {'east': 'true'}]}
        with zipfile.ZipFile(stream, 'w') as output:
            output.writestr('assets/minecraft/items/test.json', '{}')
            output.writestr('assets/minecraft/blockstates/test.json', json.dumps({'multipart': [{'when': predicate, 'apply': {'model': 'block/test'}}]}))
            output.writestr('assets/minecraft/models/block/test.json', json.dumps({'textures': {'side': 'block/test'}, 'elements': [{'faces': {'west': {'texture': '#side'}}}]}))
        with zipfile.ZipFile(stream) as archive:
            self.assertEqual(predicate, extract(archive)['minecraft:test'][0]['when'])


if __name__ == '__main__':
    unittest.main()
