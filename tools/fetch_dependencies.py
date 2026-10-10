"""Fetch pinned build dependencies without redistributing upstream mod JARs."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import urllib.request


def digest(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha512').hexdigest()


def fetch(target, entry, replace):
    if target.exists():
        if digest(target) == entry['sha512']:
            return
        if not replace:
            raise RuntimeError(f'{target.name} differs from the pinned release; use --replace to replace it')
    if not entry['url'].startswith('https://cdn.modrinth.com/'):
        raise RuntimeError('Dependency URLs must use the pinned Modrinth CDN')
    target.parent.mkdir(parents=True, exist_ok=True)
    partial = target.with_suffix('.download')
    request = urllib.request.Request(entry['url'], headers={'User-Agent': 'EndXiom/Irons-Nouveau-build'})
    with urllib.request.urlopen(request, timeout=90) as response, partial.open('wb') as output:
        shutil.copyfileobj(response, output)
    if digest(partial) != entry['sha512']:
        partial.unlink()
        raise RuntimeError(f'Checksum mismatch for {target.name}')
    partial.replace(target)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=['both', 'neoforge', 'forge'], default='both')
    parser.add_argument('--conflux', type=Path, help='Sibling Ars Conflux source checkout')
    parser.add_argument('--replace', action='store_true', help='Replace existing files with different hashes')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    core = (args.conflux or root.parent / 'Ars-Conflux').resolve()
    if not (core / 'VERSION').is_file():
        raise RuntimeError('Prepare an Ars Conflux checkout or pass --conflux')
    entries = json.loads((root / 'dependencies.lock.json').read_text(encoding='utf-8'))
    for entry in entries:
        relative = Path(entry['path'])
        platform = 'forge' if relative.parts[0] == 'forge' else 'neoforge'
        if args.platform not in ['both', platform]:
            continue
        allowed = ('forge', 'libs') if platform == 'forge' else ('libs',)
        if relative.parts[:-1] != allowed or relative.suffix != '.jar':
            raise RuntimeError('Invalid dependency destination')
        target = (root / relative).resolve()
        if not target.is_relative_to(root):
            raise RuntimeError('Dependency destination escapes the checkout')
        fetch(target, entry, args.replace)
        # Core needs only Ars and its direct API dependencies, in its own build directories.
        filename = target.name
        copies = {'ars.jar': 'ars.jar', 'curios.jar': 'curios.jar', 'geckolib.jar': 'geckolib.jar'} if platform == 'neoforge' else {
            name: name for name in ['ars-nouveau-forge1201.jar', 'curios-forge1201.jar', 'geckolib-forge1201.jar', 'patchouli-forge1201.jar']
        }
        if filename in copies:
            destination = core / ('forge/libs' if platform == 'forge' else 'libs') / copies[filename]
            fetch(destination, entry, args.replace)
        print(f'{platform}: {filename} ({entry["version"]})')


if __name__ == '__main__':
    main()
