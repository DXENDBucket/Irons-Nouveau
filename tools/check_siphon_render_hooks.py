"""Check required Siphon renderer calls in native JARs, with final Forge refmap resolution.

Does not boot Minecraft or validate client graphics/Mixin transformation. It catches native
method/descriptor drift (including a target absent only on Forge) before a client launch.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
MIXIN = 'dev.ironsnouveau.mixin.NativeSiphonRenderingMixin'
HELPER = 'io.redspace.ironsspellbooks.render.SpellRenderingHelper'


def check(platform, artifact, source_override, javap):
    forge = platform == 'forge'
    source = source_override or ROOT / ('forge/src' if forge else 'src') / 'main/java/dev/ironsnouveau/mixin/NativeSiphonRenderingMixin.java'
    native = ROOT / ('forge/libs/irons-spells-n-spellbooks-forge1201.jar' if forge else 'libs/iron.jar')
    code = subprocess.run([str(javap), '-J-Xmx64m', '-classpath', str(native), '-c', '-p', HELPER],
                          capture_output=True, text=True, check=True).stdout
    match = re.search(r'public static void renderRayOfSiphoning\([^\n]+\);\s*Code:\s*(.*?)(?=\n  (?:public|private|protected)|\Z)', code, re.S)
    if not match:
        raise RuntimeError(f'{platform}: native Siphon rendering method is missing')
    calls = set()
    for owner, name, desc in re.findall(r'// (?:InterfaceMethod|Method) ([\w/$]+)\.([\w$]+):(\S+)', match.group(1)):
        calls.add(f'L{owner};{name}{desc}')
    text = source.read_text(encoding='utf-8')
    hooks = re.findall(r'target\s*=\s*"([^"]+)"', text)
    if len(hooks) != 3:
        raise RuntimeError(f'{platform}: expected all three required renderer hooks, found {len(hooks)}')
    mappings = {}
    if forge:
        if not artifact:
            raise RuntimeError('Forge requires --artifact to inspect the final embedded refmap')
        with zipfile.ZipFile(artifact) as jar:
            refmap = json.loads(jar.read('irons_nouveau.refmap.json'))
        mappings = refmap['mappings'].get(MIXIN.replace('.', '/'), refmap['mappings'].get(MIXIN, {}))
    missing = []
    for hook in hooks:
        target = mappings.get(hook, hook)
        if target not in calls:
            missing.append(f'{hook} -> {target}')
    if missing:
        raise RuntimeError(f'{platform}: required calls absent from actual native method:\n' + '\n'.join(missing))
    print(f'{platform}: all {len(hooks)} required Siphon renderer targets match the native JAR')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=['forge', 'neoforge'], required=True)
    parser.add_argument('--artifact', type=Path)
    parser.add_argument('--mixin-source', type=Path, help='Optional historical source for regression verification')
    parser.add_argument('--javap', type=Path, default=Path(os.environ.get('JAVA_HOME', '')) / 'bin/javap.exe')
    args = parser.parse_args()
    check(args.platform, args.artifact, args.mixin_source, args.javap)


if __name__ == '__main__':
    main()
