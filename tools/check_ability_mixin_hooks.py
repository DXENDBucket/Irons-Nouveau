"""Check Boss-ability hooks against native bytecode and the final Forge refmap.

Checks selectors, descriptors and invocation counts without launching Minecraft.
This does not replace runtime transformation or client graphics verification.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MIXINS = ('AbilityDaggerMixin', 'AbilityOrbMixin', 'AbilityOrbClockMixin',
          'AbilityRiftMixin', 'AbilityWallMixin')


def inspect(javap, native, target):
    code = subprocess.run([str(javap), '-J-Xmx64m', '-classpath', str(native),
                           '-c', '-p', '-s', target], capture_output=True,
                          text=True, check=True).stdout
    methods = {}
    for match in re.finditer(r'^  [^\n;{}]+\([^\n]*\);\n'
                             r'    descriptor: (\S+)\n(.*?)(?=^  \S|^}|\Z)',
                             code, re.M | re.S):
        header = match.group(0).splitlines()[0]
        name = header.split('(')[0].split()[-1]
        methods[name + match.group(1)] = match.group(2)
    return code, methods


def check(platform, artifact, javap):
    forge = platform == 'forge'
    native = ROOT / ('forge/libs/irons-spells-n-spellbooks-forge1201.jar'
                     if forge else 'libs/iron.jar')
    with zipfile.ZipFile(artifact) as jar:
        assert jar.testzip() is None, 'Corrupt release archive'
        config = json.loads(jar.read('irons_nouveau.mixins.json'))
        registered = set(config.get('mixins', []))
        mappings = json.loads(jar.read(config['refmap']))['mappings'] if forge else {}
        for name in MIXINS:
            assert name in registered, f'{name}: absent from release Mixin config'
            assert f'dev/ironsnouveau/mixin/{name}.class' in jar.namelist()
    total = 0
    for name in MIXINS:
        relative = Path('main/java/dev/ironsnouveau/mixin') / (name + '.java')
        sources = [ROOT / 'common/src' / relative,
                   ROOT / ('forge/src' if forge else 'src') / relative]
        source = next(path for path in sources if path.is_file()).read_text(encoding='utf-8')
        simple = re.search(r'@Mixin\(value\s*=\s*(\w+)\.class', source).group(1)
        target = re.search(r'import ([\w.]+\.' + simple + r');', source).group(1)
        code, methods = inspect(javap, native, target)
        ref = mappings.get('dev/ironsnouveau/mixin/' + name, {})
        for line in source.splitlines():
            if '@WrapMethod(' not in line and '@WrapOperation(' not in line and '@Inject(' not in line:
                continue
            selector = re.search(r'method\s*=\s*"([^"]+)"', line).group(1)
            resolved = re.sub(r'^L[^;]+;', '', ref.get(selector, selector))
            bodies = [body for key, body in methods.items()
                      if key == resolved or ('(' not in resolved and key.startswith(resolved + '('))]
            assert len(bodies) == 1, f'{platform} {name}: selector {selector} -> {resolved} matched {len(bodies)} methods'
            total += 1
            hook = re.search(r'target\s*=\s*"([^"]+)"', line)
            if hook:
                original = hook.group(1)
                resolved = ref.get(original, original)
                calls = [f'L{owner};{method}{desc}' for owner, method, desc in re.findall(
                    r'// (?:InterfaceMethod|Method) ([\w/$]+)\.([\w$]+):(\S+)', bodies[0])]
                count = calls.count(resolved)
                assert count == 1, f'{platform} {name}: {original} -> {resolved}: expected 1 call, found {count}'
                total += 1
        for field in re.findall(r'@Accessor\("([^"]+)"\)', source):
            resolved = re.sub(r'^L[^;]+;', '', ref.get(field, field)).split(':')[0]
            assert re.search(r'^  [^\n]+ ' + re.escape(resolved) + r';$', code, re.M), f'{name}: missing field {resolved}'
            total += 1
    print(f'{platform}: all {total} Boss-ability selector/call/field checks passed against the native JAR')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=['forge', 'neoforge'], required=True)
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--javap', type=Path,
                        default=Path(os.environ.get('JAVA_HOME', '')) / 'bin/javap.exe')
    args = parser.parse_args()
    check(args.platform, args.artifact, args.javap)
