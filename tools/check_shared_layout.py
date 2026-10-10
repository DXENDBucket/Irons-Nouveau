"""Check shared-source ownership and generated Ars 4/5 recipe parity."""
from pathlib import Path
import argparse
import json


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generated', action='store_true', help='Also inspect both processResources outputs')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    common = root / 'common/src/main/java'
    sources = list(common.rglob('*.java'))
    for source in sources:
        relative = source.relative_to(common)
        for folder in ['src/main/java', 'forge/src/main/java']:
            if (root / folder / relative).exists():
                raise RuntimeError(f'Shared class also exists in {folder}: {relative}')
    recipes = json.loads((root / 'common/recipes/glyphs.json').read_text(encoding='utf-8'))
    for name, recipe in recipes.items():
        model = root / 'common/src/main/resources/assets/irons_nouveau/models/item' / (name + '.json')
        if not model.is_file():
            raise RuntimeError(f'Missing shared glyph model: {name}')
        if args.generated:
            neo = root / 'build/resources/main/data/irons_nouveau/recipe' / (name + '.json')
            forge = root / 'forge/build/resources/main/data/irons_nouveau/recipes' / (name + '.json')
            expected_forge = {'type': recipe['type'], 'exp': recipe['exp'], 'output': recipe['output']['id'],
                              'count': recipe['output']['count'], 'inputItems': [{'item': i} for i in recipe['inputs']]}
            if json.loads(neo.read_text(encoding='utf-8')) != recipe:
                raise RuntimeError(f'Ars 5 recipe differs from its definition: {name}')
            if json.loads(forge.read_text(encoding='utf-8')) != expected_forge:
                raise RuntimeError(f'Ars 4 recipe differs from its definition: {name}')
    print(f'{len(sources)} shared Java files; {len(recipes)} shared recipes; version {(root / "VERSION").read_text().strip()}')


if __name__ == '__main__':
    main()
