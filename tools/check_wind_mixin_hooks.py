"""Check optional Wind hooks against actual release JARs; no upstream content is copied."""
import argparse
import json
import re
import subprocess
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=['forge', 'neoforge'], required=True)
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--sources', type=Path)
    parser.add_argument('--javap', default='javap')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    native = root / ('forge/libs/wind-spellbooks-forge1201.jar' if args.platform == 'forge' else 'libs/wind-spellbooks.jar')
    base = 'net.raptorzizi.wind_spellbooks.'
    names = {
        'entity.spells.wind_blade.WindBladeProjectile': ['damageEntity'],
        'entity.spells.tornado.TornadoEntity': [],
        'entity.spells.almighty_push.AlmightyPushEntity': [],
        'entity.spells.iron_slash.IronSlashEntity': ['applyDamageToAll'],
        'spells.wind.TornadoSpell': ['getDamage'],
        'effect.AeropicEffect': [],
    }
    tick = 'm_8119_' if args.platform == 'forge' else 'tick'
    define = 'm_8097_' if args.platform == 'forge' else 'defineSynchedData'
    block = 'm_8060_' if args.platform == 'forge' else 'onHitBlock'
    effect = 'm_6742_' if args.platform == 'forge' else 'applyEffectTick'
    for cls in names:
        if cls.startswith('entity.'):
            names[cls].append(tick)
    names['entity.spells.wind_blade.WindBladeProjectile'] += [define, block]
    names['effect.AeropicEffect'].append(effect)
    for cls, methods in names.items():
        output = subprocess.run([args.javap, '-p', '-s', '-classpath', str(native), base + cls],
                                check=True, capture_output=True, text=True, encoding='utf8').stdout
        for method in methods:
            assert re.search(r'\b' + re.escape(method) + r'\(', output), (native.name, cls, method)

    with zipfile.ZipFile(args.artifact) as jar:
        assert jar.testzip() is None
        config = json.loads(jar.read('irons_nouveau.mixins.json'))
        for cls in config['mixins'] + config.get('client', []):
            assert (config['package'] + '.' + cls).replace('.', '/') + '.class' in jar.namelist(), cls
        if config.get('refmap'):
            assert config['refmap'] in jar.namelist()
        for cls, selector in [('WindBladeBehaviorMixin', tick), ('WindEntityTickMixin', tick),
                              ('WindBladeStateMixin', define), ('WindAeropicMixin', effect)]:
            assert selector.encode() in jar.read('dev/ironsnouveau/mixin/' + cls + '.class'), (cls, selector)
        assert 'pack.mcmeta' in jar.namelist()
        assert 'LICENSE' in jar.namelist() and 'THIRD_PARTY_NOTICES.md' in jar.namelist()
        assert sum(n.startswith('data/irons_nouveau/conflux/glyph_abilities/glyph_native/wind_spellbooks/') and n.endswith('.json') for n in jar.namelist()) == 7
        assert not any(n.startswith('dev/ironsnouveau/WindManualCases') for n in jar.namelist())
    for archive in [args.artifact] + ([args.sources] if args.sources else []):
        with zipfile.ZipFile(archive) as jar:
            assert not any(n.startswith(('net/raptorzizi/', 'assets/wind_spellbooks/', 'data/wind_spellbooks/', 'libs/')) for n in jar.namelist()), archive
    print(json.dumps({'platform': args.platform, 'wind': native.name, 'artifact': args.artifact.name,
                      'nativeMethodSelectors': 'PASS', 'mixinClassesAndRefmap': 'PASS',
                      'metadataAndAbilityHints': 'PASS', 'noUpstreamContent': 'PASS'}))


if __name__ == '__main__':
    main()
